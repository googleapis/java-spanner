/*
 * Copyright 2019 Google LLC
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

package com.google.cloud.spanner.connection;

import com.google.cloud.spanner.Dialect;
import com.google.cloud.spanner.connection.ConnectionImplAutocommitReadOnlyTest.ConnectionImplAutocommitReadOnlyNoActionsTest;
import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ClassInfo;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;

/**
 * This test executes a SQL script that has been generated from the log of all the subclasses of
 * {@link AbstractConnectionImplTest} and covers the same test cases. Its aim is to verify that the
 * connection reacts correctly in all possible states (i.e. DML statements should not be allowed
 * when the connection is in read-only mode, or when a read-only transaction has started etc.)
 *
 * <p>A new test script can be generated by running: <code>
 * mvn -Ddo_log_statements=true exec:java -Dexec.mainClass=com.google.cloud.spanner.connection.SqlTestScriptsGenerator -Dexec.classpathScope="test"
 * </code> It is only necessary to generate a new test script if the behavior of {@link
 * com.google.cloud.spanner.connection.Connection} has changed (for example calling COMMIT is
 * currently not allowed in AUTOCOMMIT mode, but this has changed to be a no-op). A new test script
 * must also be generated if additional test cases have been added to {@link
 * AbstractConnectionImplTest}.
 */
public class ConnectionImplGeneratedSqlScriptTest extends AbstractSqlScriptTest {
  private String getFileName() {
    switch (dialect) {
      case POSTGRESQL:
        return "postgresql/ConnectionImplGeneratedSqlScriptTest.sql";
      case GOOGLE_STANDARD_SQL:
      default:
        return "ConnectionImplGeneratedSqlScriptTest.sql";
    }
  }

  @Test
  public void testGeneratedScript() throws Exception {
    SqlScriptVerifier verifier = new SqlScriptVerifier(new TestConnectionProvider(dialect));
    verifier.verifyStatementsInFile(getFileName(), getClass(), true);
  }

  /**
   * Generates the test SQL script. It should be noted that running this method multiple times
   * without having changed anything in the underlying code, could still yield different script
   * files, as the script is generated by running a number of JUnit test cases. The order in which
   * these test cases are run is non-deterministic. That means that the generated sql script will
   * still contain exactly the same test cases after each generation, but the order of the test
   * cases in the script file is equal to the order in which the test cases were run the last time
   * the script was generated. It is therefore also not recommended including this generation in an
   * automatic build, but to generate the script only when there has been some fundamental change in
   * the code.
   *
   * <p>The sql test scripts can be generated by running <code>
   * mvn -Ddo_log_statements=true exec:java -Dexec.mainClass=com.google.cloud.spanner.connection.SqlTestScriptsGenerator -Dexec.classpathScope="test"
   * </code>
   */
  static void generateTestScript() throws ClassNotFoundException, IOException {
    // first make the current script file empty
    AbstractConnectionImplTest test = new ConnectionImplAutocommitReadOnlyNoActionsTest();
    for (Dialect dialect : Dialect.values()) {
      test.emptyScript(dialect);
    }
    JUnitCore junit = new JUnitCore();
    Class<?>[] testClasses = getAbstractConnectionImplTestSubclasses();
    Result result = junit.run(testClasses);
    if (!result.wasSuccessful()) {
      throw new RuntimeException("Generating test script failed!");
    }
  }

  private static Class<?>[] getAbstractConnectionImplTestSubclasses()
      throws IOException, ClassNotFoundException {
    List<Class<?>> list = new ArrayList<>();
    ClassPath cp = ClassPath.from(ConnectionImplGeneratedSqlScriptTest.class.getClassLoader());
    ImmutableSet<ClassInfo> classes =
        cp.getTopLevelClassesRecursive(
            ConnectionImplGeneratedSqlScriptTest.class.getPackage().getName());
    for (ClassInfo c : classes) {
      Class<?> clazz =
          ConnectionImplGeneratedSqlScriptTest.class.getClassLoader().loadClass(c.getName());
      addAbstractConnectionImplTestSubclassesToList(list, clazz);
    }
    Class<?>[] res = new Class<?>[list.size()];
    for (int i = 0; i < list.size(); i++) {
      res[i] = list.get(i);
    }
    return res;
  }

  private static void addAbstractConnectionImplTestSubclassesToList(
      List<Class<?>> list, Class<?> clazz) {
    for (Class<?> innerClass : clazz.getDeclaredClasses()) {
      addAbstractConnectionImplTestSubclassesToList(list, innerClass);
    }
    if (!clazz.isInterface()
        && !Modifier.isAbstract(clazz.getModifiers())
        && AbstractConnectionImplTest.class.isAssignableFrom(clazz)) {
      list.add(clazz);
    }
  }
}
