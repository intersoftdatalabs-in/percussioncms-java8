/*
 * Copyright 1999-2023 Percussion Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.percussion.rx.delivery.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.percussion.rx.delivery.impl.PSEc2InstanceMetadataClient.HttpUrlConnectionMetadataTransport;
import com.percussion.rx.delivery.impl.PSEc2InstanceMetadataClient.MetadataResponse;
import com.percussion.rx.delivery.impl.PSEc2InstanceMetadataClient.MetadataTransport;
import com.percussion.rx.delivery.impl.PSEc2InstanceMetadataClient.TokenResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Test;

/**
 * Unit tests for {@link PSEc2InstanceMetadataClient}. The tests stub the
 * {@link MetadataTransport} so they can drive the IMDSv2 success path, the
 * IMDSv2-not-supported IMDSv1 fallback path, and the non-EC2 (timeout / 404 /
 * connection refused) path without binding a local HTTP server.
 */
public class PSEc2InstanceMetadataClientTest {

  /** Recording transport that captures each call and returns canned responses. */
  private static final class ScriptedTransport implements MetadataTransport {
    final List<String> calls = new ArrayList<>();
    final int tokenStatus;
    final String tokenBody;
    final int metadataWithTokenStatus;
    final int metadataPlainStatus;
    final boolean tokenPutThrows;
    final boolean metadataPlainThrows;

    ScriptedTransport(
        int tokenStatus,
        String tokenBody,
        int metadataWithTokenStatus,
        int metadataPlainStatus,
        boolean tokenPutThrows,
        boolean metadataPlainThrows) {
      this.tokenStatus = tokenStatus;
      this.tokenBody = tokenBody;
      this.metadataWithTokenStatus = metadataWithTokenStatus;
      this.metadataPlainStatus = metadataPlainStatus;
      this.tokenPutThrows = tokenPutThrows;
      this.metadataPlainThrows = metadataPlainThrows;
    }

    @Override
    public TokenResponse putForToken(String endpoint, String ttlSeconds) throws IOException {
      calls.add("PUT " + endpoint + " ttl=" + ttlSeconds);
      if (tokenPutThrows) {
        throw new IOException("PUT not supported");
      }
      return new TokenResponse(tokenStatus, tokenBody);
    }

    @Override
    public MetadataResponse getWithToken(String endpoint, String token) throws IOException {
      calls.add("GET(token) " + endpoint + " token=" + token);
      return new MetadataResponse(metadataWithTokenStatus, "meta");
    }

    @Override
    public MetadataResponse getPlain(String endpoint) throws IOException {
      calls.add("GET " + endpoint);
      if (metadataPlainThrows) {
        throw new IOException("Connection refused");
      }
      return new MetadataResponse(metadataPlainStatus, "meta");
    }
  }

  @After
  public void tearDown() {
    PSEc2InstanceMetadataClient.resetCache();
    PSEc2InstanceMetadataClient.setTransportForTests(null);
  }

  /** Ensures the constant names match the wire-level IMDSv2 specifics. */
  @Test
  public void testConstantsMatchAwsImdsv2Spec() {
    assertEquals("http://169.254.169.254", PSEc2InstanceMetadataClient.DEFAULT_METADATA_ENDPOINT);
    assertEquals("21600", PSEc2InstanceMetadataClient.DEFAULT_TOKEN_TTL_SECONDS);
    assertEquals("/latest/api/token", PSEc2InstanceMetadataClient.TOKEN_PATH);
    assertEquals("/latest/meta-data/", PSEc2InstanceMetadataClient.METADATA_PATH);
    assertEquals("X-aws-ec2-metadata-token-ttl-seconds",
        PSEc2InstanceMetadataClient.HEADER_TOKEN_TTL);
    assertEquals("X-aws-ec2-metadata-token", PSEc2InstanceMetadataClient.HEADER_TOKEN);
  }

  /** IMDSv2 success path: PUT returns 200 with a token, GET-with-token returns 200. */
  @Test
  public void testImdsv2SuccessPath() {
    ScriptedTransport transport =
        new ScriptedTransport(200, "TOKEN-A", 200, 404, false, false);
    PSEc2InstanceMetadataClient.setTransportForTests(transport);
    PSEc2InstanceMetadataClient.resetCache();

    boolean detected = PSEc2InstanceMetadataClient.isEC2Instance();

    assertTrue("IMDSv2 success path should be detected as EC2", detected);
    assertEquals(2, transport.calls.size());
    assertTrue(transport.calls.get(0).startsWith("PUT"));
    assertTrue(transport.calls.get(1).startsWith("GET(token)"));
    assertTrue(transport.calls.get(1).contains("TOKEN-A"));

    // Subsequent calls should NOT re-probe (cache works).
    assertTrue(PSEc2InstanceMetadataClient.isEC2Instance());
    assertEquals("Result must be cached after first call", 2, transport.calls.size());
  }

