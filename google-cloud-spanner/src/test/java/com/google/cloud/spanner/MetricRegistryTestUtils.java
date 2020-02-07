/*
 * Copyright 2020 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.cloud.spanner;

import com.google.common.collect.Maps;
import io.opencensus.common.ToLongFunction;
import io.opencensus.metrics.DerivedDoubleCumulative;
import io.opencensus.metrics.DerivedDoubleGauge;
import io.opencensus.metrics.DerivedLongCumulative;
import io.opencensus.metrics.DerivedLongGauge;
import io.opencensus.metrics.DoubleCumulative;
import io.opencensus.metrics.DoubleGauge;
import io.opencensus.metrics.LabelKey;
import io.opencensus.metrics.LabelValue;
import io.opencensus.metrics.LongCumulative;
import io.opencensus.metrics.LongGauge;
import io.opencensus.metrics.MetricOptions;
import io.opencensus.metrics.MetricRegistry;
import java.util.List;
import java.util.Map;

class MetricRegistryTestUtils {

  static class PointWithFunction<T> {
    private final T ref;
    private final ToLongFunction<T> function;

    PointWithFunction(T obj, ToLongFunction<T> function) {
      this.ref = obj;
      this.function = function;
    }

    long get() {
      return function.applyAsLong(ref);
    }
  }

  static class MetricsRecord {
    private final Map<String, PointWithFunction> metrics;
    private final Map<List<LabelKey>, List<LabelValue>> labels;

    private MetricsRecord() {
      this.metrics = Maps.newHashMap();
      this.labels = Maps.newHashMap();
    }

    Map<String, Number> getMetrics() {
      Map<String, Number> copy = Maps.newHashMap();
      for (Map.Entry<String, PointWithFunction> entry : metrics.entrySet()) {
        copy.put(entry.getKey(), entry.getValue().get());
      }
      return copy;
    }

    Map<List<LabelKey>, List<LabelValue>> getLabels() {
      return this.labels;
    }
  }

  public static final class FakeDerivedLongGauge extends DerivedLongGauge {
    private final MetricsRecord record;
    private final String name;
    private final List<LabelKey> labelKeys;

    private FakeDerivedLongGauge(
        FakeMetricRegistry metricRegistry, String name, List<LabelKey> labelKeys) {
      this.record = metricRegistry.record;
      this.labelKeys = labelKeys;
      this.name = name;
    }

    @Override
    public <T> void createTimeSeries(
        List<LabelValue> labelValues, T t, ToLongFunction<T> toLongFunction) {
      this.record.metrics.put(this.name, new PointWithFunction(t, toLongFunction));
      this.record.labels.put(this.labelKeys, labelValues);
    }

    @Override
    public void removeTimeSeries(List<LabelValue> list) {}

    @Override
    public void clear() {}
  }

  public static final class FakeDerivedLongCumulative extends DerivedLongCumulative {
    private final MetricsRecord record;
    private final String name;
    private final List<LabelKey> labelKeys;

    private FakeDerivedLongCumulative(
        FakeMetricRegistry metricRegistry, String name, List<LabelKey> labelKeys) {
      this.record = metricRegistry.record;
      this.labelKeys = labelKeys;
      this.name = name;
    }

    @Override
    public <T> void createTimeSeries(
        List<LabelValue> labelValues, T t, ToLongFunction<T> toLongFunction) {
      this.record.metrics.put(this.name, new PointWithFunction(t, toLongFunction));
      this.record.labels.put(this.labelKeys, labelValues);
    }

    @Override
    public void removeTimeSeries(List<LabelValue> list) {}

    @Override
    public void clear() {}
  }

  /**
   * A {@link MetricRegistry} implementation that saves metrics records to be accessible from {@link
   * #pollRecord()}.
   */
  public static final class FakeMetricRegistry extends MetricRegistry {

    private MetricsRecord record;

    FakeMetricRegistry() {
      record = new MetricsRecord();
    }

    MetricsRecord pollRecord() {
      return record;
    }

    @Override
    public DerivedLongGauge addDerivedLongGauge(String s, MetricOptions metricOptions) {
      return new FakeDerivedLongGauge(this, s, metricOptions.getLabelKeys());
    }

    @Override
    public LongGauge addLongGauge(String s, MetricOptions metricOptions) {
      throw new UnsupportedOperationException();
    }

    @Override
    public DoubleGauge addDoubleGauge(String s, MetricOptions metricOptions) {
      throw new UnsupportedOperationException();
    }

    @Override
    public DerivedDoubleGauge addDerivedDoubleGauge(String s, MetricOptions metricOptions) {
      throw new UnsupportedOperationException();
    }

    @Override
    public LongCumulative addLongCumulative(String s, MetricOptions metricOptions) {
      throw new UnsupportedOperationException();
    }

    @Override
    public DoubleCumulative addDoubleCumulative(String s, MetricOptions metricOptions) {
      throw new UnsupportedOperationException();
    }

    @Override
    public DerivedLongCumulative addDerivedLongCumulative(String s, MetricOptions metricOptions) {
      return new FakeDerivedLongCumulative(this, s, metricOptions.getLabelKeys());
    }

    @Override
    public DerivedDoubleCumulative addDerivedDoubleCumulative(
        String s, MetricOptions metricOptions) {
      throw new UnsupportedOperationException();
    }
  }
}
