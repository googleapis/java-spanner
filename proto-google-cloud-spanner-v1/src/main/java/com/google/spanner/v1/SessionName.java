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

package com.google.spanner.v1;

import com.google.api.pathtemplate.PathTemplate;
import com.google.api.resourcenames.ResourceName;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** AUTO-GENERATED DOCUMENTATION AND CLASS */
@javax.annotation.Generated("by GAPIC protoc plugin")
public class SessionName implements ResourceName {

  private static final PathTemplate PATH_TEMPLATE =
      PathTemplate.createWithoutUrlEncoding(
          "projects/{project}/instances/{instance}/databases/{database}/sessions/{session}");

  private volatile Map<String, String> fieldValuesMap;

  private final String project;
  private final String instance;
  private final String database;
  private final String session;

  public String getProject() {
    return project;
  }

  public String getInstance() {
    return instance;
  }

  public String getDatabase() {
    return database;
  }

  public String getSession() {
    return session;
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public Builder toBuilder() {
    return new Builder(this);
  }

  private SessionName(Builder builder) {
    project = Preconditions.checkNotNull(builder.getProject());
    instance = Preconditions.checkNotNull(builder.getInstance());
    database = Preconditions.checkNotNull(builder.getDatabase());
    session = Preconditions.checkNotNull(builder.getSession());
  }

  public static SessionName of(String project, String instance, String database, String session) {
    return newBuilder()
        .setProject(project)
        .setInstance(instance)
        .setDatabase(database)
        .setSession(session)
        .build();
  }

  public static String format(String project, String instance, String database, String session) {
    return newBuilder()
        .setProject(project)
        .setInstance(instance)
        .setDatabase(database)
        .setSession(session)
        .build()
        .toString();
  }

  public static SessionName parse(String formattedString) {
    if (formattedString.isEmpty()) {
      return null;
    }
    Map<String, String> matchMap =
        PATH_TEMPLATE.validatedMatch(
            formattedString, "SessionName.parse: formattedString not in valid format");
    return of(
        matchMap.get("project"),
        matchMap.get("instance"),
        matchMap.get("database"),
        matchMap.get("session"));
  }

  public static List<SessionName> parseList(List<String> formattedStrings) {
    List<SessionName> list = new ArrayList<>(formattedStrings.size());
    for (String formattedString : formattedStrings) {
      list.add(parse(formattedString));
    }
    return list;
  }

  public static List<String> toStringList(List<SessionName> values) {
    List<String> list = new ArrayList<String>(values.size());
    for (SessionName value : values) {
      if (value == null) {
        list.add("");
      } else {
        list.add(value.toString());
      }
    }
    return list;
  }

  public static boolean isParsableFrom(String formattedString) {
    return PATH_TEMPLATE.matches(formattedString);
  }

  public Map<String, String> getFieldValuesMap() {
    if (fieldValuesMap == null) {
      synchronized (this) {
        if (fieldValuesMap == null) {
          ImmutableMap.Builder<String, String> fieldMapBuilder = ImmutableMap.builder();
          fieldMapBuilder.put("project", project);
          fieldMapBuilder.put("instance", instance);
          fieldMapBuilder.put("database", database);
          fieldMapBuilder.put("session", session);
          fieldValuesMap = fieldMapBuilder.build();
        }
      }
    }
    return fieldValuesMap;
  }

  public String getFieldValue(String fieldName) {
    return getFieldValuesMap().get(fieldName);
  }

  @Override
  public String toString() {
    return PATH_TEMPLATE.instantiate(
        "project", project, "instance", instance, "database", database, "session", session);
  }

  /** Builder for SessionName. */
  public static class Builder {

    private String project;
    private String instance;
    private String database;
    private String session;

    public String getProject() {
      return project;
    }

    public String getInstance() {
      return instance;
    }

    public String getDatabase() {
      return database;
    }

    public String getSession() {
      return session;
    }

    public Builder setProject(String project) {
      this.project = project;
      return this;
    }

    public Builder setInstance(String instance) {
      this.instance = instance;
      return this;
    }

    public Builder setDatabase(String database) {
      this.database = database;
      return this;
    }

    public Builder setSession(String session) {
      this.session = session;
      return this;
    }

    private Builder() {}

    private Builder(SessionName sessionName) {
      project = sessionName.project;
      instance = sessionName.instance;
      database = sessionName.database;
      session = sessionName.session;
    }

    public SessionName build() {
      return new SessionName(this);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof SessionName) {
      SessionName that = (SessionName) o;
      return (this.project.equals(that.project))
          && (this.instance.equals(that.instance))
          && (this.database.equals(that.database))
          && (this.session.equals(that.session));
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h = 1;
    h *= 1000003;
    h ^= project.hashCode();
    h *= 1000003;
    h ^= instance.hashCode();
    h *= 1000003;
    h ^= database.hashCode();
    h *= 1000003;
    h ^= session.hashCode();
    return h;
  }
}
