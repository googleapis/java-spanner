/*
 * Copyright 2017 Google LLC
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

import static com.google.common.base.Preconditions.checkState;

import com.google.api.gax.longrunning.OperationFuture;
import com.google.cloud.spanner.testing.EmulatorSpannerHelper;
import com.google.cloud.spanner.testing.RemoteSpannerHelper;
import com.google.common.collect.Iterators;
import com.google.spanner.admin.instance.v1.CreateInstanceMetadata;
import io.grpc.Status;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.rules.ExternalResource;

/**
 * JUnit 4 test rule that provides access to a Cloud Spanner instance to use for tests, and allows
 * uniquely named (per {@code IntegrationTestEnv} instance) test databases to be created within that
 * instance. An existing instance can be used by naming it in the {@link #TEST_INSTANCE_PROPERTY}
 * property; if the property is not set, an instance will be created and destroyed by the rule.
 *
 * <p>This class is normally used as a {@code @ClassRule}.
 */
public class IntegrationTestEnv extends ExternalResource {

  /** Names a property that provides the class name of the {@link TestEnvConfig} to use. */
  public static final String TEST_ENV_CONFIG_CLASS_NAME = "spanner.testenv.config.class";

  public static final String CONFIG_CLASS = System.getProperty(TEST_ENV_CONFIG_CLASS_NAME, null);

  /**
   * Names a property that, if set, identifies an existing Cloud Spanner instance to use for tests.
   */
  public static final String TEST_INSTANCE_PROPERTY = "spanner.testenv.instance";

  private static final Logger logger = Logger.getLogger(IntegrationTestEnv.class.getName());

  private TestEnvConfig config;
  private InstanceAdminClient instanceAdminClient;
  private boolean isOwnedInstance;
  private RemoteSpannerHelper testHelper;

  public RemoteSpannerHelper getTestHelper() {
    checkInitialized();
    return testHelper;
  }

  @SuppressWarnings("unchecked")
  protected void initializeConfig()
      throws ClassNotFoundException, InstantiationException, IllegalAccessException {
    if (CONFIG_CLASS == null) {
      throw new NullPointerException("Property " + TEST_ENV_CONFIG_CLASS_NAME + " needs to be set");
    }
    Class<? extends TestEnvConfig> configClass;
    configClass = (Class<? extends TestEnvConfig>) Class.forName(CONFIG_CLASS);
    config = configClass.newInstance();
  }

  @Override
  protected void before() throws Throwable {
    this.initializeConfig();
    this.config.setUp();

    SpannerOptions options = config.spannerOptions();
    String instanceProperty = System.getProperty(TEST_INSTANCE_PROPERTY, "");
    InstanceId instanceId;
    if (!instanceProperty.isEmpty()) {
      instanceId = InstanceId.of(instanceProperty);
      isOwnedInstance = false;
      logger.log(Level.INFO, "Using existing test instance: {0}", instanceId);
    } else {
      instanceId =
          InstanceId.of(
              config.spannerOptions().getProjectId(),
              String.format("test-instance-%08d", new Random().nextInt(100000000)));
      isOwnedInstance = true;
    }
    testHelper = createTestHelper(options, instanceId);
    instanceAdminClient = testHelper.getClient().getInstanceAdminClient();
    logger.log(Level.FINE, "Test env endpoint is {0}", options.getHost());
    if (isOwnedInstance) {
      initializeInstance(instanceId);
    }
  }

  RemoteSpannerHelper createTestHelper(SpannerOptions options, InstanceId instanceId)
      throws Throwable {
    return RemoteSpannerHelper.create(options, instanceId);
  }

  @Override
  protected void after() {
    cleanUpInstance();
    this.config.tearDown();
  }

  private void initializeInstance(InstanceId instanceId) {
    InstanceConfig instanceConfig =
        Iterators.get(instanceAdminClient.listInstanceConfigs().iterateAll().iterator(), 0, null);
    checkState(instanceConfig != null, "No instance configs found");

    InstanceConfigId configId = instanceConfig.getId();
    logger.log(Level.FINE, "Creating instance using config {0}", configId);
    InstanceInfo instance =
        InstanceInfo.newBuilder(instanceId)
            .setNodeCount(1)
            .setDisplayName("Test instance")
            .setInstanceConfigId(configId)
            .build();
    OperationFuture<Instance, CreateInstanceMetadata> op =
        instanceAdminClient.createInstance(instance);
    Instance createdInstance;
    try {
      createdInstance = op.get();
    } catch (Exception e) {
      boolean cancelled = false;
      try {
        // Try to cancel the createInstance operation.
        instanceAdminClient.cancelOperation(op.getName());
        com.google.longrunning.Operation createOperation =
            instanceAdminClient.getOperation(op.getName());
        cancelled =
            createOperation.hasError()
                && createOperation.getError().getCode() == Status.CANCELLED.getCode().value();
        if (cancelled) {
          logger.info("Cancelled the createInstance operation because the operation failed");
        } else {
          logger.info(
              "Tried to cancel the createInstance operation because the operation failed, but the operation could not be cancelled. Current status: "
                  + createOperation.getError().getCode());
        }
      } catch (Throwable t) {
        logger.log(Level.WARNING, "Failed to cancel the createInstance operation", t);
      }
      if (!cancelled) {
        try {
          instanceAdminClient.deleteInstance(instanceId.getInstance());
          logger.info(
              "Deleted the test instance because the createInstance operation failed and cancelling the operation did not succeed");
        } catch (Throwable t) {
          logger.log(Level.WARNING, "Failed to delete the test instance", t);
        }
      }
      throw SpannerExceptionFactory.newSpannerException(e);
    }
    logger.log(Level.INFO, "Created test instance: {0}", createdInstance.getId());
  }

  private void cleanUpInstance() {
    try {
      if (isOwnedInstance) {
        // Delete the instance, which implicitly drops all databases in it.
        try {
          if (!EmulatorSpannerHelper.isUsingEmulator()) {
            // Backups must be explicitly deleted before the instance may be deleted.
            logger.log(
                Level.FINE, "Deleting backups on test instance {0}", testHelper.getInstanceId());
            for (Backup backup :
                testHelper
                    .getClient()
                    .getDatabaseAdminClient()
                    .listBackups(testHelper.getInstanceId().getInstance())
                    .iterateAll()) {
              logger.log(Level.FINE, "Deleting backup {0}", backup.getId());
              backup.delete();
            }
          }
          logger.log(Level.FINE, "Deleting test instance {0}", testHelper.getInstanceId());
          instanceAdminClient.deleteInstance(testHelper.getInstanceId().getInstance());
          logger.log(Level.INFO, "Deleted test instance {0}", testHelper.getInstanceId());
        } catch (SpannerException e) {
          logger.log(
              Level.SEVERE, "Failed to delete test instance " + testHelper.getInstanceId(), e);
        }
      } else {
        testHelper.cleanUp();
      }
    } finally {
      testHelper.getClient().close();
    }
  }

  void checkInitialized() {
    checkState(testHelper != null, "Setup has not completed successfully");
  }
}
