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

import io.opencensus.common.Scope;

class DualScope implements IScope {

  private Scope openCensusScope;
  private io.opentelemetry.context.Scope openTelemetryScope;

  public DualScope(Scope openCensusScope, io.opentelemetry.context.Scope openTelemetryScope) {
    this.openCensusScope = openCensusScope;
    this.openTelemetryScope = openTelemetryScope;
  }

  @Override
  public void close() {
    if (openCensusScope != null) {
      openCensusScope.close();
    }
    if (openTelemetryScope != null) {
      openTelemetryScope.close();
    }
  }
}
