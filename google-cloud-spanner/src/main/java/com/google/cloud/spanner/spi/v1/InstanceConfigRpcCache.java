/*
 * Copyright 2020 Google LLC
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

package com.google.cloud.spanner.spi.v1;

import com.google.cloud.spanner.ErrorCode;
import com.google.cloud.spanner.SpannerException;
import com.google.cloud.spanner.SpannerExceptionFactory;
import com.google.cloud.spanner.SpannerOptions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.protobuf.FieldMask;
import com.google.spanner.admin.instance.v1.GetInstanceRequest;
import com.google.spanner.admin.instance.v1.Instance;
import com.google.spanner.admin.instance.v1.InstanceName;
import com.google.spanner.v1.DatabaseName;
import com.google.spanner.v1.SessionName;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

class InstanceConfigRpcCache {

  private final LoadingCache<InstanceName, SpannerRpc> cache;

  private Logger logger = Logger.getLogger(InstanceConfigRpcCache.class.getName());
  private final GapicSpannerRpc projectClient;

  private static final String RBR_ENABLED_FLAG = "GOOGLE_CLOUD_SPANNER_ENABLE_RESOURCE_BASED_ROUTING";

  private final boolean rbrEnabled;

  private static final String PERMISSIONS_ERROR_MSG = "The client library attempted to connect to an endpoint closer to your Cloud " +
  "Spanner data but was unable to do so. The client library will fall back and " +
  "route requests to the endpoint given in the client options, which may result in " +
  "increased latency. We recommend including the scope " +
  "https://www.googleapis.com/auth/spanner.admin so that the client library can " +
  "get an instance-specific endpoint and efficiently route requests.";

  InstanceConfigRpcCache(final GapicSpannerRpc projectClient) {
    this.projectClient = projectClient;
    this.rbrEnabled = Boolean.parseBoolean(System.getProperty(RBR_ENABLED_FLAG, "false"));
    cache =
        CacheBuilder.newBuilder()
            .build(
                new CacheLoader<InstanceName, SpannerRpc>() {
                  @Override
                  public SpannerRpc load(InstanceName instanceName) throws SpannerException {
                    if (!rbrEnabled) return projectClient;
                    GetInstanceRequest request =
                        GetInstanceRequest.newBuilder()
                            .setName(instanceName.toString())
                            .setFieldMask(FieldMask.newBuilder().addPaths("endpoint_uris"))
                            .build();
                    SpannerOptions.Builder optionsBuilder = projectClient.getOptions().toBuilder();
                    try {
                      Instance instance = projectClient.getInstance(request);
                      if (instance.getEndpointUrisCount() > 0) {
                        optionsBuilder.setHost(instance.getEndpointUris(0));
                      } else {
                        return projectClient;
                      }
                    } catch (SpannerException e) {
                      if (e.getErrorCode() == ErrorCode.UNIMPLEMENTED) {
                        // Ignore...
                        // This is backwards compatibility.
                        return projectClient;
                      } else if (e.getErrorCode() == ErrorCode.PERMISSION_DENIED) {
                        logger.log(Level.WARNING, PERMISSIONS_ERROR_MSG);
                        return projectClient;
                      } else {
                        logger.log(
                            Level.WARNING,
                            "Failed while resolving endpoint URLs for instance:"
                                + instanceName.toString()
                                + " reason: "
                                + e.getErrorCode().name());
                        throw e;
                      }
                    } catch (Exception e) {
                      logger.log(
                          Level.WARNING,
                          "Failed while resolving endpoint URLs for instance:"
                              + instanceName.toString());
                      throw SpannerExceptionFactory.newSpannerException(e);
                    }
                    return new GapicSpannerRpc(optionsBuilder.build(), false);
                  }
                });
  }

  SpannerRpc get(SessionName sessionName) {
    InstanceName instanceName =
        InstanceName.of(sessionName.getProject(), sessionName.getInstance());
    return get(instanceName);
  }

  SpannerRpc get(DatabaseName databaseName) {
    InstanceName instanceName =
        InstanceName.of(databaseName.getProject(), databaseName.getInstance());
    return get(instanceName);
  }

  private SpannerRpc get(InstanceName instanceName) {
    try {
      SpannerRpc spannerRpc = cache.get(instanceName);
      return spannerRpc;
    } catch (ExecutionException e) {
      logger.log(
          Level.FINE, "Failed looking up instance in cache. id:" + instanceName.toString(), e);
      throw SpannerExceptionFactory.newSpannerException(
          ErrorCode.NOT_FOUND,
          "Failed getting RPC Client for Instance:" + instanceName.toString(),
          e);
    }
  }

  void invalidateAll() {
    for (SpannerRpc rpc : cache.asMap().values()) {
      if (rpc == this.projectClient) continue;
      rpc.shutdown();
    }
    cache.invalidateAll();
  }
}
