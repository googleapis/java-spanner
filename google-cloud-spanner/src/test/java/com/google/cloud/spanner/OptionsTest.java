/*
 * Copyright 2017 Google LLC
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

package com.google.cloud.spanner;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.cloud.spanner.Options.RpcPriority;
import com.google.spanner.v1.RequestOptions.Priority;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Unit tests for {@link Options}. */
@RunWith(JUnit4.class)
public class OptionsTest {

  @Test
  public void negativeLimitsNotAllowed() {
    try {
      Options.limit(-1);
      fail("Expected exception");
    } catch (IllegalArgumentException ex) {
      assertNotNull(ex.getMessage());
    }
  }

  @Test
  public void zeroLimitNotAllowed() {
    try {
      Options.limit(0);
      fail("Expected exception");
    } catch (IllegalArgumentException ex) {
      assertNotNull(ex.getMessage());
    }
  }

  @Test
  public void negativePrefetchChunksNotAllowed() {
    try {
      Options.prefetchChunks(-1);
      fail("Expected exception");
    } catch (IllegalArgumentException ex) {
      assertNotNull(ex.getMessage());
    }
  }

  @Test
  public void zeroPrefetchChunksNotAllowed() {
    try {
      Options.prefetchChunks(0);
      fail("Expected exception");
    } catch (IllegalArgumentException ex) {
      assertNotNull(ex.getMessage());
    }
  }

  @Test
  public void allOptionsPresent() {
    Options options = Options.fromReadOptions(Options.limit(10), Options.prefetchChunks(1));
    assertThat(options.hasLimit()).isTrue();
    assertThat(options.limit()).isEqualTo(10);
    assertThat(options.hasPrefetchChunks()).isTrue();
    assertThat(options.prefetchChunks()).isEqualTo(1);
  }

  @Test
  public void allOptionsAbsent() {
    Options options = Options.fromReadOptions();
    assertThat(options.hasLimit()).isFalse();
    assertThat(options.hasPrefetchChunks()).isFalse();
    assertThat(options.hasFilter()).isFalse();
    assertThat(options.hasPageToken()).isFalse();
    assertThat(options.hasPriority()).isFalse();
    assertThat(options.toString()).isEqualTo("");
    assertThat(options.equals(options)).isTrue();
    assertThat(options.equals(null)).isFalse();
    assertThat(options.equals(this)).isFalse();

    assertThat(options.hashCode()).isEqualTo(31);
  }

  @Test
  public void listOptTest() {
    int pageSize = 3;
    String pageToken = "ptok";
    String filter = "env";
    Options opts =
        Options.fromListOptions(
            Options.pageSize(pageSize), Options.pageToken(pageToken), Options.filter(filter));

    assertThat(opts.toString())
        .isEqualTo(
            "pageSize: "
                + Integer.toString(pageSize)
                + " pageToken: "
                + pageToken
                + " filter: "
                + filter
                + " ");

    assertThat(opts.hasPageSize()).isTrue();
    assertThat(opts.hasPageToken()).isTrue();
    assertThat(opts.hasFilter()).isTrue();

    assertThat(opts.pageSize()).isEqualTo(pageSize);
    assertThat(opts.pageToken()).isEqualTo(pageToken);
    assertThat(opts.filter()).isEqualTo(filter);
    assertThat(opts.hashCode()).isEqualTo(108027089);
  }

  @Test
  public void listEquality() {
    Options o1;
    Options o2;
    Options o3;

    o1 = Options.fromListOptions();
    o2 = Options.fromListOptions();
    assertThat(o1.equals(o2)).isTrue();

    o2 = Options.fromListOptions(Options.pageSize(1));
    assertThat(o1.equals(o2)).isFalse();
    assertThat(o2.equals(o1)).isFalse();

    o3 = Options.fromListOptions(Options.pageSize(1));
    assertThat(o2.equals(o3)).isTrue();

    o3 = Options.fromListOptions(Options.pageSize(2));
    assertThat(o2.equals(o3)).isFalse();

    o2 = Options.fromListOptions(Options.pageToken("t1"));
    assertThat(o1.equals(o2)).isFalse();

    o3 = Options.fromListOptions(Options.pageToken("t1"));
    assertThat(o2.equals(o3)).isTrue();

    o3 = Options.fromListOptions(Options.pageToken("t2"));
    assertThat(o2.equals(o3)).isFalse();

    o2 = Options.fromListOptions(Options.filter("f1"));
    assertThat(o1.equals(o2)).isFalse();

    o3 = Options.fromListOptions(Options.filter("f1"));
    assertThat(o2.equals(o3)).isTrue();

    o3 = Options.fromListOptions(Options.filter("f2"));
    assertThat(o2.equals(o3)).isFalse();
  }

