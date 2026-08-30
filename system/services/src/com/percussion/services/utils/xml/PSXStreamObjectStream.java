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
package com.percussion.services.utils.xml;

import com.percussion.security.xml.PSXStreamSecurity;
import com.thoughtworks.xstream.XStream;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Reader;
import java.io.Writer;

/**
 * 
 * An implementation of {@link PSObjectStream} that uses
 * the popular XML Serialization XStream.
 * @author adamgent
 *
 * @param <T>
 */
public class PSXStreamObjectStream<T> extends PSObjectStream<T>
{

   private XStream m_xstream = new XStream();

   /***
    * Initialize the xstream security framework.
    *
    * <p>T2.x.4 hardening (issue #104): delegates to the shared
    * {@link PSXStreamSecurity#setupDefaultSecurity(XStream)} helper so this
    * site uses the same post-1.4.7 baseline and the project's gadget-chain
    * deny list as the other 4 XStream init sites in the project. The
    * {@code com.percussion.**} wildcard is re-applied after the helper so
    * project classes remain deserializable.
    */
   private static void initSecurityFramework(XStream stream){
      PSXStreamSecurity.setupDefaultSecurity(stream);
      stream.allowTypesByWildcard(new String[] {
              "com.percussion.**"
      });
   }

   public PSXStreamObjectStream() throws IOException
   {
      super();
      initSecurityFramework(m_xstream);

   }
   
   @Override
   protected ObjectOutputStream createObjectOutputStream(Writer writer) throws IOException
   {
      return m_xstream.createObjectOutputStream(writer);
   }

   @Override
   protected ObjectInputStream createObjectInputStream(Reader reader) throws IOException
   {
      return m_xstream.createObjectInputStream(reader);
   }

}
