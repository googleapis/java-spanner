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

import com.google.protobuf.TextFormat;
import com.google.spanner.v1.CacheUpdate;
import com.google.spanner.v1.ExecuteSqlRequest;
import com.google.spanner.v1.ReadRequest;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ChannelFinderGoldenTest {

  @Test
  public void goldenTest() throws Exception {
    String content;
    try (InputStream inputStream =
        getClass().getClassLoader().getResourceAsStream("finder_test.textproto")) {
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
      ChannelFinder finder = new ChannelFinder(endpointCache, "instances/default/databases/db");
      finder.useDeterministicRandom();

      for (Event event : testCase.events) {
        if (event.cacheUpdate != null) {
          finder.update(event.cacheUpdate);
        }

        if (event.read != null) {
          endpointCache.setUnhealthyServers(event.unhealthyServers);
          ReadRequest.Builder builder = event.read.toBuilder();
          ChannelEndpoint endpoint = finder.findServer(builder);
          assertHintAndServer(testCase.name, event, builder.getRoutingHint(), endpoint);
        }

        if (event.sql != null) {
          endpointCache.setUnhealthyServers(event.unhealthyServers);
          ExecuteSqlRequest.Builder builder = event.sql.toBuilder();
          ChannelEndpoint endpoint = finder.findServer(builder);
          assertHintAndServer(testCase.name, event, builder.getRoutingHint(), endpoint);
        }
      }
    }
  }

  private static void assertHintAndServer(
      String testCaseName, Event event, RoutingHint actualHint, ChannelEndpoint endpoint) {
    assertEquals("RoutingHint mismatch for test case: " + testCaseName, event.hint, actualHint);
    if (event.serverAddress != null) {
      assertNotNull("Expected server for test case: " + testCaseName, endpoint);
      assertEquals(event.serverAddress, endpoint.getAddress());
    } else {
      assertNull("Expected no server for test case: " + testCaseName, endpoint);
    }
  }

  private static class TestCase {
    String name;
    List<Event> events = new ArrayList<>();
  }

  private static class Event {
    CacheUpdate cacheUpdate;
    ReadRequest read;
    ExecuteSqlRequest sql;
    RoutingHint hint = RoutingHint.getDefaultInstance();
    String serverAddress;
    Set<String> unhealthyServers = Collections.emptySet();
  }

  private List<TestCase> parseTestCases(String content) throws TextFormat.ParseException {
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

  private TestCase parseTestCase(String content) throws TextFormat.ParseException {
    TestCase testCase = new TestCase();
    String name = parseTopLevelFieldValue(content, "name");
    if (name != null) {
      testCase.name = unquote(name);
    }
    for (String eventContent : extractTopLevelBlocks(content, "event")) {
      testCase.events.add(parseEvent(eventContent));
    }
    return testCase;
  }

  private Event parseEvent(String content) throws TextFormat.ParseException {
    Event event = new Event();

    List<String> updateBlocks = extractTopLevelBlocks(content, "cache_update");
    if (!updateBlocks.isEmpty()) {
      CacheUpdate.Builder builder = CacheUpdate.newBuilder();
      TextFormat.merge(updateBlocks.get(0), builder);
      event.cacheUpdate = builder.build();
    }

    List<String> readBlocks = extractTopLevelBlocks(content, "read");
    if (!readBlocks.isEmpty()) {
      ReadRequest.Builder builder = ReadRequest.newBuilder();
      TextFormat.merge(readBlocks.get(0), builder);
      event.read = builder.build();
    }

    List<String> sqlBlocks = extractTopLevelBlocks(content, "sql");
    if (!sqlBlocks.isEmpty()) {
      ExecuteSqlRequest.Builder builder = ExecuteSqlRequest.newBuilder();
      TextFormat.merge(sqlBlocks.get(0), builder);
      event.sql = builder.build();
    }

    List<String> hintBlocks = extractTopLevelBlocks(content, "hint");
    if (!hintBlocks.isEmpty()) {
      RoutingHint.Builder builder = RoutingHint.newBuilder();
      TextFormat.merge(hintBlocks.get(0), builder);
      event.hint = builder.build();
    }

    String server = parseTopLevelFieldValue(content, "server");
    if (server != null) {
      event.serverAddress = unquote(server);
    }

    Set<String> unhealthyServers = parseTopLevelStringList(content, "unhealthy_servers");
    if (!unhealthyServers.isEmpty()) {
      event.unhealthyServers = unhealthyServers;
    }

    return event;
  }

  private static List<String> extractTopLevelBlocks(String content, String name) {
    List<String> blocks = new ArrayList<>();
    int depth = 0;
    boolean inQuotes = false;
    boolean escaped = false;
    int i = 0;
    while (i < content.length()) {
      if (!inQuotes && depth == 0 && matchesBlockNameAt(content, i, name)) {
        int braceStart = content.indexOf('{', i);
        int braceEnd = findMatchingBrace(content, braceStart);
        blocks.add(content.substring(braceStart + 1, braceEnd));
        i = braceEnd + 1;
        continue;
      }
      char c = content.charAt(i);
      if (escaped) {
        escaped = false;
      } else if (c == '\\') {
        escaped = true;
      } else if (c == '"') {
        inQuotes = !inQuotes;
      } else if (!inQuotes) {
        if (c == '{') {
          depth++;
        } else if (c == '}') {
          depth--;
        }
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
    boolean inQuotes = false;
    boolean escaped = false;
    for (int i = startIndex; i < content.length(); i++) {
      char c = content.charAt(i);
      if (escaped) {
        escaped = false;
        continue;
      }
      if (c == '\\') {
        escaped = true;
        continue;
      }
      if (c == '"') {
        inQuotes = !inQuotes;
        continue;
      }
      if (!inQuotes) {
        if (c == '{') {
          depth++;
        } else if (c == '}') {
          depth--;
          if (depth == 0) {
            return i;
          }
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

  private static Set<String> parseTopLevelStringList(String content, String fieldName) {
    Set<String> values = new HashSet<>();
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
        values.add(unquote(stripInlineComment(value)));
      }
      depth += countBraces(rawLine);
    }
    return values;
  }

  private static int countBraces(String line) {
    int count = 0;
    boolean inQuotes = false;
    boolean escaped = false;
    for (int i = 0; i < line.length(); i++) {
      char c = line.charAt(i);
      if (escaped) {
        escaped = false;
        continue;
      }
      if (c == '\\') {
        escaped = true;
        continue;
      }
      if (c == '"') {
        inQuotes = !inQuotes;
        continue;
      }
      if (!inQuotes) {
        if (c == '{') {
          count++;
        } else if (c == '}') {
          count--;
        }
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
    private volatile Set<String> unhealthyServers = Collections.emptySet();

    void setUnhealthyServers(Set<String> unhealthyServers) {
      this.unhealthyServers = unhealthyServers;
    }

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

    private final class FakeEndpoint implements ChannelEndpoint {
      private final String address;

      private FakeEndpoint(String address) {
        this.address = address;
      }

      @Override
      public String getAddress() {
        return address;
      }

      @Override
      public boolean isHealthy() {
        return !unhealthyServers.contains(address);
      }

      @Override
      public ManagedChannel getChannel() {
        return new ManagedChannel() {
          @Override
          public ManagedChannel shutdown() {
            return this;
          }

          @Override
          public ManagedChannel shutdownNow() {
            return this;
          }

          @Override
          public boolean isShutdown() {
            return false;
          }

          @Override
          public boolean isTerminated() {
            return false;
          }

          @Override
          public boolean awaitTermination(long timeout, TimeUnit unit) {
            return true;
          }

          @Override
          public <RequestT, ResponseT> ClientCall<RequestT, ResponseT> newCall(
              MethodDescriptor<RequestT, ResponseT> methodDescriptor, CallOptions callOptions) {
            throw new UnsupportedOperationException();
          }

          @Override
          public String authority() {
            return address;
          }
        };
      }
    }
  }
}
