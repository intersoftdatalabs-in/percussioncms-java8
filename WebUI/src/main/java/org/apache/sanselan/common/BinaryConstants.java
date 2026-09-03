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

package org.apache.sanselan.common;

/**
 * Shim byte-order constants interface matching the public API of sanselan 0.97-incubator.
 *
 * <p>Part of the sanselan classpath shim for Apache Shindig 1.1-BETA5-incubating. See {@link
 * org.apache.sanselan.Sanselan} for context.
 */
public interface BinaryConstants {
  int BYTE_ORDER_INTEL = 0;
  int BYTE_ORDER_LEAST_SIGNIFICANT_BYTE = 0;
  int BYTE_ORDER_LSB = 0;
  int BYTE_ORDER_LITTLE_ENDIAN = 0;
  int BYTE_ORDER_MOTOROLA = 1;
  int BYTE_ORDER_MOST_SIGNIFICANT_BYTE = 1;
  int BYTE_ORDER_MSB = 1;
  int BYTE_ORDER_NETWORK = 1;
  int BYTE_ORDER_BIG_ENDIAN = 1;
}
