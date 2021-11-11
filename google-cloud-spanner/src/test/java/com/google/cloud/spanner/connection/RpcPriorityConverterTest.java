/*
 * Copyright 2021 Google LLC
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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import com.google.cloud.spanner.Options.RpcPriority;
import com.google.cloud.spanner.connection.ClientSideStatementImpl.CompileException;
import com.google.cloud.spanner.connection.ClientSideStatementValueConverters.RpcPriorityConverter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class RpcPriorityConverterTest {

  @Test
  public void testConvert() throws CompileException {
    String allowedValues = "'(HIGH|MEDIUM|LOW)'";
    assertThat(allowedValues, is(notNullValue()));
    RpcPriorityConverter converter =
        new ClientSideStatementValueConverters.RpcPriorityConverter(allowedValues);
    assertThat(converter.convert("high"), is(equalTo(RpcPriority.HIGH)));
    assertThat(converter.convert("HIGH"), is(equalTo(RpcPriority.HIGH)));
    assertThat(converter.convert("High"), is(equalTo(RpcPriority.HIGH)));

    assertThat(converter.convert("medium"), is(equalTo(RpcPriority.MEDIUM)));
    assertThat(converter.convert("Low"), is(equalTo(RpcPriority.LOW)));
    assertThat(converter.convert("Medium"), is(equalTo(RpcPriority.MEDIUM)));

    assertThat(converter.convert(""), is(nullValue()));
    assertThat(converter.convert(" "), is(nullValue()));
    assertThat(converter.convert("random string"), is(nullValue()));
  }
}
