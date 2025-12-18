/*
 * Copyright 2024 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.spanner.spi.v1;

import static org.junit.Assert.assertEquals;

import com.google.protobuf.ByteString;
import com.google.protobuf.ListValue;
import com.google.protobuf.Struct;
import com.google.protobuf.TextFormat;
import com.google.spanner.v1.KeyRange;
import com.google.spanner.v1.KeySet;
import com.google.spanner.v1.Mutation;
import com.google.spanner.v1.RecipeList;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class RecipeGoldenTest {

  @Test
  public void goldenTest() throws Exception {
    String content;
    try (InputStream inputStream =
        getClass().getClassLoader().getResourceAsStream("recipe_test.textproto")) {
      content =
          new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))
              .lines()
              .reduce("", (a, b) -> a + "\n" + b);
    }

    List<TestCase> testCases = parseTestCases(content);

    for (TestCase testCase : testCases) {
      System.out.println("Running test case: " + testCase.name);

      // Skip test cases with invalid recipes that couldn't be parsed
      if (testCase.invalidRecipe) {
        System.out.println("  Skipped (invalid recipe)");
        continue;
      }

      // Skip random tests since Java's Random doesn't match C++'s absl::BitGen
      if (testCase.name.contains("Random")) {
        System.out.println("  Skipped (random PRNG mismatch)");
        continue;
      }

      KeyRecipe recipe;
      try {
        recipe = KeyRecipe.create(testCase.recipes.getRecipe(0));
      } catch (IllegalArgumentException e) {
        // Invalid recipe - verify all tests expect approximate: true
        System.out.println("  Invalid recipe (caught in KeyRecipe.create): " + e.getMessage());
        for (TestInstance test : testCase.tests) {
          assertEquals(
              "Invalid recipe should result in approximate=true in test case: " + testCase.name,
              true,
              test.expectedApproximate);
        }
        continue;
      }

      int testNum = 0;
      for (TestInstance test : testCase.tests) {
        testNum++;
        System.out.println("  Test #" + testNum + ": type=" + test.operationType);
        System.out.println("    Expected start: " + bytesToHex(test.expectedStart));
        System.out.println("    Expected limit: " + bytesToHex(test.expectedLimit));
        System.out.println("    Expected approx: " + test.expectedApproximate);

        TargetRange target = null;
        switch (test.operationType) {
          case "key":
            System.out.println("    Key: " + test.key);
            target = recipe.keyToTargetRange(test.key);
            break;
          case "key_range":
            target = recipe.keyRangeToTargetRange(test.keyRange);
            break;
          case "key_set":
            target = recipe.keySetToTargetRange(test.keySet);
            break;
          case "mutation":
            target = recipe.mutationToTargetRange(test.mutation);
            break;
          case "query_params":
            target = recipe.queryParamsToTargetRange(test.queryParams);
            break;
          default:
            throw new UnsupportedOperationException("Unsupported operation: " + test.operationType);
        }

        System.out.println("    Actual start: " + bytesToHex(target.start));
        System.out.println("    Actual limit: " + bytesToHex(target.limit));
        System.out.println("    Actual approx: " + target.approximate);

        assertEquals(
            "Start mismatch in test case: " + testCase.name + " test #" + testNum,
            test.expectedStart,
            target.start);
        assertEquals(
            "Limit mismatch in test case: " + testCase.name + " test #" + testNum,
            test.expectedLimit,
            target.limit);
        assertEquals(
            "Approximate mismatch in test case: " + testCase.name + " test #" + testNum,
            test.expectedApproximate,
            target.approximate);
      }
    }
  }

  private static class TestCase {
    String name;
    RecipeList recipes;
    List<TestInstance> tests = new ArrayList<>();
    boolean invalidRecipe = false;
  }

  private static class TestInstance {
    String operationType;
    ListValue key;
    KeyRange keyRange;
    KeySet keySet;
    Mutation mutation;
    Struct queryParams;
    ByteString expectedStart = ByteString.EMPTY;
    ByteString expectedLimit = ByteString.EMPTY;
    boolean expectedApproximate = false;
  }

  private List<TestCase> parseTestCases(String content) throws Exception {
    List<TestCase> testCases = new ArrayList<>();
    int pos = 0;

    while (pos < content.length()) {
      int testCaseStart = content.indexOf("test_case {", pos);
      if (testCaseStart == -1) break;

      int testCaseEnd = findMatchingBrace(content, testCaseStart + 10);
      String testCaseContent = content.substring(testCaseStart + 11, testCaseEnd);

      TestCase tc = parseTestCase(testCaseContent);
      testCases.add(tc);

      pos = testCaseEnd + 1;
    }

    return testCases;
  }

  private TestCase parseTestCase(String content) throws Exception {
    TestCase tc = new TestCase();

    // Parse name
    Pattern namePattern = Pattern.compile("name:\\s*\"([^\"]+)\"");
    Matcher nameMatcher = namePattern.matcher(content);
    if (nameMatcher.find()) {
      tc.name = nameMatcher.group(1);
    }

    // Parse recipes
    int recipesStart = content.indexOf("recipes {");
    if (recipesStart != -1) {
      int recipesEnd = findMatchingBrace(content, recipesStart + 8);
      String recipesContent = content.substring(recipesStart + 9, recipesEnd);
      RecipeList.Builder recipesBuilder = RecipeList.newBuilder();
      try {
        TextFormat.merge(recipesContent, recipesBuilder);
        tc.recipes = recipesBuilder.build();
      } catch (TextFormat.ParseException e) {
        // Invalid recipe - skip this test case but mark it as having invalid recipes
        tc.invalidRecipe = true;
        System.out.println("Skipping test case with invalid recipe: " + tc.name);
      }
    }

    // Parse tests
    int pos = 0;
    while (pos < content.length()) {
      // Find "test {" that's not part of "test_case"
      int testStart = findNextTest(content, pos);
      if (testStart == -1) break;

      // "test {" is 6 chars, { is at position testStart + 5
      int bracePos = testStart + 5;
      int testEnd = findMatchingBrace(content, bracePos);
      String testContent = content.substring(bracePos + 1, testEnd);

      TestInstance test = parseTest(testContent);
      tc.tests.add(test);

      pos = testEnd + 1;
    }

    return tc;
  }

  private int findNextTest(String content, int start) {
    int pos = start;
    while (true) {
      int testPos = content.indexOf("test {", pos);
      if (testPos == -1) return -1;

      // Make sure this is not part of "test_case {"
      if (testPos >= 5) {
        String before = content.substring(testPos - 5, testPos);
        if (before.contains("_")) {
          pos = testPos + 1;
          continue;
        }
      }
      return testPos;
    }
  }

  private TestInstance parseTest(String content) throws Exception {
    TestInstance test = new TestInstance();

    // Determine operation type and parse operation
    // NOTE: Check mutation FIRST since it can contain nested key_set/key_range/key
    if (content.contains("mutation {")) {
      test.operationType = "mutation";
      int start = content.indexOf("mutation {");
      int end = findMatchingBrace(content, start + 9);
      String mutationContent = content.substring(start + 10, end);
      Mutation.Builder builder = Mutation.newBuilder();
      TextFormat.merge(mutationContent, builder);
      test.mutation = builder.build();
    } else if (content.contains("query_params {")) {
      test.operationType = "query_params";
      int start = content.indexOf("query_params {");
      int end = findMatchingBrace(content, start + 13);
      String queryParamsContent = content.substring(start + 14, end);
      Struct.Builder builder = Struct.newBuilder();
      TextFormat.merge(queryParamsContent, builder);
      test.queryParams = builder.build();
    } else if (content.contains("key_set {")) {
      test.operationType = "key_set";
      int start = content.indexOf("key_set {");
      int end = findMatchingBrace(content, start + 8);
      String keySetContent = content.substring(start + 9, end);
      KeySet.Builder builder = KeySet.newBuilder();
      TextFormat.merge(keySetContent, builder);
      test.keySet = builder.build();
    } else if (content.contains("key_range {")) {
      test.operationType = "key_range";
      int start = content.indexOf("key_range {");
      int end = findMatchingBrace(content, start + 10);
      String keyRangeContent = content.substring(start + 11, end);
      KeyRange.Builder builder = KeyRange.newBuilder();
      TextFormat.merge(keyRangeContent, builder);
      test.keyRange = builder.build();
    } else if (content.contains("key {")
        && !content.contains("key_range")
        && !content.contains("key_set")
        && !content.contains("limit_key")) {
      test.operationType = "key";
      int keyStart = content.indexOf("key {");
      int keyEnd = findMatchingBrace(content, keyStart + 4);
      String keyContent = content.substring(keyStart + 5, keyEnd);
      ListValue.Builder keyBuilder = ListValue.newBuilder();
      TextFormat.merge(keyContent, keyBuilder);
      test.key = keyBuilder.build();
    }

    // Parse expected start
    Pattern startPattern = Pattern.compile("start:\\s*\"([^\"]*)\"");
    Matcher startMatcher = startPattern.matcher(content);
    if (startMatcher.find()) {
      test.expectedStart = parseEscapedString(startMatcher.group(1));
    }

    // Parse expected limit
    Pattern limitPattern = Pattern.compile("(?<!_)limit:\\s*\"([^\"]*)\"");
    Matcher limitMatcher = limitPattern.matcher(content);
    if (limitMatcher.find()) {
      test.expectedLimit = parseEscapedString(limitMatcher.group(1));
    }

    // Parse approximate
    Pattern approxPattern = Pattern.compile("approximate:\\s*(true|false)");
    Matcher approxMatcher = approxPattern.matcher(content);
    if (approxMatcher.find()) {
      test.expectedApproximate = Boolean.parseBoolean(approxMatcher.group(1));
    }

    return test;
  }

  private int findMatchingBrace(String content, int openBracePos) {
    int depth = 1;
    int pos = openBracePos + 1;
    boolean inString = false;
    boolean escape = false;

    while (pos < content.length() && depth > 0) {
      char c = content.charAt(pos);

      if (escape) {
        escape = false;
        pos++;
        continue;
      }

      if (c == '\\') {
        escape = true;
        pos++;
        continue;
      }

      if (c == '"') {
        inString = !inString;
      } else if (!inString) {
        if (c == '{') {
          depth++;
        } else if (c == '}') {
          depth--;
        }
      }
      pos++;
    }
    return pos - 1;
  }

  private static String bytesToHex(ByteString bs) {
    StringBuilder sb = new StringBuilder();
    for (byte b : bs.toByteArray()) {
      sb.append(String.format("%02x ", b & 0xFF));
    }
    return sb.toString();
  }

  private ByteString parseEscapedString(String escaped) {
    byte[] bytes = new byte[escaped.length()];
    int byteIndex = 0;
    int i = 0;

    while (i < escaped.length()) {
      char c = escaped.charAt(i);
      if (c == '\\' && i + 1 < escaped.length()) {
        char next = escaped.charAt(i + 1);
        if (next >= '0' && next <= '7') {
          // Octal escape
          int value = 0;
          int count = 0;
          while (i + 1 < escaped.length()
              && count < 3
              && escaped.charAt(i + 1) >= '0'
              && escaped.charAt(i + 1) <= '7') {
            value = value * 8 + (escaped.charAt(i + 1) - '0');
            i++;
            count++;
          }
          bytes[byteIndex++] = (byte) value;
        } else if (next == 'n') {
          bytes[byteIndex++] = '\n';
          i++;
        } else if (next == 't') {
          bytes[byteIndex++] = '\t';
          i++;
        } else if (next == 'r') {
          bytes[byteIndex++] = '\r';
          i++;
        } else if (next == '\\') {
          bytes[byteIndex++] = '\\';
          i++;
        } else if (next == '"') {
          bytes[byteIndex++] = '"';
          i++;
        } else if (next == 'x' && i + 3 < escaped.length()) {
          // Hex escape \xNN
          int value = Integer.parseInt(escaped.substring(i + 2, i + 4), 16);
          bytes[byteIndex++] = (byte) value;
          i += 3;
        } else {
          bytes[byteIndex++] = (byte) c;
        }
      } else {
        bytes[byteIndex++] = (byte) c;
      }
      i++;
    }

    return ByteString.copyFrom(bytes, 0, byteIndex);
  }
}
