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

import java.util.LinkedList;

class EventRate {
  static class Event {
    private final long timestamp;

    Event(long timestamp) {
      this.timestamp = timestamp;
    }
  }

  private final LinkedList<Event> events = new LinkedList<>();

  private final long windowSizeMillis;

  private final int maxSize;

  EventRate(long windowSizeMillis, int maxSize) {
    this.windowSizeMillis = windowSizeMillis;
    this.maxSize = maxSize;
  }

  /** Adds a new event and returns the current event count within the window. */
  int addEvent() {
    synchronized (this) {
      removeExpiredEvents();
      if (this.events.size() < maxSize) {
        this.events.addFirst(new Event(System.currentTimeMillis()));
      }
      return this.events.size();
    }
  }

  private void removeExpiredEvents() {
    // Remove events outside the event window.
    long threshold = System.currentTimeMillis() - this.windowSizeMillis;
    this.events.removeIf(event -> event.timestamp < threshold);
  }
}
