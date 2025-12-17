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

public final class KeyRangeCache {

  private final ChannelFinderServerFactory serverFactory;

  private final NavigableMap<ByteString, CachedRange> ranges =
      new TreeMap<>(ByteString.unsignedLexicographicalComparator());
  private final Map<Long, CachedGroup> groups = new HashMap<>();
  private final Map<Long, CachedTablet> tablets = new HashMap<>();
  private final Map<String, ServerEntry> servers = new HashMap<>();

  public KeyRangeCache(ChannelFinderServerFactory serverFactory) {
    this.serverFactory = Objects.requireNonNull(serverFactory);
  }

  private static class ServerEntry {
    final ChannelFinderServer server;
    int refs = 1; // Start with 1 as it's added to the map

    ServerEntry(ChannelFinderServer server) {
      this.server = server;
    }

    String debugString() {
      return server.getAddress() + "#" + refs;
    }
  }

  private static class CachedTablet {
    final long tabletUid;
    ByteString incarnation = ByteString.EMPTY;
    ServerEntry server = null; // Can be null initially or if server is removed
    int refs = 1;

    CachedTablet(long tabletUid) {
      this.tabletUid = tabletUid;
    }

    String debugString() {
      return tabletUid
          + ":"
          + (server != null ? server.server.getAddress() : "null_server")
          + "@"
          + incarnation
          + "#"
          + refs;
    }
  }

  private static class CachedGroup {
    final long groupUid;
    ByteString generation = ByteString.EMPTY; // Initialize to empty
    List<CachedTablet> localTablets = new ArrayList<>();
    CachedTablet leader = null;
    int refs = 1;

    CachedGroup(long groupUid) {
      this.groupUid = groupUid;
    }

    String debugString() {
      StringBuilder sb = new StringBuilder();
      sb.append(groupUid).append(":[");
      for (int i = 0; i < localTablets.size(); i++) {
        sb.append(localTablets.get(i).tabletUid);
        if (i < localTablets.size() - 1) {
          sb.append(",");
        }
      }
      sb.append("],");
      sb.append(leader == null ? "null" : leader.tabletUid);
      sb.append("@")
          .append(
              generation.isEmpty()
                  ? ""
                  : generation.toStringUtf8()); // TODO: Escape generation if needed
      sb.append("#").append(refs);
      return sb.toString();
    }
  }

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

    CachedRange() {
      this.startKey = ByteString.EMPTY;
    }

