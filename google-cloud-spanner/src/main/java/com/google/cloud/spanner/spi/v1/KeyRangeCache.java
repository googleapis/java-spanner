package com.google.cloud.spanner.spi.v1;

import com.google.protobuf.ByteString;
import com.google.spanner.v1.CacheUpdate;
import com.google.spanner.v1.Group;
import com.google.spanner.v1.Range;
import com.google.spanner.v1.RoutingHint;
import com.google.spanner.v1.Tablet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Cache for routing information. - Tablets are stored directly within Groups - Groups are updated
 * atomically with their tablets - Ranges reference groups
 */
public final class KeyRangeCache {

  private final ChannelFinderServerFactory serverFactory;

  // Map keyed by limit_key, value contains start_key and group reference
  private final NavigableMap<ByteString, CachedRange> ranges =
      new TreeMap<>(ByteString.unsignedLexicographicalComparator());

  // Groups indexed by group_uid
  private final Map<Long, CachedGroup> groups = new HashMap<>();

  // Servers indexed by address - shared across all tablets
  private final Map<String, ServerEntry> servers = new HashMap<>();

  public KeyRangeCache(ChannelFinderServerFactory serverFactory) {
    this.serverFactory = Objects.requireNonNull(serverFactory);
  }

  private static class ServerEntry {
    final ChannelFinderServer server;
    int refs = 1;

    ServerEntry(ChannelFinderServer server) {
      this.server = server;
    }

    String debugString() {
      return server.getAddress() + "#" + refs;
    }
  }

  /**
   * Represents a single tablet within a Group. Tablets are stored directly in the Group, not in a
   * separate cache.
   */
  private class CachedTablet {
    long tabletUid = 0;
    ByteString incarnation = ByteString.EMPTY;
    String serverAddress = "";
    int distance = 0;
    boolean skip = false;
    Tablet.Role role = Tablet.Role.ROLE_UNSPECIFIED;
    String location = "";

    // Lazily initialized server connection
    ChannelFinderServer server = null;

    CachedTablet() {}

    /** Updates tablet from proto, ignoring updates that are too old. */
    void update(Tablet tabletIn) {
      // Check incarnation - only update if newer
      if (tabletUid > 0
          && ByteString.unsignedLexicographicalComparator()
                  .compare(incarnation, tabletIn.getIncarnation())
              > 0) {
        return;
      }

      tabletUid = tabletIn.getTabletUid();
      incarnation = tabletIn.getIncarnation();
      distance = tabletIn.getDistance();
      skip = tabletIn.getSkip();
      role = tabletIn.getRole();
      location = tabletIn.getLocation();

      // Only reset server if address changed
      if (!serverAddress.equals(tabletIn.getServerAddress())) {
        serverAddress = tabletIn.getServerAddress();
        server = null; // Will be lazily initialized
      }
    }

    /** Returns true if tablet should be skipped (unhealthy, marked skip, or no address). */
    boolean shouldSkip(RoutingHint.Builder hintBuilder) {
      if (skip || serverAddress.isEmpty()) {
        addSkippedTablet(hintBuilder);
        return true;
      }
      // Check server health
      if (server != null && !server.isHealthy()) {
        addSkippedTablet(hintBuilder);
        return true;
      }
      return false;
    }

    private void addSkippedTablet(RoutingHint.Builder hintBuilder) {
      RoutingHint.SkippedTablet.Builder skipped = hintBuilder.addSkippedTabletUidBuilder();
      skipped.setTabletUid(tabletUid);
      skipped.setIncarnation(incarnation);
    }

    /** Picks this tablet for the request and returns the server. */
    ChannelFinderServer pick(RoutingHint.Builder hintBuilder) {
      hintBuilder.setTabletUid(tabletUid);
      if (server == null && !serverAddress.isEmpty()) {
        // Lazy server initialization - matches C++ behavior
        ServerEntry entry = findOrInsertServer(serverAddress);
        server = entry.server;
      }
      return server;
    }