  /**
   * IMDSv2 token PUT returns 405 (or other non-200) -> IMDSv1 fallback. IMDSv1
   * GET returns 200 -> detected as EC2.
   */
  @Test
  public void testImdsv1FallbackWhenTokenPutFails() {
    ScriptedTransport transport = new ScriptedTransport(405, "", 404, 200, false, false);
    PSEc2InstanceMetadataClient.setTransportForTests(transport);
    PSEc2InstanceMetadataClient.resetCache();

    boolean detected = PSEc2InstanceMetadataClient.isEC2Instance();

    assertTrue("IMDSv1 fallback to 200 should be detected as EC2", detected);
    assertEquals(2, transport.calls.size());
    assertTrue(transport.calls.get(0).startsWith("PUT"));
    assertTrue(transport.calls.get(1).startsWith("GET "));
    assertFalse("Plain GET should not include token header",
        transport.calls.get(1).contains("token="));
  }

  /**
   * IMDSv2 token PUT throws (e.g. IOException: Connection refused on non-EC2
   * host) -> IMDSv1 fallback also runs - if IMDSv1 also fails, the host is
   * non-EC2 and the result is false.
   */
  @Test
  public void testNonEc2HostConnectionRefused() {
    ScriptedTransport transport = new ScriptedTransport(500, "", 500, 0, true, true);
    PSEc2InstanceMetadataClient.setTransportForTests(transport);
    PSEc2InstanceMetadataClient.resetCache();

    boolean detected = PSEc2InstanceMetadataClient.isEC2Instance();

    assertFalse("Non-EC2 host should NOT be detected as EC2", detected);
    assertEquals(2, transport.calls.size());
    assertTrue(transport.calls.get(0).startsWith("PUT"));
    assertTrue(transport.calls.get(1).startsWith("GET "));
  }

  /**
   * IMDSv2 token PUT returns 200 but metadata GET returns 403 / 500 -> IMDSv1
   * fallback runs. If IMDSv1 also returns non-2xx, result is false.
   */
  @Test
  public void testImdsv2MetadataFailsImdsv1AlsoFails() {
    ScriptedTransport transport = new ScriptedTransport(200, "TOK", 500, 404, false, false);
    PSEc2InstanceMetadataClient.setTransportForTests(transport);
    PSEc2InstanceMetadataClient.resetCache();

    boolean detected = PSEc2InstanceMetadataClient.isEC2Instance();

    assertFalse("Both IMDSv2 GET and IMDSv1 GET must fail -> not EC2", detected);
    // 3 calls: PUT token, GET-with-token (500), GET plain (404). All three fire
    // before the helper returns false.
    assertEquals(3, transport.calls.size());
  }

  /** Caching: cached result is returned even after the transport changes. */
  @Test
  public void testCachedResultIsReturnedOnSubsequentCalls() {
    PSEc2InstanceMetadataClient.setTransportForTests(
        new ScriptedTransport(200, "TOK", 200, 404, false, false));
    PSEc2InstanceMetadataClient.resetCache();
    assertTrue(PSEc2InstanceMetadataClient.isEC2Instance());

    // Swap to a transport that would otherwise say "not EC2"; cached true wins.
    PSEc2InstanceMetadataClient.setTransportForTests(
        new ScriptedTransport(500, "", 500, 0, true, true));
    assertTrue("Cached true result must be returned",
        PSEc2InstanceMetadataClient.isEC2Instance());
  }

  /** resetCache() clears the cached value so the probe can run again. */
  @Test
  public void testResetCacheForcesReprobe() {
    ScriptedTransport first = new ScriptedTransport(200, "TOK", 200, 404, false, false);
    PSEc2InstanceMetadataClient.setTransportForTests(first);
    PSEc2InstanceMetadataClient.resetCache();
    assertTrue(PSEc2InstanceMetadataClient.isEC2Instance());
    assertEquals(2, first.calls.size());

    ScriptedTransport second = new ScriptedTransport(500, "", 500, 0, true, true);
    PSEc2InstanceMetadataClient.setTransportForTests(second);
    PSEc2InstanceMetadataClient.resetCache();
    assertFalse(PSEc2InstanceMetadataClient.isEC2Instance());
    assertEquals(2, second.calls.size());
  }

  /** Spot check that the production transport class is wired and instantiates. */
  @Test
  public void testDefaultTransportInstantiates() {
    HttpUrlConnectionMetadataTransport transport =
        new HttpUrlConnectionMetadataTransport(100, 100);
    // The transport class is package-private by design; we just verify it can
    // be instantiated with the requested connect/read timeouts.
    assertEquals(100, transport.connectTimeoutMs);
    assertEquals(100, transport.readTimeoutMs);
  }
}
