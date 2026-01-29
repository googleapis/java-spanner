/*
 * Copyright 2026 Google LLC
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.google.protobuf.ByteString;
import com.google.protobuf.TextFormat;
import com.google.spanner.v1.CacheUpdate;
import com.google.spanner.v1.DirectedReadOptions;
import com.google.spanner.v1.RoutingHint;
import io.grpc.CallOptions;
import io.grpc.ClientCall;
import io.grpc.ManagedChannel;
import io.grpc.MethodDescriptor;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class KeyRangeCacheGoldenTest {

  private static final int DEFAULT_MIN_ENTRIES_FOR_RANDOM_PICK = 1000;

  @Test
  public void goldenTest() throws Exception {
    String content;
    try (InputStream inputStream =
        getClass().getClassLoader().getResourceAsStream("range_cache_test.textproto")) {
      content =
          new BufferedReader(
                  new InputStreamReader(
                      Objects.requireNonNull(inputStream), StandardCharsets.UTF_8))
              .lines()
              .reduce("", (a, b) -> a + "\n" + b);
    }

    List<TestCase> testCases = parseTestCases(content);

    for (TestCase testCase : testCases) {
      FakeEndpointCache endpointCache = new FakeEndpointCache();
      KeyRangeCache cache = new KeyRangeCache(endpointCache);
      cache.useDeterministicRandom();

      for (Step step : testCase.steps) {
        if (step.update != null) {
          cache.addRanges(step.update);
        }
        for (TestInstance test : step.tests) {
          cache.setMinCacheEntriesForRandomPick(DEFAULT_MIN_ENTRIES_FOR_RANDOM_PICK);
          if (test.minCacheEntriesForRandomPick.isPresent()) {
            cache.setMinCacheEntriesForRandomPick(test.minCacheEntriesForRandomPick.get());
          }

          RoutingHint.Builder hintBuilder = RoutingHint.newBuilder();
          if (test.key.isPresent()) {
            hintBuilder.setKey(test.key.get());
          }
          if (test.limitKey.isPresent()) {
            hintBuilder.setLimitKey(test.limitKey.get());
          }

          DirectedReadOptions directedReadOptions =
              test.directedReadOptions.orElse(DirectedReadOptions.getDefaultInstance());
          ChannelEndpoint server =
              cache.fillRoutingHint(test.leader, test.rangeMode, directedReadOptions, hintBuilder);

          assertEquals(
              "RoutingHint mismatch for test case: " + testCase.name,
              test.expectedResult,
              hintBuilder.build());
          if (test.serverAddress.isPresent()) {
            assertNotNull("Expected server for test case: " + testCase.name, server);
            assertEquals(test.serverAddress.get(), server.getAddress());
          } else {
            assertNull("Expected no server for test case: " + testCase.name, server);
          }
        }
      }

      cache.clear();
    }
  }

  private static class TestCase {
    String name;
    List<Step> steps = new ArrayList<>();
  }

  private static class Step {
    CacheUpdate update;
    List<TestInstance> tests = new ArrayList<>();
  }

  private static class TestInstance {
    boolean leader = false;
    Optional<ByteString> key = Optional.empty();
    Optional<ByteString> limitKey = Optional.empty();
    KeyRangeCache.RangeMode rangeMode = KeyRangeCache.RangeMode.COVERING_SPLIT;
    Optional<Integer> minCacheEntriesForRandomPick = Optional.empty();
    Optional<DirectedReadOptions> directedReadOptions = Optional.empty();
    RoutingHint expectedResult = RoutingHint.getDefaultInstance();
    Optional<String> serverAddress = Optional.empty();
  }

  private List<TestCase> parseTestCases(String content) throws Exception {
    List<TestCase> testCases = new ArrayList<>();
    int pos = 0;
    while (pos < content.length()) {
      int testCaseStart = content.indexOf("test_case", pos);
      if (testCaseStart == -1) {
        break;
      }
      int braceStart = content.indexOf('{', testCaseStart);
      if (braceStart == -1) {
        break;
      }
      int braceEnd = findMatchingBrace(content, braceStart);
      String testCaseContent = content.substring(braceStart + 1, braceEnd);
      testCases.add(parseTestCase(testCaseContent));
      pos = braceEnd + 1;
    }
    return testCases;
  }

  private TestCase parseTestCase(String content) throws Exception {
    TestCase testCase = new TestCase();
    String name = parseTopLevelFieldValue(content, "name");
    if (name != null) {
      testCase.name = unquote(name);
    }
    for (String stepContent : extractTopLevelBlocks(content, "step")) {
      testCase.steps.add(parseStep(stepContent));
    }
    return testCase;
  }

  private Step parseStep(String content) throws Exception {
    Step step = new Step();
    List<String> updates = extractTopLevelBlocks(content, "update");
    if (!updates.isEmpty()) {
      CacheUpdate.Builder builder = CacheUpdate.newBuilder();
      TextFormat.merge(updates.get(0), builder);
      step.update = builder.build();
    }

    for (String testContent : extractTopLevelBlocks(content, "test")) {
      step.tests.add(parseTest(testContent));
    }
    return step;
  }

  private TestInstance parseTest(String content) throws Exception {
    TestInstance test = new TestInstance();

    String leader = parseTopLevelFieldValue(content, "leader");
    if (leader != null) {
      test.leader = Boolean.parseBoolean(leader);
    }

    String keyValue = parseTopLevelFieldValue(content, "key");
    if (keyValue != null) {
      test.key = Optional.of(parseBytes(keyValue));
    }

    String limitKeyValue = parseTopLevelFieldValue(content, "limit_key");
    if (limitKeyValue != null) {
      test.limitKey = Optional.of(parseBytes(limitKeyValue));
    }

    String rangeMode = parseTopLevelFieldValue(content, "range_mode");
    if (rangeMode != null && rangeMode.contains("PICK_RANDOM")) {
      test.rangeMode = KeyRangeCache.RangeMode.PICK_RANDOM;
    }

    String minEntries = parseTopLevelFieldValue(content, "min_cache_entries_for_random_pick");
    if (minEntries != null) {
      test.minCacheEntriesForRandomPick = Optional.of(Integer.parseInt(minEntries));
    }

    List<String> directedOptionsBlocks = extractTopLevelBlocks(content, "directed_read_options");
    if (!directedOptionsBlocks.isEmpty()) {
      DirectedReadOptions.Builder builder = DirectedReadOptions.newBuilder();
      TextFormat.merge(directedOptionsBlocks.get(0), builder);
      test.directedReadOptions = Optional.of(builder.build());
    }

    List<String> resultBlocks = extractTopLevelBlocks(content, "result");
    if (!resultBlocks.isEmpty()) {
      RoutingHint.Builder builder = RoutingHint.newBuilder();
      TextFormat.merge(resultBlocks.get(0), builder);
      test.expectedResult = builder.build();
    }

    String server = parseTopLevelFieldValue(content, "server");
    if (server != null) {
      test.serverAddress = Optional.of(unquote(server));
    }

    return test;
  }

  private static List<String> extractTopLevelBlocks(String content, String name) {
    List<String> blocks = new ArrayList<>();
    int depth = 0;
    int i = 0;
    while (i < content.length()) {
      if (depth == 0 && matchesBlockNameAt(content, i, name)) {
        int braceStart = content.indexOf('{', i);
        int braceEnd = findMatchingBrace(content, braceStart);
        blocks.add(content.substring(braceStart + 1, braceEnd));
        i = braceEnd + 1;
        continue;
      }
      char c = content.charAt(i);
      if (c == '{') {
        depth++;
      } else if (c == '}') {
        depth--;
      }
      i++;
    }
    return blocks;
  }

  private static boolean matchesBlockNameAt(String content, int index, String name) {
    if (!content.regionMatches(index, name, 0, name.length())) {
      return false;
    }
    if (index > 0) {
      char before = content.charAt(index - 1);
      if (Character.isLetterOrDigit(before) || before == '_') {
        return false;
      }
    }
    int pos = index + name.length();
    while (pos < content.length() && Character.isWhitespace(content.charAt(pos))) {
      pos++;
    }
    return pos < content.length() && content.charAt(pos) == '{';
  }

  private static int findMatchingBrace(String content, int startIndex) {
    int depth = 0;
    for (int i = startIndex; i < content.length(); i++) {
      char c = content.charAt(i);
      if (c == '{') {
        depth++;
      } else if (c == '}') {
        depth--;
        if (depth == 0) {
          return i;
        }
      }
    }
    throw new IllegalArgumentException("No matching brace found");
  }

  private static String parseTopLevelFieldValue(String content, String fieldName) {
    int depth = 0;
    String[] lines = content.split("\n");
    for (String rawLine : lines) {
      String line = rawLine.trim();
      if (line.isEmpty()) {
        depth += countBraces(rawLine);
        continue;
      }
      if (line.startsWith("#")) {
        depth += countBraces(rawLine);
        continue;
      }
      if (depth == 0 && line.startsWith(fieldName + ":")) {
        String value = line.substring(fieldName.length() + 1).trim();
        return stripInlineComment(value);
      }
      depth += countBraces(rawLine);
    }
    return null;
  }

  private static int countBraces(String line) {
    int count = 0;
    for (int i = 0; i < line.length(); i++) {
      char c = line.charAt(i);
      if (c == '{') {
        count++;
      } else if (c == '}') {
        count--;
      }
    }
    return count;
  }

  private static String stripInlineComment(String value) {
    boolean inQuotes = false;
    boolean escaped = false;
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      if (escaped) {
        escaped = false;
        continue;
      }
      if (c == '\\') {
        escaped = true;
        continue;
      }
      if (c == '\"') {
        inQuotes = !inQuotes;
        continue;
      }
      if (!inQuotes && c == '#') {
        return value.substring(0, i).trim();
      }
    }
    return value.trim();
  }

  private static ByteString parseBytes(String value) throws TextFormat.ParseException {
    String unquoted = unquote(value);
    try {
      return TextFormat.unescapeBytes(unquoted);
    } catch (TextFormat.InvalidEscapeSequenceException e) {
      throw new TextFormat.ParseException(e.getMessage());
    }
  }

  private static String unquote(String value) {
    String trimmed = value.trim();
    if (trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
      return trimmed.substring(1, trimmed.length() - 1);
    }
    return trimmed;
  }

  private static final class FakeEndpointCache implements ChannelEndpointCache {
    private final Map<String, FakeEndpoint> endpoints = new HashMap<>();
    private final FakeEndpoint defaultEndpoint = new FakeEndpoint("default");

    @Override
    public ChannelEndpoint defaultChannel() {
      return defaultEndpoint;
    }

    @Override
    public ChannelEndpoint get(String address) {
      return endpoints.computeIfAbsent(address, FakeEndpoint::new);
    }

    @Override
    public void evict(String address) {
      endpoints.remove(address);
    }

    @Override
    public void shutdown() {
      endpoints.clear();
    }
  }

  private static final class FakeEndpoint implements ChannelEndpoint {
    private final String address;
    private final ManagedChannel channel = new FakeManagedChannel();

    FakeEndpoint(String address) {
      this.address = address;
    }

    @Override
    public String getAddress() {
      return address;
    }

    @Override
    public boolean isHealthy() {
      return true;
    }

    @Override
    public ManagedChannel getChannel() {
      return channel;
    }
  }

  private static final class FakeManagedChannel extends ManagedChannel {
    private boolean shutdown = false;

    @Override
    public ManagedChannel shutdown() {
      shutdown = true;
      return this;
    }

    @Override
    public boolean isShutdown() {
      return shutdown;
    }

    @Override
    public boolean isTerminated() {
      return shutdown;
    }

    @Override
    public ManagedChannel shutdownNow() {
      shutdown = true;
      return this;
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) {
      return shutdown;
    }

    @Override
    public <RequestT, ResponseT> ClientCall<RequestT, ResponseT> newCall(
        MethodDescriptor<RequestT, ResponseT> methodDescriptor, CallOptions callOptions) {
      throw new UnsupportedOperationException();
    }

    @Override
    public String authority() {
      return "fake";
    }
  }
}