    String debugString() {
      return tabletUid
          + ":"
          + serverAddress
          + "@"
          + incarnation
          + "(location="
          + location
          + ",role="
          + role
          + ",distance="
          + distance
          + (skip ? ",skip" : "")
          + ")";
    }
  }

  /** Represents a paxos group with its tablets. Tablets are stored directly in the group. */
  private class CachedGroup {
    final long groupUid;
    ByteString generation = ByteString.EMPTY;
    List<CachedTablet> tablets = new ArrayList<>();
    int leaderIndex = -1;
    int refs = 1;

    CachedGroup(long groupUid) {
      this.groupUid = groupUid;
    }

    /** Updates group from proto, including its tablets. */
    void update(Group groupIn) {
      System.out.println("DEBUG [BYPASS]: Group.update for group " + groupUid 
          + ", incoming tablets: " + groupIn.getTabletsCount() 
          + ", leader_index: " + groupIn.getLeaderIndex());

      // Only update leader if generation is newer
      if (ByteString.unsignedLexicographicalComparator()
              .compare(groupIn.getGeneration(), generation)
          > 0) {
        generation = groupIn.getGeneration();

        // Update leader index
        if (groupIn.getLeaderIndex() >= 0 && groupIn.getLeaderIndex() < groupIn.getTabletsCount()) {
          leaderIndex = groupIn.getLeaderIndex();
          System.out.println("DEBUG [BYPASS]: Set leader_index to " + leaderIndex);
        } else {
          leaderIndex = -1;
          System.out.println("DEBUG [BYPASS]: No valid leader, set to -1");
        }
      }

      // Update tablet locations. Optimize for typical case where tablets haven't changed.
      if (tablets.size() == groupIn.getTabletsCount()) {
        boolean mismatch = false;
        for (int t = 0; t < groupIn.getTabletsCount(); t++) {
          if (tablets.get(t).tabletUid != groupIn.getTablets(t).getTabletUid()) {
            mismatch = true;
            break;
          }
        }
        if (!mismatch) {
          // Same tablets, just update them in place
          System.out.println("DEBUG [BYPASS]: Tablets unchanged, updating in place");
          for (int t = 0; t < groupIn.getTabletsCount(); t++) {
            tablets.get(t).update(groupIn.getTablets(t));
          }
          return;
        }
      }

      // Tablets changed - rebuild the list, reusing existing tablets where possible
      System.out.println("DEBUG [BYPASS]: Rebuilding tablet list");
      Map<Long, CachedTablet> tabletsByUid = new HashMap<>();
      for (CachedTablet tablet : tablets) {
        tabletsByUid.put(tablet.tabletUid, tablet);
      }

      List<CachedTablet> newTablets = new ArrayList<>(groupIn.getTabletsCount());
      for (int t = 0; t < groupIn.getTabletsCount(); t++) {
        Tablet tabletIn = groupIn.getTablets(t);
        CachedTablet tablet = tabletsByUid.get(tabletIn.getTabletUid());
        if (tablet == null) {
          tablet = new CachedTablet();
          System.out.println("DEBUG [BYPASS]: Created new tablet for uid " + tabletIn.getTabletUid());
        }
        tablet.update(tabletIn);
        System.out.println("DEBUG [BYPASS]: Tablet[" + t + "]: uid=" + tablet.tabletUid 
            + ", server=" + tablet.serverAddress 
            + ", distance=" + tablet.distance);
        newTablets.add(tablet);
      }
      tablets = newTablets;
      System.out.println("DEBUG [BYPASS]: Group " + groupUid + " now has " + tablets.size() + " tablets");
    }