  @Test
  public void readOptTest() {
    int limit = 3;
    Options opts = Options.fromReadOptions(Options.limit(limit));

    assertThat(opts.toString()).isEqualTo("limit: " + Integer.toString(limit) + " ");
    assertThat(opts.hashCode()).isEqualTo(964);
  }

  @Test
  public void readEquality() {
    Options o1;
    Options o2;
    Options o3;

    o1 = Options.fromReadOptions();
    o2 = Options.fromReadOptions();
    assertThat(o1.equals(o2)).isTrue();

    o2 = Options.fromReadOptions(Options.limit(1));
    assertThat(o1.equals(o2)).isFalse();
    assertThat(o2.equals(o1)).isFalse();

    o3 = Options.fromReadOptions(Options.limit(1));
    assertThat(o2.equals(o3)).isTrue();

    o3 = Options.fromReadOptions(Options.limit(2));
    assertThat(o2.equals(o3)).isFalse();
  }

  @Test
  public void queryOptTest() {
    int chunks = 3;
    Options opts = Options.fromQueryOptions(Options.prefetchChunks(chunks));
    assertThat(opts.toString()).isEqualTo("prefetchChunks: " + Integer.toString(chunks) + " ");
    assertThat(opts.prefetchChunks()).isEqualTo(chunks);
    assertThat(opts.hashCode()).isEqualTo(964);
  }

  @Test
  public void queryEquality() {
    Options o1;
    Options o2;
    Options o3;

    o1 = Options.fromQueryOptions();
    o2 = Options.fromQueryOptions();
    assertThat(o1.equals(o2)).isTrue();

    o2 = Options.fromReadOptions(Options.prefetchChunks(1));
    assertThat(o1.equals(o2)).isFalse();
    assertThat(o2.equals(o1)).isFalse();

    o3 = Options.fromReadOptions(Options.prefetchChunks(1));
    assertThat(o2.equals(o3)).isTrue();

    o3 = Options.fromReadOptions(Options.prefetchChunks(2));
    assertThat(o2.equals(o3)).isFalse();
  }

  @Test
  public void testFromTransactionOptions_toStringNoOptions() {
    Options opts = Options.fromTransactionOptions();
    assertThat(opts.toString()).isEqualTo("");
  }

  @Test
  public void testFromTransactionOptions_toStringWithCommitStats() {
    Options options = Options.fromTransactionOptions(Options.commitStats());
    assertThat(options.toString()).contains("withCommitStats: true");
  }

  @Test
  public void testTransactionOptions_noOptionsAreEqual() {
    Options option1 = Options.fromTransactionOptions();
    Options option2 = Options.fromTransactionOptions();
    assertEquals(option1, option2);
  }

  @Test
  public void testTransactionOptions_withCommitStatsAreEqual() {
    Options option1 = Options.fromTransactionOptions(Options.commitStats());
    Options option2 = Options.fromTransactionOptions(Options.commitStats());
    assertEquals(option1, option2);
  }

  @Test
  public void testTransactionOptions_withCommitStatsAndOtherOptionAreNotEqual() {
    Options option1 = Options.fromTransactionOptions(Options.commitStats());
    Options option2 = Options.fromQueryOptions(Options.prefetchChunks(10));
    assertNotEquals(option1, option2);
  }

  @Test
  public void testTransactionOptions_noOptionsHashCode() {
    Options option1 = Options.fromTransactionOptions();
    Options option2 = Options.fromTransactionOptions();
    assertEquals(option2.hashCode(), option1.hashCode());
  }

  @Test
  public void testTransactionOptions_withCommitStatsHashCode() {
    Options option1 = Options.fromTransactionOptions(Options.commitStats());
    Options option2 = Options.fromTransactionOptions(Options.commitStats());
    assertEquals(option2.hashCode(), option1.hashCode());
  }

  @Test
  public void testTransactionOptions_withCommitStatsAndOtherOptionHashCode() {
    Options option1 = Options.fromTransactionOptions(Options.commitStats());
    Options option2 = Options.fromQueryOptions(Options.prefetchChunks(10));
    assertNotEquals(option2.hashCode(), option1.hashCode());
  }

  @Test
  public void testTransactionOptionsPriority() {
    RpcPriority priority = RpcPriority.HIGH;
    Options options = Options.fromTransactionOptions(Options.priority(priority));
    assertTrue(options.hasPriority());
    assertEquals("priority: " + priority + " ", options.toString());
  }

  @Test
  public void testQueryOptionsPriority() {
    RpcPriority priority = RpcPriority.MEDIUM;
    Options options = Options.fromQueryOptions(Options.priority(priority));
    assertTrue(options.hasPriority());
    assertEquals("priority: " + priority + " ", options.toString());
  }

