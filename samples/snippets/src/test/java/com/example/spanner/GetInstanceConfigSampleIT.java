package com.example.spanner;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class GetInstanceConfigSampleIT extends SampleTestBase {

  @Test
  public void testGetInstanceConfig() throws Exception {
    final String out = SampleRunner.runSample(() ->
        GetInstanceConfigSample.getInstanceConfig(projectId, instanceConfigName)
    );

    assertTrue(
        "Expected instance config " + instanceConfigName + " to contain at least one leader option."
            + " Output received was " + out,
        out.matches("(?s:.*\\[.+\\].*)")
    );
  }
}