    /** Fills routing hint with tablet information and returns the server. */
    ChannelFinderServer fillRoutingHint(boolean preferLeader, RoutingHint.Builder hintBuilder) {
      System.out.println("DEBUG [BYPASS]: Group.fillRoutingHint - preferLeader: " + preferLeader 
          + ", tablets count: " + tablets.size());

      // Try leader first if preferred
      if (preferLeader && hasLeader()) {
        CachedTablet leaderTablet = leader();
        System.out.println("DEBUG [BYPASS]: Trying leader tablet: uid=" + leaderTablet.tabletUid 
            + ", address=" + leaderTablet.serverAddress 
            + ", skip=" + leaderTablet.skip);
        if (!leaderTablet.shouldSkip(hintBuilder)) {
          ChannelFinderServer server = leaderTablet.pick(hintBuilder);
          System.out.println("DEBUG [BYPASS]: Leader tablet picked, server: " 
              + (server != null ? server.getAddress() : "null"));
          return server;
        }
      }

      // Try other tablets in order (they're ordered by distance)
      for (int i = 0; i < tablets.size(); i++) {
        CachedTablet tablet = tablets.get(i);
        System.out.println("DEBUG [BYPASS]: Trying tablet[" + i + "]: uid=" + tablet.tabletUid 
            + ", address=" + tablet.serverAddress 
            + ", distance=" + tablet.distance 
            + ", skip=" + tablet.skip);
        if (!tablet.shouldSkip(hintBuilder)) {
          ChannelFinderServer server = tablet.pick(hintBuilder);
          System.out.println("DEBUG [BYPASS]: Tablet[" + i + "] picked, server: " 
              + (server != null ? server.getAddress() : "null"));
          return server;
        }
      }

      System.out.println("DEBUG [BYPASS]: No suitable tablet found in group");
      return null;
    }

    boolean hasLeader() {
      return leaderIndex >= 0 && leaderIndex < tablets.size();
    }

    CachedTablet leader() {
      return tablets.get(leaderIndex);
    }

    String debugString() {
      StringBuilder sb = new StringBuilder();
      sb.append(groupUid).append(":[");
      for (int i = 0; i < tablets.size(); i++) {
        sb.append(tablets.get(i).debugString());
        if (hasLeader() && i == leaderIndex) {
          sb.append(" (leader)");
        }
        if (i < tablets.size() - 1) {
          sb.append(", ");
        }
      }
      sb.append("]@").append(generation.toStringUtf8());
      sb.append("#").append(refs);
      return sb.toString();
    }
  }

  /** Represents a cached range with its group and split information. */
  private static class CachedRange {
    final ByteString startKey;
    CachedGroup group = null;
    long splitId = 0;
    ByteString generation;

    CachedRange(ByteString startKey, CachedGroup group, long splitId, ByteString generation) {
      this.startKey = startKey;
      this.group = group;
      this.splitId = splitId;
      this.generation = generation;
    }

    String debugString() {
      return (group != null ? group.groupUid : "null_group")
          + ","
          + splitId
          + "@"
          + (generation.isEmpty() ? "" : generation.toStringUtf8());
    }
  }

  private ServerEntry findOrInsertServer(String address) {
    ServerEntry entry = servers.get(address);
    if (entry == null) {
      entry = new ServerEntry(serverFactory.create(address));
      servers.put(address, entry);
    } else {
      entry.refs++;
    }
    return entry;
  }

  private void unref(ServerEntry serverEntry) {
    if (serverEntry == null) {
      return;
    }
    if (--serverEntry.refs == 0) {
      servers.remove(serverEntry.server.getAddress());
    }
  }

  private CachedGroup findGroup(long groupUid) {
    CachedGroup group = groups.get(groupUid);
    if (group != null) {
      group.refs++;
    }
    return group;
  }

  /** Finds or inserts a group and updates it with proto data. */
  private CachedGroup findOrInsertGroup(Group groupIn) {
    CachedGroup group = groups.get(groupIn.getGroupUid());
    if (group == null) {
      group = new CachedGroup(groupIn.getGroupUid());
      groups.put(groupIn.getGroupUid(), group);
    } else {
      group.refs++;
    }
    group.update(groupIn);
    return group;
  }

  private void unref(CachedGroup group) {
    if (group == null) {
      return;
    }
    if (--group.refs == 0) {
      groups.remove(group.groupUid);
    }
  }

