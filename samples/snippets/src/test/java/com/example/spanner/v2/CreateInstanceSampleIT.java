package com.example.spanner.v2;

import static org.junit.Assert.assertTrue;

import com.example.spanner.SampleRunner;
import com.google.spanner.admin.instance.v1.InstanceConfig;
import com.google.spanner.admin.instance.v1.InstanceName;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class CreateInstanceSampleIT extends SampleTestBaseV2 {

  @Test
  public void testCreateInstance() throws Exception {
    final String instanceId = idGenerator.generateInstanceId();

    // Runs sample
    final String out = SampleRunner.runSample(() ->
        CreateInstanceSample.createInstance(projectId, instanceId)
    );

    assertTrue(
        "Expected created instance " + instanceId + "."
            + " Output received was " + out, out.contains("was successfully created")
    );
  }
}
