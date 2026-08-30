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
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.percussion.security.xml;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.security.CGLIBProxyTypePermission;
import com.thoughtworks.xstream.security.NoTypePermission;
import com.thoughtworks.xstream.security.NullPermission;
import com.thoughtworks.xstream.security.PrimitiveTypePermission;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Consolidated XStream security framework initialization.
 *
 * <p>XStream 1.4.21 is the latest 1.x line. Without the security framework, every XStream
 * deserialization endpoint is exposed to gadget-chain RCE (CVE-2013-7285, CVE-2020-26217,
 * CVE-2021-29505, and ~30 more in the xstream 1.4.21 range, per the project's vulnerability
 * analysis).
 *
 * <p>Use {@link #setupDefaultSecurity(XStream)} at every {@code new XStream()} site to apply the
 * post-1.4.7 recommended baseline. The wildcard {@code com.percussion.**} allowlist remains under
 * the caller's control (added after this call) so that project classes can still be deserialized;
 * the deny lists here add defense in depth even inside that wildcard.
 *
 * <p>Pattern established in T2.x.4 hardening (issue #104).
 *
 * @see <a href="https://x-stream.github.io/security.html">XStream security framework</a>
 */
public final class PSXStreamSecurity {

  private PSXStreamSecurity() {
    // utility class
  }

  /**
   * Apply the recommended post-1.4.7 XStream security baseline plus the project's defense-in-depth
   * deny list.
   *
   * <p>This is a drop-in replacement for the inline pattern that several modules previously
   * copy-pasted. After this call returns, the XStream instance has:
   *
   * <ul>
   *   <li>{@code NoTypePermission.NONE} as the default (must allow each type explicitly). Set
   *       internally by {@link XStream#setupDefaultSecurity}.
   *   <li>Allow for {@code null}, primitive types, and CGLIB proxies.
   *   <li>Allow the type hierarchies for {@link Collection}, {@link Set}, {@link List}, and {@link
   *       String} (the classes typically required for serialized DTOs).
   *   <li>Deny known gadget-chain classes (JNDI enumeration, sun.reflect, sun.tracing,
   *       com.sun.corba, JAX-WS SEI, $ProxyLazyValue, $URLData, and similar). See {@link
   *       #denyGadgetChains(XStream)}.
   * </ul>
   *
   * <p>After this call, the caller is expected to add a specific allowlist (typically {@code
   * allowTypesByWildcard("com.percussion.**")} for project classes) before any deserialization.
   */
  public static void setupDefaultSecurity(XStream xs) {
    // The post-1.4.7 recommended baseline. This is the static helper that
    // sets NoTypePermission.NONE and the basic primitive / null / proxy
    // permissions, then clears any prior permissions.
    XStream.setupDefaultSecurity(xs);
    xs.addPermission(NoTypePermission.NONE);
    xs.addPermission(NullPermission.NULL);
    xs.addPermission(PrimitiveTypePermission.PRIMITIVES);
    xs.addPermission(CGLIBProxyTypePermission.PROXIES);
    xs.allowTypeHierarchy(Collection.class);
    xs.allowTypeHierarchy(Set.class);
    xs.allowTypeHierarchy(List.class);
    xs.allowTypeHierarchy(String.class);
    denyGadgetChains(xs);
  }

  /**
   * Apply the project's deny list for known gadget-chain patterns.
   *
   * <p>Patterns included are drawn from XStream's own published CVE list and from the analysis
   * report at {@code docs/ai-generated/tasks/PR#-DependencyVulnerabilityAnalysis/}. Even inside a
   * {@code com.percussion.**} wildcard allowlist, these patterns are explicitly denied so that an
   * attacker who plants a matching class in the project's namespace still cannot reach a
   * deserialization gadget.
   */
  public static void denyGadgetChains(XStream xs) {
    // Common reflection / tracing / CORBA / JNDI internals that have
    // appeared in XStream gadget-chain advisories.
    xs.denyTypesByWildcard(
        new String[] {
          "sun.reflect.**",
          "sun.tracing.**",
          "com.sun.corba.**",
          "com.sun.jndi.**",
          "com.sun.rowset.**"
        });

    // Specific gadget-chain class patterns. The regex form catches proxy
    // and inner-class variants of the same root class.
    xs.denyTypesByRegExp(
        new String[] {
          ".*\\.ws\\.client\\.sei\\..*",
          ".*\\$ProxyLazyValue",
          ".*\\$URLData",
          ".*\\$Aux0",
          ".*\\$Aux1",
          ".*\\$AuxiliaryInputStream",
          ".*\\.ldap\\..*",
          ".*EventHandler.*",
          ".*\\.bound\\..*"
        });
  }
}
