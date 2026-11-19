/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.percussion.delivery.utils.security;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.apache.hc.client5.http.ssl.TlsSocketStrategy;
import org.junit.jupiter.api.Test;

/**
 * Unit tests verifying {@link PSTlsSocketFactory} initialization and Apache HttpClient 5 {@link
 * TlsSocketStrategy} construction (issue #310).
 */
public class PSTlsSocketFactoryTest {

  @Test
  public void testCreateTlsSocketStrategyWithDefaultAndCustomCiphers() throws Exception {
    PSTlsSocketFactory defaultFactory = new PSTlsSocketFactory(null);
    TlsSocketStrategy defaultStrategy = defaultFactory.createTlsSocketStrategy();
    assertNotNull(defaultStrategy);

    PSTlsSocketFactory customFactory =
        new PSTlsSocketFactory(
            "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256, TLS_RSA_WITH_AES_128_GCM_SHA256");
    TlsSocketStrategy customStrategy = customFactory.createTlsSocketStrategy();
    assertNotNull(customStrategy);
  }
}
