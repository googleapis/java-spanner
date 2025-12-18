package com.google.cloud.spanner.spi.v1;

import com.google.protobuf.ByteString;
import com.google.protobuf.ListValue;
import com.google.protobuf.Struct;
import com.google.spanner.v1.KeyRange;
import com.google.spanner.v1.KeySet;
import com.google.spanner.v1.Mutation;
import com.google.spanner.v1.RecipeList;
import java.util.ArrayList;
import java.util.List;

public final class RecipeTestCases {

  private final List<RecipeTestCase> testCases;

  private RecipeTestCases(Builder builder) {
    this.testCases = new ArrayList<>(builder.testCases);
  }

  public List<RecipeTestCase> getTestCaseList() {
    return testCases;
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public static final class Builder {
    private final List<RecipeTestCase> testCases = new ArrayList<>();

    public Builder addTestCase(RecipeTestCase testCase) {
      this.testCases.add(testCase);
      return this;
    }

    public RecipeTestCases build() {
      return new RecipeTestCases(this);
    }
  }

  public static final class RecipeTestCase {
    private final String name;
    private final RecipeList recipes;
    private final List<Test> tests;

    private RecipeTestCase(Builder builder) {
      this.name = builder.name;
      this.recipes = builder.recipes;
      this.tests = new ArrayList<>(builder.tests);
    }

    public String getName() {
      return name;
    }

    public RecipeList getRecipes() {
      return recipes;
    }

    public List<Test> getTestList() {
      return tests;
    }

    public static Builder newBuilder() {
      return new Builder();
    }

    public static final class Builder {
      private String name;
      private RecipeList recipes;
      private final List<Test> tests = new ArrayList<>();

      public Builder setName(String name) {
        this.name = name;
        return this;
      }

      public Builder setRecipes(RecipeList recipes) {
        this.recipes = recipes;
        return this;
      }

      public Builder addTest(Test test) {
        this.tests.add(test);
        return this;
      }

      public RecipeTestCase build() {
        return new RecipeTestCase(this);
      }
    }

    public static final class Test {
      private final OperationCase operationCase;
      private final Object operation;
      private final ByteString start;
      private final ByteString limit;
      private final boolean approximate;

      public enum OperationCase {
        KEY,
        KEY_RANGE,
        KEY_SET,
        MUTATION,
        QUERY_PARAMS,
        OPERATION_NOT_SET
      }

      private Test(Builder builder) {
        this.operationCase = builder.operationCase;
        this.operation = builder.operation;
        this.start = builder.start;
        this.limit = builder.limit;
        this.approximate = builder.approximate;
      }

      public OperationCase getOperationCase() {
        return operationCase;
      }

      public ListValue getKey() {
        if (operationCase == OperationCase.KEY) {
          return (ListValue) operation;
        }
        return ListValue.getDefaultInstance();
      }

      public KeyRange getKeyRange() {
        if (operationCase == OperationCase.KEY_RANGE) {
          return (KeyRange) operation;
        }
        return KeyRange.getDefaultInstance();
      }

      public KeySet getKeySet() {
        if (operationCase == OperationCase.KEY_SET) {
          return (KeySet) operation;
        }
        return KeySet.getDefaultInstance();
      }

      public Mutation getMutation() {
        if (operationCase == OperationCase.MUTATION) {
          return (Mutation) operation;
        }
        return Mutation.getDefaultInstance();
      }

      public Struct getQueryParams() {
        if (operationCase == OperationCase.QUERY_PARAMS) {
          return (Struct) operation;
        }
        return Struct.getDefaultInstance();
      }

      public ByteString getStart() {
        return start;
      }

      public ByteString getLimit() {
        return limit;
      }

      public boolean getApproximate() {
        return approximate;
      }

      public static Builder newBuilder() {
        return new Builder();
      }

      public static final class Builder {
        private OperationCase operationCase = OperationCase.OPERATION_NOT_SET;
        private Object operation;
        private ByteString start;
        private ByteString limit;
        private boolean approximate;

        public Builder setKey(ListValue key) {
          this.operationCase = OperationCase.KEY;
          this.operation = key;
          return this;
        }

        public Builder setKeyRange(KeyRange keyRange) {
          this.operationCase = OperationCase.KEY_RANGE;
          this.operation = keyRange;
          return this;
        }

        public Builder setKeySet(KeySet keySet) {
          this.operationCase = OperationCase.KEY_SET;
          this.operation = keySet;
          return this;
        }

        public Builder setMutation(Mutation mutation) {
          this.operationCase = OperationCase.MUTATION;
          this.operation = mutation;
          return this;
        }

        public Builder setQueryParams(Struct queryParams) {
          this.operationCase = OperationCase.QUERY_PARAMS;
          this.operation = queryParams;
          return this;
        }

        public Builder setStart(ByteString start) {
          this.start = start;
          return this;
        }

        public Builder setLimit(ByteString limit) {
          this.limit = limit;
          return this;
        }

        public Builder setApproximate(boolean approximate) {
          this.approximate = approximate;
          return this;
        }

        public Test build() {
          return new Test(this);
        }
      }
    }
  }
}