  private void replaceRangeIfNewer(Range rangeIn) {
    ByteString startKey = rangeIn.getStartKey();
    ByteString limitKey = rangeIn.getLimitKey();

    List<ByteString> affectedLimitKeys = new ArrayList<>();
    boolean newerBlockingRangeExists = false;

    // Find overlapping ranges
    for (Map.Entry<ByteString, CachedRange> entry : ranges.tailMap(startKey, false).entrySet()) {
      ByteString existingLimit = entry.getKey();
      CachedRange existingRange = entry.getValue();
      ByteString existingStart = existingRange.startKey;

      if (ByteString.unsignedLexicographicalComparator().compare(existingStart, limitKey) >= 0) {
        break;
      }

      if (isNewerOrSame(rangeIn, existingRange, existingLimit)) {
        affectedLimitKeys.add(existingLimit);
      } else {
        newerBlockingRangeExists = true;
        break;
      }
    }

    if (newerBlockingRangeExists) {
      return;
    }

    for (ByteString keyToRemove : affectedLimitKeys) {
      CachedRange removed = ranges.remove(keyToRemove);
      if (removed == null) {
        continue;
      }

      if (ByteString.unsignedLexicographicalComparator().compare(limitKey, keyToRemove) < 0) {
        CachedRange tailPart =
            new CachedRange(limitKey, removed.group, removed.splitId, removed.generation);
        if (tailPart.group != null) {
          tailPart.group.refs++;
        }
        ranges.put(keyToRemove, tailPart);
      }

      if (ByteString.unsignedLexicographicalComparator().compare(removed.startKey, startKey) < 0) {
        ranges.put(startKey, removed);
      } else {
        if (removed.group != null) {
          unref(removed.group);
        }
      }
    }

    CachedRange newCachedRange =
        new CachedRange(
            startKey,
            findGroup(rangeIn.getGroupUid()),
            rangeIn.getSplitId(),
            rangeIn.getGeneration());
    ranges.put(limitKey, newCachedRange);
  }

  private boolean isNewerOrSame(
      Range rangeIn, CachedRange existingCachedRange, ByteString existingMapKeyLimit) {
    int genCompare =
        ByteString.unsignedLexicographicalComparator()
            .compare(rangeIn.getGeneration(), existingCachedRange.generation);
    if (genCompare > 0) {
      return true;
    }
    if (genCompare == 0) {
      return rangeIn.getStartKey().equals(existingCachedRange.startKey)
          && rangeIn.getLimitKey().equals(existingMapKeyLimit);
    }
    return false;
  }

  /** Applies cache updates. Tablets are processed inside group updates. */
  public void addRanges(CacheUpdate cacheUpdate) {
    System.out.println("DEBUG [BYPASS]: addRanges called with " 
        + cacheUpdate.getGroupCount() + " groups, " 
        + cacheUpdate.getRangeCount() + " ranges");

    // Insert all groups. Tablets are processed inside findOrInsertGroup -> Group.update()
    List<CachedGroup> newGroups = new ArrayList<>();
    for (Group groupIn : cacheUpdate.getGroupList()) {
      System.out.println("DEBUG [BYPASS]: Processing group " + groupIn.getGroupUid() 
          + " with " + groupIn.getTabletsCount() + " tablets");
      newGroups.add(findOrInsertGroup(groupIn));
    }

    // Process ranges
    for (Range rangeIn : cacheUpdate.getRangeList()) {
      System.out.println("DEBUG [BYPASS]: Processing range for group " + rangeIn.getGroupUid() 
          + ", split_id=" + rangeIn.getSplitId());
      replaceRangeIfNewer(rangeIn);
    }

    // Unref the groups we acquired (ranges hold their own refs)
    for (CachedGroup g : newGroups) {
      unref(g);
    }

    System.out.println("DEBUG [BYPASS]: After addRanges - ranges: " + ranges.size() 
        + ", groups: " + groups.size() + ", servers: " + servers.size());
  }

