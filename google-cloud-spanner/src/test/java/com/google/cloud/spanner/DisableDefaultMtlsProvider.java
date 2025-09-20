/*
 * Copyright 2025 Google LLC
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

import static net.bytebuddy.agent.builder.AgentBuilder.RedefinitionStrategy.RETRANSFORMATION;
import static net.bytebuddy.implementation.bytecode.assign.Assigner.Typing.DYNAMIC;
import static net.bytebuddy.matcher.ElementMatchers.is;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.none;

import com.google.auth.mtls.DefaultMtlsProviderFactory;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;

/** Temporary fix for https://github.com/googleapis/sdk-platform-java/issues/3911. */
public class DisableDefaultMtlsProvider {
  static class DisableMtlsCheck {
    @Advice.OnMethodEnter(skipOn = Advice.OnDefaultValue.class)
    public static boolean before() {
      // Skip original method execution
      return false;
    }

    @Advice.OnMethodExit
    public static void after(
        @Advice.Return(readOnly = false, typing = DYNAMIC) Object returnValue) {
      returnValue = null;
    }
  }

  static void premain(String agentArgs, Instrumentation inst) {
    new AgentBuilder.Default()
        .disableClassFormatChanges()
        .with(RETRANSFORMATION)
        .ignore(none())
        .type(is(DefaultMtlsProviderFactory.class))
        .transform(
            (builder, typeDescription, classLoader, module, protectionDomain) ->
                builder.visit(Advice.to(DisableMtlsCheck.class).on(named("create"))))
        .installOn(inst);
  }

  private static boolean initialized = false;

  public static void disableDefaultMtlsProvider() throws UnmodifiableClassException {
    synchronized (DisableDefaultMtlsProvider.class) {
      if (initialized) {
        return;
      }
      Instrumentation instrumentation = ByteBuddyAgent.install();
      premain("", instrumentation);
      instrumentation.retransformClasses(DefaultMtlsProviderFactory.class);
      initialized = true;
    }
  }
}
