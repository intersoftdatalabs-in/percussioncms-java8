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

package com.percussion.soln.rx.assembly;

import com.percussion.security.xml.PSXStreamSecurity;
import com.percussion.services.assembly.IPSAssemblyItem;
import com.percussion.services.assembly.IPSAssemblyResult;
import com.thoughtworks.xstream.XStream;
import java.nio.charset.StandardCharsets;

public class XStreamAssemblyResult extends MutableAssemblyResult implements IPSAssemblyResult {

  private static XStream xstream;
  private Object data;

  public XStreamAssemblyResult(IPSAssemblyItem assemblyItem, Object data) {
    super(assemblyItem, null, "application/xml");
    if (data == null)
      throw new IllegalArgumentException("Data object to be serialized cannot be null");
    this.data = data;
  }

  private XStream getXStream() {
    if (xstream == null) {
      xstream = new XStream();
      // T2.x.4 hardening (issue #104): apply the shared security baseline.
      // T2.x.5 hardening (issue #107): the wildcard com.percussion.** is
      // gone; the per-class allowlist is added when the first data object
      // is serialized (lazy, in toXMLData below). This site is currently
      // write-only (toXML), but defense in depth: if a future contributor
      // adds a fromXML call, the instance is already hardened.
      PSXStreamSecurity.setupDefaultSecurity(xstream);
    }

    return xstream;
  }

  /**
   * Lazy per-class allowlist (T2.x.5). Called the first time the result is serialized, after the
   * data class is known. The wildcard pattern is gone; only the concrete {@code data} class and its
   * declared fields are deserializable, not the entire {@code com.percussion.**} namespace.
   */
  private void ensureXStreamAllowed(Object data) {
    if (data == null) return;
    XStream xs = getXStream();
    PSXStreamSecurity.allowProjectTypes(xs, data.getClass());
  }

  public byte[] getResultData() {
    if (super.getResultData() == null) {
      ensureXStreamAllowed(getData());
      super.setResultData(getXStream().toXML(getData()).getBytes(StandardCharsets.UTF_8));
    }
    return super.getResultData();
  }

  public Object getData() {
    return data;
  }

  public void setData(Object data) {
    this.data = data;
  }
}