    String debugString() {
      return (group != null ? group.groupUid : "null_group")
          + ","
          + splitId
          + "@"
          + (generation.isEmpty()
              ? ""
              : generation.toStringUtf8()); // TODO: Escape generation if needed
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

  private CachedTablet findTablet(long tabletUid) {
    CachedTablet tablet = tablets.get(tabletUid);
    if (tablet != null) {
      tablet.refs++;
    }
    return tablet;
  }

  private void unref(CachedTablet tablet) {
    if (tablet == null) {
      return;
    }
    if (--tablet.refs == 0) {
      if (tablet.server != null) {
        unref(tablet.server);
      }
      tablets.remove(tablet.tabletUid);
    }
  }

  private void unref(ServerEntry serverEntry) {
    if (serverEntry == null) {
      return;
    }
    if (--serverEntry.refs == 0) {
      servers.remove(serverEntry.server.getAddress());
    }
  }

  private void unref(CachedGroup group) {
    if (group == null) {
      return;
    }
    if (--group.refs == 0) {
      for (CachedTablet t : group.localTablets) {
        unref(t);
      }
      if (group.leader != null) {
        unref(group.leader);
      }
      groups.remove(group.groupUid);
    }
  }

  private CachedTablet findOrInsertTablet(Tablet tabletIn) {
    CachedTablet tablet = tablets.get(tabletIn.getTabletUid());
    if (tablet == null) {
      tablet = new CachedTablet(tabletIn.getTabletUid());
      tablets.put(tabletIn.getTabletUid(), tablet);
    } else {
      tablet.refs++;
    }

    if (ByteString.unsignedLexicographicalComparator()
            .compare(tabletIn.getIncarnation(), tablet.incarnation)
        <= 0) {
      return tablet;
    }
    tablet.incarnation = tabletIn.getIncarnation();
    ServerEntry newServer = findOrInsertServer(tabletIn.getServerAddress());
    if (tablet.server != null) {
      unref(tablet.server); // Unref old server before assigning new one
    }
    tablet.server = newServer;
    return tablet;
  }

  private CachedGroup findGroup(long groupUid) {
    CachedGroup group = groups.get(groupUid);
    if (group != null) {
      group.refs++;
    }
    return group;
  }

  private CachedGroup findOrInsertGroup(Group groupIn) {
    CachedGroup group = groups.get(groupIn.getGroupUid());
    if (group == null) {
      group = new CachedGroup(groupIn.getGroupUid());
      groups.put(groupIn.getGroupUid(), group);
    } else {
      group.refs++;
    }

    if (ByteString.unsignedLexicographicalComparator()
                .compare(groupIn.getGeneration(), group.generation)
            <= 0
        && !group.generation.isEmpty()) {
      return group;
    }
    group.generation = groupIn.getGeneration();

    // Clear old tablets and leader refs
    for (CachedTablet t : group.localTablets) {
      unref(t);
    }
    group.localTablets.clear();
    if (group.leader != null) {
      unref(group.leader);
      group.leader = null;
    }

    for (Tablet t : groupIn.getTabletsList()) {
      CachedTablet tablet = findTablet(t.getTabletUid());
      if (tablet == null) {
        System.err.println(
            "Tablet not found during group update: " + t + " in group " + group.groupUid);
        continue;
      }
      group.localTablets.add(tablet); // findTablet already incremented ref
    }

    if (groupIn.getLeaderIndex() != 0) {
      group.leader = findTablet(groupIn.getLeaderIndex());
      if (group.leader == null) {
        System.err.println(
            "Leader tablet not found during group update: "
                + groupIn.getLeaderIndex()
                + " in group "
                + group.groupUid);
      }
    }
    return group;
  }

  private void replaceRangeIfNewer(Range rangeIn) {
    // IntervalMap logic is complex. This is a simplified interpretation.
    // For a production system, a robust IntervalMap implementation is needed.
    // This simplified logic handles basic non-overlapping and replacement scenarios.

    ByteString startKey = rangeIn.getStartKey();
    ByteString limitKey = rangeIn.getLimitKey();

    // With limitKey as the map key, the logic for finding overlaps changes.
    // We are looking for ranges (existingStart, existingLimit] where
    // existingLimit > newStartKey AND existingStart < newLimitKey.

    List<ByteString> affectedLimitKeys = new ArrayList<>();
    boolean newerOrIdenticalBlockingRangeExists = false;

    // Iterate through ranges that *might* overlap or be affected.
    // A range [s, l] is affected if l > startKey AND s < limitKey.
    // Since map is keyed by limit, we look for entries with limitKey > startKey.
    for (Map.Entry<ByteString, CachedRange> entry : ranges.tailMap(startKey, false).entrySet()) {
      ByteString existingLimit = entry.getKey(); // This is the key of the map
      CachedRange existingRange = entry.getValue();
      ByteString existingStart = existingRange.startKey;

      if (ByteString.unsignedLexicographicalComparator().compare(existingStart, limitKey) >= 0) {
        // This existing range starts at or after the new range's limit, so no more overlaps
        // possible.
        break;
      }

      // Now we have an overlapping or adjacent candidate:
      // existing: [existingStart, existingLimit]
      // new:      [startKey, limitKey]

      if (isNewerOrSame(rangeIn, existingRange, existingLimit)) {
        // The new range is newer or same. The existing range might be removed or split.
        // For simplification, if there's any overlap, we'll mark the existing for removal.
        // A true interval map would handle splits.
        affectedLimitKeys.add(existingLimit);
      } else {
        // An existing range is newer and overlaps/is identical. Block the new range.
        newerOrIdenticalBlockingRangeExists = true;
        break;
      }
    }

    if (newerOrIdenticalBlockingRangeExists) {
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
            findGroup(rangeIn.getGroupUid()), // findGroup increments ref
            rangeIn.getSplitId(),
            rangeIn.getGeneration());
    ranges.put(limitKey, newCachedRange); // Key by limitKey now
  }

  private boolean isOverlapping(
      ByteString newStart,
      ByteString newLimit,
      ByteString existingStart,
      ByteString existingLimit) {
    // Assuming empty limit means infinity for newLimit, but existingLimit (map key) should not be
    // empty unless it represents positive infinity.
    boolean newLimitInfinite = newLimit.isEmpty();
    boolean existingLimitInfinite = existingLimit.isEmpty();

    if (newLimitInfinite && existingLimitInfinite) {
      return true;
    }
    if (newLimitInfinite) {
      return ByteString.unsignedLexicographicalComparator().compare(newStart, existingLimit) < 0;
    }
    if (existingLimitInfinite) {
      return ByteString.unsignedLexicographicalComparator().compare(existingStart, newLimit) < 0;
    }

    // Standard overlap: newStart < existingLimit AND existingStart < newLimit
    return ByteString.unsignedLexicographicalComparator().compare(newStart, existingLimit) < 0
        && ByteString.unsignedLexicographicalComparator().compare(existingStart, newLimit) < 0;
  }

  // Check if rangeIn is newer or same as existingCachedRange (identified by its limitKey)
  private boolean isNewerOrSame(
      Range rangeIn, CachedRange existingCachedRange, ByteString existingMapKeyLimit) {
    int genCompare =
        ByteString.unsignedLexicographicalComparator()
            .compare(rangeIn.getGeneration(), existingCachedRange.generation);
    if (genCompare > 0) {
      return true; // new is strictly newer
    }
    if (genCompare == 0) { // Same generation, check if bounds are identical
      return rangeIn.getStartKey().equals(existingCachedRange.startKey)
          && rangeIn.getLimitKey().equals(existingMapKeyLimit);
    }
    return false; // new is older
  }

  public void addRanges(CacheUpdate cacheUpdate) {
    List<CachedTablet> newTablets = new ArrayList<>();
    for (Group groupIn : cacheUpdate.getGroupList()) {
      for (Tablet tabletIn : groupIn.getTabletsList()) {
        newTablets.add(findOrInsertTablet(tabletIn));
      }
    }
    List<CachedGroup> newGroups = new ArrayList<>();

    for (Group groupIn : cacheUpdate.getGroupList()) {
      newGroups.add(findOrInsertGroup(groupIn));
    }

    for (Range rangeIn : cacheUpdate.getRangeList()) {
      replaceRangeIfNewer(rangeIn);
    }

    // Unref newly acquired groups and tablets if they were only temporary for this update
    // The ones that are now part of the cache structure (ranges_, groups_, tablets_)
    // will have their refs maintained by those structures.
    for (CachedGroup g : newGroups) {
      unref(g);
    }
    for (CachedTablet t : newTablets) {
      unref(t);
    }
  }

  public ChannelFinderServer fillRoutingInfo(
      String sessionUri, boolean preferLeader, RoutingHint.Builder hintBuilder) {
    if (hintBuilder.getKey().isEmpty()) {
      return serverFactory.defaultServer();
    }

    ByteString requestKey = hintBuilder.getKey();
    // Find the first range whose limitKey is >= requestKey.
    // This means the requestKey *might* fall into this range or an earlier one.
    Map.Entry<ByteString, CachedRange> ceilingEntry = ranges.ceilingEntry(requestKey);

    CachedRange targetRange = null;
    ByteString targetRangeLimitKey = null;

    if (ceilingEntry != null) {
      // Check if the requestKey falls into the range ending at ceilingEntry.getKey()
      if (ByteString.unsignedLexicographicalComparator()
              .compare(requestKey, ceilingEntry.getValue().startKey)
          >= 0) {
        targetRange = ceilingEntry.getValue();
        targetRangeLimitKey = ceilingEntry.getKey();
      } else {
        // The requestKey is before the start of the ceilingEntry's range.
        // We need to check the range *before* the ceilingEntry.
        Map.Entry<ByteString, CachedRange> floorEntry = ranges.lowerEntry(ceilingEntry.getKey());
        if (floorEntry != null
            && ByteString.unsignedLexicographicalComparator()
                    .compare(requestKey, floorEntry.getValue().startKey)
                >= 0
            && ByteString.unsignedLexicographicalComparator()
                    .compare(requestKey, floorEntry.getKey())
                < 0) {
          targetRange = floorEntry.getValue();
          targetRangeLimitKey = floorEntry.getKey();
        }
      }
    } else {
      // No range limit is >= requestKey. This means the requestKey might be in the last range
      // if the map is not empty (i.e., a range with an "infinite" or very large limit key).
      // Or, the key is beyond all known ranges.
      if (!ranges.isEmpty()) {
        Map.Entry<ByteString, CachedRange> lastEntry = ranges.lastEntry();
        if (lastEntry != null
            && ByteString.unsignedLexicographicalComparator()
                    .compare(requestKey, lastEntry.getValue().startKey)
                >= 0) {
          // Assuming last range extends to infinity if its limit is the map's greatest key
          // and this key is effectively unbounded, or if its limit is an empty string (our
          // infinity).
          targetRange = lastEntry.getValue();
          targetRangeLimitKey = lastEntry.getKey();
        }
      }
    }

    if (targetRange == null) {
      return pickDefaultServer(sessionUri); // No suitable range found
    }

    // Check if the request's limitKey (if any) exceeds the found range's limitKey
    if (!hintBuilder.getLimitKey().isEmpty()
        && !targetRangeLimitKey.isEmpty()
        && ByteString.unsignedLexicographicalComparator()
                .compare(hintBuilder.getLimitKey(), targetRangeLimitKey)
            > 0) {
      return serverFactory.defaultServer();
    }

    if (targetRange.group == null) {
      return pickDefaultServer(sessionUri);
    }

    hintBuilder.setGroupUid(targetRange.group.groupUid);
    hintBuilder.setSplitId(targetRange.splitId);
    hintBuilder.setKey(targetRange.startKey);
    hintBuilder.setLimitKey(targetRangeLimitKey);

    // Try to pick a tablet
    if (preferLeader && targetRange.group.leader != null) {
      if (tryPickTablet(targetRange.group.leader, hintBuilder)) {
        return targetRange.group.leader.server.server;
      }
    }

    if (targetRange.group.localTablets.isEmpty()) {
      return pickDefaultServer(sessionUri);
    }

    long fp = sessionUri.hashCode();
    int offset =
        targetRange.group.localTablets.size() <= 1
            ? 0
            : (int) (fp % targetRange.group.localTablets.size());
    if (offset < 0) {
      offset += targetRange.group.localTablets.size(); // ensure positive for modulo
    }

    for (int i = 0; i < targetRange.group.localTablets.size(); ++i) {
      int idx = (offset + i) % targetRange.group.localTablets.size();
      CachedTablet tablet = targetRange.group.localTablets.get(idx);
      if (tryPickTablet(tablet, hintBuilder)) {
        return tablet.server.server;
      }
    }
    return pickDefaultServer(sessionUri);
  }

  private boolean tryPickTablet(CachedTablet tablet, RoutingHint.Builder hintBuilder) {
    if (tablet.server == null || !tablet.server.server.isHealthy()) {
      RoutingHint.SkippedTablet.Builder skipped = hintBuilder.addSkippedTabletUidBuilder();
      skipped.setTabletUid(tablet.tabletUid);
      skipped.setIncarnation(tablet.incarnation);
      return false;
    }
    hintBuilder.setTabletUid(tablet.tabletUid);
    return true;
  }

  private ChannelFinderServer pickDefaultServer(String key) {
    try {
      if (!servers.isEmpty()) {
        long fp = key.hashCode();
        int idx = (int) (fp % servers.size());
        if (idx < 0) {
          idx += servers.size(); // Ensure positive index
        }

        List<ServerEntry> serverList = new ArrayList<>(servers.values());

        // Try from idx to end
        for (int i = 0; i < serverList.size(); ++i) {
          int currentIdx = (idx + i) % serverList.size();
          ServerEntry entry = serverList.get(currentIdx);
          if (entry.server.isHealthy()) {
            return entry.server;
          }
        }
      }
      return serverFactory.defaultServer();
    } finally {
    }
  }

  public void clear() {
    try {
      for (CachedRange range : ranges.values()) {
        if (range.group != null) {
          unref(range.group);
        }
      }
      ranges.clear();
      // groups, tablets, servers should be empty if all unrefs were correct
      if (!groups.isEmpty() || !tablets.isEmpty() || !servers.isEmpty()) {
        // This indicates a potential ref counting issue
        System.err.println("Warning: Non-empty collections after clearing KeyRangeCache.");
        System.err.println(
            "Groups: "
                + groups.size()
                + ", Tablets: "
                + tablets.size()
                + ", Servers: "
                + servers.size());
        // Force clear them to ensure clean state for next use, though this hides the root cause.
        groups.clear();
        tablets.clear();
        servers.clear();
      }
    } finally {
    }
  }

  public String debugString() {
    try {
      StringBuilder sb = new StringBuilder();
      for (Map.Entry<ByteString, CachedRange> entry : ranges.entrySet()) {
        CachedRange cachedRange = entry.getValue();
        // Key of the map is the limitKey
        sb.append("Range[")
            .append(cachedRange.startKey.toStringUtf8())
            .append("-")
            .append(entry.getKey().toStringUtf8()) // entry.getKey() is the limitKey
            .append("]: ");
        sb.append(cachedRange.debugString()).append("\n");
      }
      for (CachedGroup g : groups.values()) {
        sb.append(g.debugString()).append("\n");
      }
      for (CachedTablet t : tablets.values()) {
        sb.append(t.debugString()).append("\n");
      }
      for (ServerEntry s : servers.values()) {
        sb.append(s.debugString()).append("\n");
      }
      return sb.toString();
    } finally {
    }
  }
}
