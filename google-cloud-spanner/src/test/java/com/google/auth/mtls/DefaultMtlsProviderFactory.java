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

package com.google.auth.mtls;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

// TODO: Remove once the actual implementation of this class in the auth library has a config
//       option that allows us to skip the most expensive code paths during tests.
public class DefaultMtlsProviderFactory {
  public static final AtomicBoolean SKIP_MTLS = new AtomicBoolean(false);

  public static MtlsProvider create() throws IOException {
    if (SKIP_MTLS.get()) {
      return null;
    }
    // Note: The caller should handle CertificateSourceUnavailableException gracefully, since
    // it is an expected error case. All other IOExceptions are unexpected and should be surfaced
    // up the call stack.
    MtlsProvider mtlsProvider = new X509Provider();
    if (mtlsProvider.isAvailable()) {
      return mtlsProvider;
    }
    mtlsProvider = new SecureConnectProvider();
    if (mtlsProvider.isAvailable()) {
      return mtlsProvider;
    }
    throw new CertificateSourceUnavailableException(
        "No Certificate Source is available on this device.");
  }
}