  @Test
  public void testReadOptionsPriority() {
    RpcPriority priority = RpcPriority.LOW;
    Options options = Options.fromReadOptions(Options.priority(priority));
    assertTrue(options.hasPriority());
    assertEquals("priority: " + priority + " ", options.toString());
  }

  @Test
  public void testUpdateOptionsPriority() {
    RpcPriority priority = RpcPriority.LOW;
    Options options = Options.fromUpdateOptions(Options.priority(priority));
    assertTrue(options.hasPriority());
    assertEquals("priority: " + priority + " ", options.toString());
  }

  @Test
  public void testTransactionOptionsHashCode() {
    Options option1 = Options.fromTransactionOptions();
    Options option2 = Options.fromTransactionOptions();
    assertEquals(option1.hashCode(), option2.hashCode());
  }

  @Test
  public void testTransactionOptionsWithPriorityEquality() {
    Options optionsWithHighPriority1 =
        Options.fromTransactionOptions(Options.priority(RpcPriority.HIGH));
    Options optionsWithHighPriority2 =
        Options.fromTransactionOptions(Options.priority(RpcPriority.HIGH));
    assertTrue(optionsWithHighPriority1.equals(optionsWithHighPriority2));

    Options optionsWithMediumPriority =
        Options.fromTransactionOptions(Options.priority(RpcPriority.MEDIUM));
    assertFalse(optionsWithHighPriority1.equals(optionsWithMediumPriority));
  }

  @Test
  public void testTransactionOptionsWithPriorityHashCode() {
    Options optionsWithHighPriority1 =
        Options.fromTransactionOptions(Options.priority(RpcPriority.HIGH));
    Options optionsWithHighPriority2 =
        Options.fromTransactionOptions(Options.priority(RpcPriority.HIGH));
    assertEquals(optionsWithHighPriority1.hashCode(), optionsWithHighPriority2.hashCode());

    Options optionsWithMediumPriority =
        Options.fromTransactionOptions(Options.priority(RpcPriority.MEDIUM));
    assertNotEquals(optionsWithHighPriority1.hashCode(), optionsWithMediumPriority.hashCode());
  }

  @Test
  public void testUpdateOptionsEquality() {
    Options option1 = Options.fromUpdateOptions();
    Options option2 = Options.fromUpdateOptions();
    assertTrue(option1.equals(option2));
  }

  @Test
  public void testUpdateOptionsHashCode() {
    Options option1 = Options.fromUpdateOptions();
    Options option2 = Options.fromUpdateOptions();
    assertEquals(option1.hashCode(), option2.hashCode());
  }

  @Test
  public void testUpdateOptionsWithPriorityEquality() {
    Options optionsWithHighPriority1 =
        Options.fromUpdateOptions(Options.priority(RpcPriority.HIGH));
    Options optionsWithHighPriority2 =
        Options.fromUpdateOptions(Options.priority(RpcPriority.HIGH));
    assertTrue(optionsWithHighPriority1.equals(optionsWithHighPriority2));

    Options optionsWithMediumPriority =
        Options.fromUpdateOptions(Options.priority(RpcPriority.MEDIUM));
    assertFalse(optionsWithHighPriority1.equals(optionsWithMediumPriority));
  }

  @Test
  public void testUpdateOptionsWithPriorityHashCode() {
    Options optionsWithHighPriority1 =
        Options.fromUpdateOptions(Options.priority(RpcPriority.HIGH));
    Options optionsWithHighPriority2 =
        Options.fromUpdateOptions(Options.priority(RpcPriority.HIGH));
    assertEquals(optionsWithHighPriority1.hashCode(), optionsWithHighPriority2.hashCode());

    Options optionsWithMediumPriority =
        Options.fromUpdateOptions(Options.priority(RpcPriority.MEDIUM));
    assertNotEquals(optionsWithHighPriority1.hashCode(), optionsWithMediumPriority.hashCode());
  }

  @Test
  public void testQueryOptionsEquality() {
    Options option1 = Options.fromQueryOptions();
    Options option2 = Options.fromQueryOptions();
    assertTrue(option1.equals(option2));
  }

  @Test
  public void testQueryOptionsHashCode() {
    Options option1 = Options.fromQueryOptions();
    Options option2 = Options.fromQueryOptions();
    assertEquals(option1.hashCode(), option2.hashCode());
  }

  @Test
  public void testQueryOptionsWithPriorityEquality() {
    Options optionsWithHighPriority1 = Options.fromQueryOptions(Options.priority(RpcPriority.HIGH));
    Options optionsWithHighPriority2 = Options.fromQueryOptions(Options.priority(RpcPriority.HIGH));
    assertTrue(optionsWithHighPriority1.equals(optionsWithHighPriority2));

    Options optionsWithMediumPriority =
        Options.fromQueryOptions(Options.priority(RpcPriority.MEDIUM));
    assertFalse(optionsWithHighPriority1.equals(optionsWithMediumPriority));

    Options optionsWithHighPriorityAndBufferRows =
        Options.fromQueryOptions(Options.priority(RpcPriority.HIGH), Options.bufferRows(10));
    assertFalse(optionsWithHighPriorityAndBufferRows.equals(optionsWithHighPriority1));
  }

