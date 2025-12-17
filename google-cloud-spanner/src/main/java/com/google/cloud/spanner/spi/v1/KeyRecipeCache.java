package com.google.cloud.spanner.spi.v1;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.ByteString;
import com.google.spanner.v1.ReadRequest;
import com.google.spanner.v1.RecipeList;
import com.google.spanner.v1.RoutingHint;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class KeyRecipeCache {

  // TODO: Implement robust fingerprinting
  // algorithm or a way to call the C++ Fingerprint2011.
  private static long fingerprint(ReadRequest req) {
    long result = Objects.hash(req.getTable());
    result = 31 * result + Objects.hash(PreparedRead.getKind(req));
    for (String column : req.getColumnsList()) {
      result = 31 * result + column.hashCode();
    }
    return result;
  }

  private final AtomicLong nextQueryUid = new AtomicLong(1);
  private ByteString schemaGeneration = ByteString.EMPTY;

  // query_recipes_ are not used for ReadRequest handling, so omitted for now.
  // private final Map<Long, KeyRecipe> queryRecipes = new ConcurrentHashMap<>();
  private final Map<String, KeyRecipe> schemaRecipes = new ConcurrentHashMap<>();
  private final Map<Long, PreparedRead> preparedReads = new ConcurrentHashMap<>();

  // For simplicity, miss reasons are not explicitly tracked with status in this version.
  // enum MissReason { FINGERPRINT_COLLISION, SCHEMA_RECIPE_NOT_FOUND, FAILED_KEY_ENCODING,
  // INELIGIBLE_READ }

  public KeyRecipeCache() {}

  public synchronized void addRecipes(RecipeList recipeList) {
    int cmp =
        ByteString.unsignedLexicographicalComparator()
            .compare(recipeList.getSchemaGeneration(), schemaGeneration);
    if (cmp < 0) {
      return;
    }
    if (cmp > 0) {
      schemaGeneration = recipeList.getSchemaGeneration();
      // queryRecipes.clear(); // Not used for ReadRequest
      schemaRecipes.clear();
    }

    for (com.google.spanner.v1.KeyRecipe recipeProto : recipeList.getRecipeList()) {
      try {
        KeyRecipe recipe = KeyRecipe.create(recipeProto);
        if (recipeProto.hasTableName()) {
          schemaRecipes.put(recipeProto.getTableName(), recipe);
        } else if (recipeProto.hasIndexName()) {
          schemaRecipes.put(recipeProto.getIndexName(), recipe);
        } else if (recipeProto.hasOperationUid()) {
          // Not handling query_uid recipes for ReadRequest
        }
      } catch (IllegalArgumentException e) {
        // Log or handle failed recipe creation
        System.err.println("Failed to add recipe: " + recipeProto + ", error: " + e.getMessage());
      }
    }
  }

  public void computeKeys(ReadRequest.Builder reqBuilder) {
    long reqFp = fingerprint(reqBuilder.buildPartial()); // Partial build OK for fingerprinting

    RoutingHint.Builder hintBuilder = reqBuilder.getRoutingHintBuilder();
    if (!schemaGeneration.isEmpty()) {
      hintBuilder.setSchemaGeneration(schemaGeneration);
    }

    PreparedRead preparedRead = preparedReads.get(reqFp);
    if (preparedRead == null) {
      preparedRead = PreparedRead.fromRequest(reqBuilder.buildPartial());
      preparedRead.queryUid = nextQueryUid.getAndIncrement();
      preparedReads.put(reqFp, preparedRead);
    } else if (!preparedRead.matches(reqBuilder.buildPartial())) {
      // recordMiss(MissReason.FINGERPRINT_COLLISION);
      System.err.println("Fingerprint collision for ReadRequest: " + reqFp);
      return;
    }

    hintBuilder.setOperationUid(preparedRead.queryUid);
    String recipeKey = reqBuilder.getTable();
    if (!reqBuilder.getIndex().isEmpty()) {
      recipeKey = reqBuilder.getIndex();
    }

    KeyRecipe recipe = schemaRecipes.get(recipeKey);
    if (recipe == null) {
      // recordMiss(MissReason.SCHEMA_RECIPE_NOT_FOUND);
      System.err.println("Schema recipe not found for: " + recipeKey);
      return;
    }

    try {
      switch (preparedRead.kind) {
        case POINT:
          if (reqBuilder.getKeySet().getKeysCount() == 0) {
            System.err.println("POINT read has no keys in KeySet.");
            return;
          }
          hintBuilder.setKey(recipe.keyToSS(reqBuilder.getKeySet().getKeys(0)));
          break;
        case RANGE:
        case RANGE_WITH_LIMIT:
          if (reqBuilder.getKeySet().getRangesCount() == 0) {
            System.err.println("RANGE read has no ranges in KeySet.");
            return;
          }
          hintBuilder.setKey(recipe.keyRangeToSSRangeStart(reqBuilder.getKeySet().getRanges(0)));
          hintBuilder.setLimitKey(
              recipe.keyRangeToSSRangeLimit(reqBuilder.getKeySet().getRanges(0)));
          break;
        case INELIGIBLE:
          // recordMiss(MissReason.INELIGIBLE_READ);
          System.err.println("Ineligible read request for key computation.");
          return;
      }
    } catch (IllegalArgumentException e) {
      // recordMiss(MissReason.FAILED_KEY_ENCODING, e.getMessage());
      System.err.println("Failed key encoding: " + e.getMessage());
    }
  }

  public synchronized void clear() {
    schemaGeneration = ByteString.EMPTY;
    preparedReads.clear();
    // queryRecipes.clear(); // Not used for ReadRequest
    schemaRecipes.clear();
  }

  private static class PreparedRead {
    final String table;
    final ImmutableList<String> columns;
    final Kind kind;
    long queryUid; // Not final, assigned after construction

    enum Kind {
      POINT,
      RANGE,
      RANGE_WITH_LIMIT,
      INELIGIBLE
    }

    private PreparedRead(String table, List<String> columns, Kind kind) {
      this.table = table;
      this.columns = ImmutableList.copyOf(columns);
      this.kind = kind;
    }

    static Kind getKind(ReadRequest req) {
      if (req.getKeySet().getAll()) {
        return Kind.INELIGIBLE;
      }
      if (req.getKeySet().getKeysCount() == 1 && req.getKeySet().getRangesCount() == 0) {
        return Kind.POINT;
      }
      if (req.getKeySet().getKeysCount() == 0 && req.getKeySet().getRangesCount() == 1) {
        return req.getLimit() > 0 ? Kind.RANGE_WITH_LIMIT : Kind.RANGE;
      }
      return Kind.INELIGIBLE;
    }

    static PreparedRead fromRequest(ReadRequest req) {
      return new PreparedRead(req.getTable(), req.getColumnsList(), getKind(req));
    }

    boolean matches(ReadRequest req) {
      if (!Objects.equals(table, req.getTable())) {
        return false;
      }
      if (!columns.equals(req.getColumnsList())) {
        return false;
      }
      return kind == getKind(req);
    }
  }
}
