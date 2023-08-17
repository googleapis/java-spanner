/*
 * Copyright 2023 Google LLC
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

import io.opentelemetry.sdk.metrics.InstrumentSelector;
import io.opentelemetry.sdk.metrics.View;
import java.util.Arrays;
import java.util.Optional;

/** */
public enum RPCViewImpl {
  SPANNER_GFE_LATENCY_VIEW(
      RPCViewAttributes.SPANNER_GFE_LATENCY_SELECTOR, RPCViewAttributes.SPANNER_GFE_LATENCY_VIEW),

  SPANNER_GFE_HEADER_MISSING_COUNT_VIEW(
      RPCViewAttributes.SPANNER_GFE_HEADER_MISSING_COUNT_SELECTOR,
      RPCViewAttributes.SPANNER_GFE_HEADER_MISSING_COUNT_VIEW);
  private InstrumentSelector instrumentSelector;
  private View view;

  public InstrumentSelector getInstrumentSelector() {
    return instrumentSelector;
  }

  public View getView() {
    return view;
  }

  RPCViewImpl(InstrumentSelector instrumentSelector, View view) {
    this.instrumentSelector = instrumentSelector;
    this.view = view;
  }

  public static Optional<RPCViewImpl> getRPCViewByName(String name) {
    return Arrays.stream(RPCViewImpl.values())
        .filter(rpcViewImpl -> rpcViewImpl.name().equalsIgnoreCase(name))
        .findFirst();
  }
}
