/*
 * Copyright 1999-2023 Percussion Software, Inc.
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

import java.lang.reflect.Field;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.Security;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.ssl.ClientTlsStrategyBuilder;
import org.apache.hc.client5.http.ssl.TlsSocketStrategy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Configures TLS context, protocols, and cipher suites for Delivery Tier HTTP clients. */
public class PSTlsSocketFactory {

  public static final Logger log = LogManager.getLogger(PSTlsSocketFactory.class);
  private static final String defaultProtocols = "TLSv1.1,TLSv1.2";
  private final String[] protocols;
  private final SSLContext context;
  private final String[] enabledCiphers;
  private static final String[] DEFAULT_CIPHERS = {
    "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
    "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
    "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
    "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
    "TLS_RSA_WITH_AES_128_GCM_SHA256",
    "TLS_RSA_WITH_AES_256_GCM_SHA384"
  };

  public PSTlsSocketFactory(String ciphers)
      throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
    String protString = System.getProperty("jdk.tls.client.protocols");
    if (protString == null) {
      protString = System.getProperty("https.protocols");
    }
    if (protString != null) {
      String[] protArr = protString.split(",");
      protocols = new String[protArr.length];
      for (int i = 0; i < protArr.length; i++) {
        protocols[i] = protArr[i].trim();
      }
    } else {
      protocols = defaultProtocols.split(",");
    }

    // Work around for Secure Random failing
    try {
      Field propsField = Security.class.getDeclaredField("props");
      propsField.setAccessible(true);
      Properties props = (Properties) propsField.get(null);
      props.remove("securerandom.source");
    } catch (Exception e) {
      log.warn("Unable to modify java.lang.Security properties!");
      log.warn("BouncyCastle DRBG may fail.");
    }

    Provider[] prov = Security.getProviders();
    log.debug("--- Configured Providers ---");
    for (Provider p : prov) {
      log.debug(p.getName());
    }
    context = SSLContext.getInstance("TLS");

    log.debug("Initialized TLSSocketFactory enabled protocols to : {}", Arrays.toString(protocols));
    log.debug("Using Security Provider: {}", context.getProvider());

    TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("PKIX");
    trustManagerFactory.init((KeyStore) null);
    TrustManager[] tm = trustManagerFactory.getTrustManagers();
    SecureRandom random;

    try {
      random = SecureRandom.getInstance("DEFAULT", "BC");
    } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
      random = new SecureRandom();
    }
    context.init(null, tm, random);

    if (StringUtils.isEmpty(ciphers)) {
      enabledCiphers = DEFAULT_CIPHERS.clone();
    } else {
      String[] raw = ciphers.split(",");
      enabledCiphers = new String[raw.length];
      for (int i = 0; i < raw.length; i++) {
        enabledCiphers[i] = raw[i].trim();
      }
    }
  }

  /**
   * Builds an Apache HttpClient 5 {@link TlsSocketStrategy} configured with this factory's {@link
   * SSLContext}, enabled TLS versions, and supported cipher suites.
   */
  public TlsSocketStrategy createTlsSocketStrategy() {
    String[] scrubbedCiphers =
        scrubCiphers(context.getSupportedSSLParameters().getCipherSuites(), enabledCiphers);
    if (log.isDebugEnabled()) {
      log.debug("Setting Enabled Ciphers to: {}", StringUtils.join(scrubbedCiphers, ","));
    }
    ClientTlsStrategyBuilder builder =
        ClientTlsStrategyBuilder.create().setSslContext(context).setTlsVersions(protocols);
    if (scrubbedCiphers.length > 0) {
      builder.setCiphers(scrubbedCiphers);
    }
    return builder.buildClassic();
  }

  public SSLContext getSslContext() {
    return context;
  }

  public String[] getProtocols() {
    return protocols.clone();
  }

  public String[] getEnabledCiphers() {
    return scrubCiphers(context.getSupportedSSLParameters().getCipherSuites(), enabledCiphers);
  }

  private static String[] scrubCiphers(String[] supportedCipherSuites, String[] enabledCiphers) {
    ArrayList<String> ret = new ArrayList<>();
    Collections.addAll(ret, enabledCiphers);
    List<String> copySupported = Arrays.asList(supportedCipherSuites);
    for (String s : enabledCiphers) {
      if (!copySupported.contains(s)) {
        ret.remove(s);
      }
    }
    return ret.toArray(new String[0]);
  }
}