  @Test
  public void testQueryOptionsWithPriorityHashCode() {
    Options optionsWithHighPriority1 = Options.fromQueryOptions(Options.priority(RpcPriority.HIGH));
    Options optionsWithHighPriority2 = Options.fromQueryOptions(Options.priority(RpcPriority.HIGH));
    assertEquals(optionsWithHighPriority1.hashCode(), optionsWithHighPriority2.hashCode());

    Options optionsWithMediumPriority =
        Options.fromQueryOptions(Options.priority(RpcPriority.MEDIUM));
    assertNotEquals(optionsWithHighPriority1.hashCode(), optionsWithMediumPriority.hashCode());

    Options optionsWithHighPriorityAndBufferRows =
        Options.fromQueryOptions(Options.priority(RpcPriority.HIGH), Options.bufferRows(10));
    assertNotEquals(
        optionsWithHighPriorityAndBufferRows.hashCode(), optionsWithHighPriority1.hashCode());
  }

  @Test
  public void testReadOptionsEquality() {
    Options option1 = Options.fromReadOptions();
    Options option2 = Options.fromReadOptions();
    assertTrue(option1.equals(option2));
  }

  @Test
  public void testReadOptionsHashCode() {
    Options option1 = Options.fromReadOptions();
    Options option2 = Options.fromReadOptions();
    assertEquals(option1.hashCode(), option2.hashCode());
  }

  @Test
  public void testReadOptionsWithPriorityEquality() {
    Options optionsWithHighPriority1 = Options.fromReadOptions(Options.priority(RpcPriority.HIGH));
    Options optionsWithHighPriority2 = Options.fromReadOptions(Options.priority(RpcPriority.HIGH));
    assertTrue(optionsWithHighPriority1.equals(optionsWithHighPriority2));

    Options optionsWithMediumPriority =
        Options.fromReadOptions(Options.priority(RpcPriority.MEDIUM));
    assertFalse(optionsWithHighPriority1.equals(optionsWithMediumPriority));

    Options optionsWithHighPriorityAndBufferRows =
        Options.fromReadOptions(Options.priority(RpcPriority.HIGH), Options.bufferRows(10));
    assertFalse(optionsWithHighPriorityAndBufferRows.equals(optionsWithHighPriority1));
  }

  @Test
  public void testReadOptionsWithPriorityHashCode() {
    Options optionsWithHighPriority1 = Options.fromReadOptions(Options.priority(RpcPriority.HIGH));
    Options optionsWithHighPriority2 = Options.fromReadOptions(Options.priority(RpcPriority.HIGH));
    assertEquals(optionsWithHighPriority1.hashCode(), optionsWithHighPriority2.hashCode());

    Options optionsWithMediumPriority =
        Options.fromReadOptions(Options.priority(RpcPriority.MEDIUM));
    assertNotEquals(optionsWithHighPriority1.hashCode(), optionsWithMediumPriority.hashCode());

    Options optionsWithHighPriorityAndBufferRows =
        Options.fromReadOptions(Options.priority(RpcPriority.HIGH), Options.bufferRows(10));
    assertNotEquals(
        optionsWithHighPriorityAndBufferRows.hashCode(), optionsWithHighPriority1.hashCode());
  }

  @Test
  public void testFromUpdateOptions() {
    Options options = Options.fromUpdateOptions();
    assertThat(options.toString()).isEqualTo("");
  }

  @Test
  public void testTransactionOptions() {
    RpcPriority prio = RpcPriority.HIGH;
    Options opts = Options.fromTransactionOptions(Options.priority(prio));
    assertThat(opts.toString()).isEqualTo("priority: " + prio + " ");
    assertThat(opts.priority()).isEqualTo(Priority.PRIORITY_HIGH);
  }

  @Test
  public void testTransactionOptionsEquality() {
    Options o1;
    Options o2;
    Options o3;

    o1 = Options.fromTransactionOptions();
    o2 = Options.fromTransactionOptions();
    assertThat(o1.equals(o2)).isTrue();

    o2 = Options.fromTransactionOptions(Options.priority(RpcPriority.HIGH));
    assertThat(o1.equals(o2)).isFalse();
    assertThat(o2.equals(o1)).isFalse();

    o3 = Options.fromTransactionOptions(Options.priority(RpcPriority.HIGH));
    assertThat(o2.equals(o3)).isTrue();

    o3 = Options.fromTransactionOptions(Options.priority(RpcPriority.LOW));
    assertThat(o2.equals(o3)).isFalse();
  }
}
