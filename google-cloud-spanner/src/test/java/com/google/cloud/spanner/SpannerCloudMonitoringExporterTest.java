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

package com.google.cloud.spanner;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutures;
import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.rpc.UnaryCallable;
import com.google.auth.Credentials;
import com.google.cloud.monitoring.v3.MetricServiceClient;
import com.google.cloud.monitoring.v3.stub.MetricServiceStub;
import com.google.common.collect.ImmutableList;
import com.google.monitoring.v3.CreateTimeSeriesRequest;
import com.google.monitoring.v3.TimeSeries;
import com.google.protobuf.Empty;
import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import io.opentelemetry.sdk.common.export.MemoryMode;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.data.LongPointData;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.internal.data.ImmutableLongPointData;
import io.opentelemetry.sdk.metrics.internal.data.ImmutableMetricData;
import io.opentelemetry.sdk.metrics.internal.data.ImmutableSumData;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static com.google.cloud.spanner.SpannerMetricsConstant.CLIENT_NAME_KEY;
import static com.google.cloud.spanner.SpannerMetricsConstant.CLIENT_UID_KEY;
import static com.google.cloud.spanner.SpannerMetricsConstant.DATABASE_KEY;
import static com.google.cloud.spanner.SpannerMetricsConstant.DIRECT_PATH_ENABLED_KEY;
import static com.google.cloud.spanner.SpannerMetricsConstant.DIRECT_PATH_USED_KEY;
import static com.google.cloud.spanner.SpannerMetricsConstant.INSTANCE_CONFIG_ID_KEY;
import static com.google.cloud.spanner.SpannerMetricsConstant.INSTANCE_ID_KEY;
import static com.google.cloud.spanner.SpannerMetricsConstant.LOCATION_ID_KEY;
import static com.google.cloud.spanner.SpannerMetricsConstant.OPERATION_LATENCIES_NAME;
import static com.google.cloud.spanner.SpannerMetricsConstant.PROJECT_ID_KEY;

import static com.google.common.truth.Truth.assertThat;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.resources.Resource;

import java.io.IOException;
import java.util.Arrays;

public class SpannerCloudMonitoringExporterTest {

    private static final String projectId = "fake-project";
    private static final String instanceId = "fake-instance";
    private static final String locationId = "default";
    private static final String tableId = "fake-table";
    private static final String zone = "us-east-1";
    private static final String cluster = "cluster-1";

    private static final String clientName = "fake-client-name";
    private static final String taskId = "fake-task-id";

    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private MetricServiceStub mockMetricServiceStub;
    private MetricServiceClient fakeMetricServiceClient;
    private SpannerCloudMonitoringExporter exporter;

    private Attributes attributes;
    private Resource resource;
    private InstrumentationScopeInfo scope;

    @Before
    public void setUp() {
        fakeMetricServiceClient = new FakeMetricServiceClient(mockMetricServiceStub);
        exporter = new SpannerCloudMonitoringExporter(projectId, fakeMetricServiceClient, taskId);

        attributes =
                Attributes.builder()
                        .put(PROJECT_ID_KEY, projectId)
                        .put(INSTANCE_ID_KEY, instanceId)
                        .put(LOCATION_ID_KEY, locationId)
                        .put(INSTANCE_CONFIG_ID_KEY, cluster)
                        .put(DATABASE_KEY, zone)
                        .put(String.valueOf(DIRECT_PATH_ENABLED_KEY), true)
                        .put(String.valueOf(DIRECT_PATH_USED_KEY), true)
                        .build();

        resource = Resource.create(Attributes.empty());

        scope = InstrumentationScopeInfo.create(SpannerMetricsConstant.GAX_METER_NAME);
    }

    @After
    public void tearDown() {
    }

    @Test
    public void createWhenNullCredentials() throws IOException {
        Credentials credentials =  null;
        SpannerCloudMonitoringExporter actualExporter = SpannerCloudMonitoringExporter.create(projectId, null);
        assertThat(actualExporter.getMemoryMode()).isEqualTo(MemoryMode.IMMUTABLE_DATA);
    }

    @Test
    public void createWhenValidCredentials() throws IOException {
        Credentials credentials =  new NoCredentialsProvider().getCredentials();
        SpannerCloudMonitoringExporter actualExporter = SpannerCloudMonitoringExporter.create(projectId, credentials);
        assertThat(actualExporter.getMemoryMode()).isEqualTo(MemoryMode.IMMUTABLE_DATA);
    }

    @Test
    public void testExportingSumData() {
        ArgumentCaptor<CreateTimeSeriesRequest> argumentCaptor =
                ArgumentCaptor.forClass(CreateTimeSeriesRequest.class);

        UnaryCallable<CreateTimeSeriesRequest, Empty> mockCallable = Mockito.mock(UnaryCallable.class);
        Mockito.when(mockMetricServiceStub.createServiceTimeSeriesCallable()).thenReturn(mockCallable);
        ApiFuture<Empty> future = ApiFutures.immediateFuture(Empty.getDefaultInstance());
        Mockito.when(mockCallable.futureCall(argumentCaptor.capture())).thenReturn(future);

        long fakeValue = 11L;

        long startEpoch = 10;
        long endEpoch = 15;
        LongPointData longPointData = ImmutableLongPointData.create(startEpoch, endEpoch, attributes, fakeValue);

        MetricData longData =
                ImmutableMetricData.createLongSum(
                        resource,
                        scope,
                        "spanner.googleapis.com/internal/client/" + OPERATION_LATENCIES_NAME,
                        "description",
                        "1",
                        ImmutableSumData.create(
                                true, AggregationTemporality.CUMULATIVE, ImmutableList.of(longPointData)));

        exporter.export(Arrays.asList(longData));

        CreateTimeSeriesRequest request = argumentCaptor.getValue();

        assertThat(request.getTimeSeriesList()).hasSize(1);

        TimeSeries timeSeries = request.getTimeSeriesList().get(0);

        assertThat(timeSeries.getResource().getLabelsMap())
                .containsExactly(
                        PROJECT_ID_KEY.getKey(), projectId,
                        INSTANCE_ID_KEY.getKey(), instanceId,
                        LOCATION_ID_KEY.getKey(), locationId,
                        INSTANCE_CONFIG_ID_KEY.getKey(), cluster,
                        DATABASE_KEY.getKey(), zone,
                        DIRECT_PATH_ENABLED_KEY.getKey(), true,
                        DIRECT_PATH_USED_KEY.getKey(), true);

        assertThat(timeSeries.getMetric().getLabelsMap()).hasSize(2);

        assertThat(timeSeries.getMetric().getLabelsMap())
                .containsAtLeast(CLIENT_NAME_KEY.getKey(), "java", CLIENT_UID_KEY.getKey(), taskId);
        assertThat(timeSeries.getPoints(0).getValue().getInt64Value()).isEqualTo(fakeValue);
        assertThat(timeSeries.getPoints(0).getInterval().getStartTime().getNanos())
                .isEqualTo(startEpoch);
        assertThat(timeSeries.getPoints(0).getInterval().getEndTime().getNanos()).isEqualTo(endEpoch);
    }

    @Test
    public void getAggregationTemporality() throws IOException {
        SpannerCloudMonitoringExporter actualExporter = SpannerCloudMonitoringExporter.create(projectId, null);
        assertThat(actualExporter.getAggregationTemporality(InstrumentType.COUNTER)).isEqualTo(AggregationTemporality.CUMULATIVE);
    }

    private static class FakeMetricServiceClient extends MetricServiceClient {

        protected FakeMetricServiceClient(MetricServiceStub stub) {
            super(stub);
        }
    }
}