  /** Fills routing hint and returns the server to use. */
  public ChannelFinderServer fillRoutingInfo(
      String sessionUri, boolean preferLeader, RoutingHint.Builder hintBuilder) {
    System.out.println("DEBUG [BYPASS]: fillRoutingInfo called, ranges in cache: " + ranges.size() 
        + ", groups in cache: " + groups.size());
    
    if (hintBuilder.getKey().isEmpty()) {
      System.out.println("DEBUG [BYPASS]: No key in hint, using default server");
      return serverFactory.defaultServer();
    }

    ByteString requestKey = hintBuilder.getKey();
    ByteString requestLimitKey = hintBuilder.getLimitKey();

    // Find range containing the key
    Map.Entry<ByteString, CachedRange> entry = ranges.higherEntry(requestKey);

    CachedRange targetRange = null;
    ByteString targetRangeLimitKey = null;

    if (entry != null) {
      ByteString rangeLimit = entry.getKey();
      CachedRange range = entry.getValue();

      // Check if key is within this range
      if (ByteString.unsignedLexicographicalComparator().compare(requestKey, range.startKey) >= 0) {
        targetRange = range;
        targetRangeLimitKey = rangeLimit;
        System.out.println("DEBUG [BYPASS]: Found range for key, group_uid: " 
            + (range.group != null ? range.group.groupUid : "null"));
      }
    }

    if (targetRange == null) {
      System.out.println("DEBUG [BYPASS]: No range found for key, using default server");
      return serverFactory.defaultServer();
    }

    // For point reads (empty limit_key), check if key is in the split
    // For range reads, check if the whole range is covered
    if (!requestLimitKey.isEmpty()) {
      // Range read - check if limit is within the split
      if (ByteString.unsignedLexicographicalComparator()
              .compare(requestLimitKey, targetRangeLimitKey)
          > 0) {
        // Range extends beyond this split
        System.out.println("DEBUG [BYPASS]: Range extends beyond split, using default server");
        return serverFactory.defaultServer();
      }
    }

    if (targetRange.group == null) {
      System.out.println("DEBUG [BYPASS]: Range has no group, using default server");
      return serverFactory.defaultServer();
    }

    // Fill in routing hint with range/group/split info
    hintBuilder.setGroupUid(targetRange.group.groupUid);
    hintBuilder.setSplitId(targetRange.splitId);
    hintBuilder.setKey(targetRange.startKey);
    hintBuilder.setLimitKey(targetRangeLimitKey);

    System.out.println("DEBUG [BYPASS]: Group " + targetRange.group.groupUid 
        + " has " + targetRange.group.tablets.size() + " tablets"
        + ", hasLeader: " + targetRange.group.hasLeader()
        + ", leaderIndex: " + targetRange.group.leaderIndex);

    // Let the group pick the tablet
    ChannelFinderServer server = targetRange.group.fillRoutingHint(preferLeader, hintBuilder);
    if (server != null) {
      System.out.println("DEBUG [BYPASS]: Group returned server: " + server.getAddress());
      return server;
    }

    System.out.println("DEBUG [BYPASS]: Group returned no server, using default");
    return serverFactory.defaultServer();
  }

  public void clear() {
    for (CachedRange range : ranges.values()) {
      if (range.group != null) {
        unref(range.group);
      }
    }
    ranges.clear();
    groups.clear();
    servers.clear();
  }

  public String debugString() {
    StringBuilder sb = new StringBuilder();
    for (Map.Entry<ByteString, CachedRange> entry : ranges.entrySet()) {
      CachedRange cachedRange = entry.getValue();
      sb.append("Range[")
          .append(cachedRange.startKey.toStringUtf8())
          .append("-")
          .append(entry.getKey().toStringUtf8())
          .append("]: ");
      sb.append(cachedRange.debugString()).append("\n");
    }
    for (CachedGroup g : groups.values()) {
      sb.append(g.debugString()).append("\n");
    }
    for (ServerEntry s : servers.values()) {
      sb.append(s.debugString()).append("\n");
    }
    return sb.toString();
  }
}
