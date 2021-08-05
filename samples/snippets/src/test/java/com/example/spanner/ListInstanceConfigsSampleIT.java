package com.example.spanner;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ListInstanceConfigsSampleIT extends SampleTestBase {

  @Test
  public void testListInstanceConfigs() throws Exception {
    final String out = SampleRunner.runSample(() ->
        ListInstanceConfigsSample.listInstanceConfigs(projectId)
    );

    assertTrue(
        "Expected instance config " + instanceConfigName + " to contain at least one leader option."
            + " Output received was " + out,
        out.matches("(?s:.*nam6: \\[.+\\].*)")
    );
  }
}
