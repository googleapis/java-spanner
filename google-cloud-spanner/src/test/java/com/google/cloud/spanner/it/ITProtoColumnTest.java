/*
 * Copyright 2022 Google LLC
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

package com.google.cloud.spanner.it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeFalse;

import com.google.cloud.ByteArray;
import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.DatabaseId;
import com.google.cloud.spanner.KeySet;
import com.google.cloud.spanner.Mutation;
import com.google.cloud.spanner.ParallelIntegrationTest;
import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.SingerProto.Genre;
import com.google.cloud.spanner.SingerProto.SingerInfo;
import com.google.cloud.spanner.Spanner;
import com.google.cloud.spanner.SpannerOptions;
import com.google.cloud.spanner.Statement;
import com.google.cloud.spanner.testing.EmulatorSpannerHelper;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.AbstractMessage;
import com.google.protobuf.ProtocolMessageEnum;
import java.util.Arrays;
import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Integrations Tests to test Proto Columns & Enums */
@Category(ParallelIntegrationTest.class)
@RunWith(JUnit4.class)
public class ITProtoColumnTest {

  private static String projectId;
  private static String instanceId;
  private static String databaseId;
  private static DatabaseClient databaseClient;
  private static String tableName;

  @BeforeClass
  public static void beforeClass() throws Exception {
    assumeFalse(
        "Proto Column is not supported in the emulator", EmulatorSpannerHelper.isUsingEmulator());

    projectId = "span-cloud-testing";
    databaseId = "go_int_test_proto_column_db";
    instanceId = "go-int-test-proto-column";

    Spanner spanner =
        SpannerOptions.newBuilder()
            .setHost("https://staging-wrenchworks.sandbox.googleapis.com")
            .setProjectId(projectId)
            .build()
            .getService();

    databaseClient = spanner.getDatabaseClient(DatabaseId.of(projectId, instanceId, databaseId));
    tableName = "Types";
  }

  @AfterClass
  public static void afterClass() throws Exception {
    databaseClient.write(ImmutableList.of(Mutation.delete(tableName, KeySet.all())));
  }

  /**
   * Assuming Types table with following DDL:
   *
   * <p>CREATE TABLE Types ( RowID INT64 NOT NULL, Int64a INT64, Bytes BYTES(MAX), Int64Array
   * ARRAY<INT64>, BytesArray ARRAY<BYTES(MAX)>, ProtoMessage spanner.examples.music.SingerInfo,
   * ProtoEnum spanner.examples.music.Genre, ProtoMessageArray
   * ARRAY<spanner.examples.music.SingerInfo>, ProtoEnumArray ARRAY<spanner.examples.music.Genre>, )
   * PRIMARY KEY (RowID);
   */
  @Test
  public void testProtoUpdateAndRead() {
    assumeFalse(
        "Proto Column is not supported in the emulator", EmulatorSpannerHelper.isUsingEmulator());
    SingerInfo singerInfo =
        SingerInfo.newBuilder().setSingerId(11).setNationality("Country1").build();
    ByteArray singerInfoBytes = ByteArray.copyFrom(singerInfo.toByteArray());

    Genre genre = Genre.JAZZ;
    long genreConst = genre.getNumber();

    List<AbstractMessage> singerInfoList =
        Arrays.asList(singerInfo, null, SingerInfo.getDefaultInstance());
    List<ByteArray> singerInfoBytesList =
        Arrays.asList(
            singerInfoBytes,
            null,
            ByteArray.copyFrom(SingerInfo.getDefaultInstance().toByteArray()));

    List<ProtocolMessageEnum> enumList = Arrays.asList(Genre.FOLK, null, Genre.ROCK);
    List<Long> enumConstList =
        Arrays.asList((long) Genre.FOLK_VALUE, null, (long) Genre.ROCK_VALUE);

    databaseClient.write(
        ImmutableList.of(
            Mutation.newInsertOrUpdateBuilder(tableName)
                .set("RowID")
                .to(11)
                .set("Int64a")
                .to(genreConst)
                .set("Bytes")
                .to(singerInfoBytes)
                .set("Int64Array")
                .toInt64Array(enumConstList)
                .set("BytesArray")
                .toBytesArray(singerInfoBytesList)
                .set("ProtoMessage")
                .to(singerInfo)
                .set("ProtoEnum")
                .to(genre)
                .set("ProtoMessageArray")
                .toProtoMessageArray(singerInfoList, SingerInfo.getDescriptor())
                .set("ProtoEnumArray")
                .toProtoEnumArray(enumList, Genre.getDescriptor())
                .build()));

    try (ResultSet resultSet =
        databaseClient.singleUse().executeQuery(Statement.of("SELECT * FROM " + tableName))) {

      resultSet.next();
      assertEquals(11, resultSet.getLong("RowID"));
      assertEquals(genreConst, resultSet.getLong("Int64a"));
      assertEquals(singerInfoBytes, resultSet.getBytes("Bytes"));
      assertEquals(enumConstList, resultSet.getLongList("Int64Array"));
      assertEquals(singerInfoBytesList, resultSet.getBytesList("BytesArray"));
      assertEquals(
          singerInfo, resultSet.getProtoMessage("ProtoMessage", SingerInfo.getDefaultInstance()));
      assertEquals(genre, resultSet.getProtoEnum("ProtoEnum", Genre::forNumber));
      assertEquals(
          singerInfoList,
          resultSet.getProtoMessageList("ProtoMessageArray", SingerInfo.getDefaultInstance()));
      assertEquals(enumList, resultSet.getProtoEnumList("ProtoEnumArray", Genre::forNumber));

      // Check compatability between Proto Messages & Bytes
      assertEquals(singerInfoBytes, resultSet.getBytes("ProtoMessage"));
      assertEquals(singerInfo, resultSet.getProtoMessage("Bytes", SingerInfo.getDefaultInstance()));

      assertEquals(singerInfoBytesList, resultSet.getBytesList("ProtoMessageArray"));
      assertEquals(
          singerInfoList,
          resultSet.getProtoMessageList("BytesArray", SingerInfo.getDefaultInstance()));

      // Check compatability between Proto Enum & Int64
      assertEquals(genreConst, resultSet.getLong("ProtoEnum"));
      assertEquals(genre, resultSet.getProtoEnum("Int64a", Genre::forNumber));

      assertEquals(enumConstList, resultSet.getLongList("ProtoEnumArray"));
      assertEquals(enumList, resultSet.getProtoEnumList("Int64Array", Genre::forNumber));
    }
  }
}
