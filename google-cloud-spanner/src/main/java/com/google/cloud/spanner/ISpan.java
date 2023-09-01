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

import io.opencensus.trace.Status;
import java.util.Map;

interface ISpan {

  void addAnnotation(String message, Map<String, Object> attributes);

  void addAnnotation(String message);

  void addAnnotation(String message, String key, String value);

  void addAnnotation(String message, String key, long value);

  void addAnnotation(String message, Throwable e);

  void setStatus(Throwable e);

  void setStatus(Status status);

  void end();
}
