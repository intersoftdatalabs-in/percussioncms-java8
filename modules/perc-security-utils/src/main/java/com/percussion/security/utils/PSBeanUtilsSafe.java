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
package com.percussion.security.utils;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.beanutils.ConvertUtilsBean;
import org.apache.commons.beanutils.PropertyUtilsBean;

/**
 * Safe wrapper around Apache Commons BeanUtils that prevents class-injection gadget chains via the
 * {@code class} and {@code classLoader} properties.
 *
 * <p>The Apache Commons BeanUtils 1.x static facade ({@code BeanUtils.copyProperties}, {@code
 * BeanUtils.populate}, {@code PropertyUtils.setProperty}) does not filter the {@code class}
 * property by default. If an untrusted source (a deserialized bean, a JNDI datasource, a JCR
 * property name) contains a {@code class} key with a gadget-class value, the target bean's class
 * can be swapped to that gadget class, leading to RCE on the next access.
 *
 * <p>BeanUtils 1.11.0 ships a security framework (the {@link BeanUtilsBean} class with a
 * configurable {@link PropertyUtilsBean}) that lets callers opt out of the {@code class} property.
 * This helper wraps that framework with sensible defaults for the project.
 *
 * <p>T2.x.6 hardening (issue #109): use {@link #copyProperties(Object, Object)}, {@link
 * #populate(Object, Map)}, and {@link #setProperty(Object, String, Object)} at every call site
 * where the source of the property values could be untrusted (deserialization, JNDI, JCR, user
 * input).
 *
 * @see <a href="https://commons.apache.org/proper/commons-beanutils/security.html">BeanUtils
 *     security framework</a>
 */
public final class PSBeanUtilsSafe {

  /**
   * Properties that are never allowed to be set via {@link #copyProperties}, {@link #populate}, or
   * {@link #setProperty}, regardless of the source. These are the historical class-injection
   * vectors.
   */
  private static final Set<String> FORBIDDEN_PROPERTIES =
      Collections.unmodifiableSet(new HashSet<>(Arrays.asList("class", "classLoader")));

  private static final BeanUtilsBean BEAN_UTILS_BEAN =
      new BeanUtilsBean(
          new ConvertUtilsBean(),
          new PropertyUtilsBean() {
            @Override
            public PropertyDescriptor[] getPropertyDescriptors(Class<?> clazz) {
              PropertyDescriptor[] pds = super.getPropertyDescriptors(clazz);
              if (pds == null) {
                return null;
              }
              // Filter out the forbidden properties so they can never be read or
              // written. This is the post-1.11.0 recommended hardening pattern.
              java.util.List<PropertyDescriptor> safe = new java.util.ArrayList<>(pds.length);
              for (PropertyDescriptor pd : pds) {
                String name = pd.getName();
                if (name != null && !FORBIDDEN_PROPERTIES.contains(name)) {
                  safe.add(pd);
                }
              }
              return safe.toArray(new PropertyDescriptor[0]);
            }

            @Override
            public PropertyDescriptor getPropertyDescriptor(Object bean, String name)
                throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
              if (FORBIDDEN_PROPERTIES.contains(name)) {
                return null;
              }
              return super.getPropertyDescriptor(bean, name);
            }

            @Override
            public boolean isReadable(Object bean, String name) {
              if (FORBIDDEN_PROPERTIES.contains(name)) {
                return false;
              }
              return super.isReadable(bean, name);
            }

            @Override
            public boolean isWriteable(Object bean, String name) {
              if (FORBIDDEN_PROPERTIES.contains(name)) {
                return false;
              }
              return super.isWriteable(bean, name);
            }
          });

  private PSBeanUtilsSafe() {
    // utility class
  }

  /**
   * Safe replacement for {@code BeanUtils.copyProperties(dest, orig)}. Copies properties from
   * {@code orig} to {@code dest}, but rejects any property named {@code class} or {@code
   * classLoader} (the historical class-injection vectors).
   */
  public static void copyProperties(Object dest, Object orig)
      throws IllegalAccessException, InvocationTargetException {
    if (dest == null) {
      throw new IllegalArgumentException("dest must not be null");
    }
    if (orig == null) {
      throw new IllegalArgumentException("orig must not be null");
    }
    BEAN_UTILS_BEAN.copyProperties(dest, orig);
  }

  /**
   * Safe replacement for {@code BeanUtils.populate(bean, properties)}. Copies entries from the
   * {@code properties} map to {@code bean}, but rejects any key named {@code class} or {@code
   * classLoader}.
   */
  public static void populate(Object bean, Map<String, ?> properties)
      throws IllegalAccessException, InvocationTargetException {
    if (bean == null) {
      throw new IllegalArgumentException("bean must not be null");
    }
    if (properties == null) {
      return;
    }
    // Defensive copy that drops forbidden keys before they reach BeanUtilsBean.
    // Even if the underlying PropertyUtilsBean.filter is bypassed, this copy
    // guarantees the forbidden keys never reach the BeanUtils API.
    Map<String, Object> sanitized = new java.util.LinkedHashMap<>(properties.size());
    for (Map.Entry<String, ?> e : properties.entrySet()) {
      String key = e.getKey();
      if (key != null && !FORBIDDEN_PROPERTIES.contains(key)) {
        sanitized.put(key, e.getValue());
      }
    }
    BEAN_UTILS_BEAN.populate(bean, sanitized);
  }

  /**
   * Safe replacement for {@code PropertyUtils.setProperty(bean, name, value)}. Throws an
   * IllegalArgumentException if {@code name} is a forbidden property (the historical
   * class-injection vector). For boolean properties, prefer {@code name} being the actual property
   * name (not the "is" prefix); the underlying BeanUtils framework handles both.
   */
  public static void setProperty(Object bean, String name, Object value)
      throws IllegalAccessException, InvocationTargetException {
    if (bean == null) {
      throw new IllegalArgumentException("bean must not be null");
    }
    if (name == null) {
      throw new IllegalArgumentException("name must not be null");
    }
    if (FORBIDDEN_PROPERTIES.contains(name)) {
      throw new IllegalArgumentException(
          "Property '" + name + "' is forbidden by PSBeanUtilsSafe (class-injection guard).");
    }
    BEAN_UTILS_BEAN.setProperty(bean, name, value);
  }
}
