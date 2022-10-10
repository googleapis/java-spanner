/*
 * Copyright 2022 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: google/spanner/executor/v1/executor.proto

package com.google.spanner.executor.v1;

public interface MutationActionOrBuilder extends
    // @@protoc_insertion_point(interface_extends:google.spanner.executor.v1.MutationAction)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <code>repeated .google.spanner.executor.v1.MutationAction.Mod mod = 1;</code>
   */
  java.util.List<com.google.spanner.executor.v1.MutationAction.Mod> 
      getModList();
  /**
   * <code>repeated .google.spanner.executor.v1.MutationAction.Mod mod = 1;</code>
   */
  com.google.spanner.executor.v1.MutationAction.Mod getMod(int index);
  /**
   * <code>repeated .google.spanner.executor.v1.MutationAction.Mod mod = 1;</code>
   */
  int getModCount();
  /**
   * <code>repeated .google.spanner.executor.v1.MutationAction.Mod mod = 1;</code>
   */
  java.util.List<? extends com.google.spanner.executor.v1.MutationAction.ModOrBuilder> 
      getModOrBuilderList();
  /**
   * <code>repeated .google.spanner.executor.v1.MutationAction.Mod mod = 1;</code>
   */
  com.google.spanner.executor.v1.MutationAction.ModOrBuilder getModOrBuilder(
      int index);
}
