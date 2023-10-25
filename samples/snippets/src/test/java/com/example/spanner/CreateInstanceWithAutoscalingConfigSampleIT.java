/*
 * Copyright 2022 Google LLC
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

package com.example.spanner;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CreateInstanceWithAutoscalingConfigSampleIT extends SampleTestBase {
    @Test
    public void testCreateInstanceWithAutoscalingConfig() throws Exception {
        String instanceId = formatForTest("sample-inst");
        String out =
                runSampleRunnable(() -> {
                    try {
                        CreateInstanceWithAutoscalingConfigExample.createInstance(
                                dbId.getInstanceId().getProject(), instanceId);
                    } finally {
                        spanner.getInstanceAdminClient().deleteInstance(instanceId);
                    }
                });
        assertThat(out)
                .contains(
                        String.format(
                                "Created autoscaler instance %s",
                                InstanceId.of(dbId.getInstanceId().getProject(), instanceId)));
    }
}
