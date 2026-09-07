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

package org.apache.sanselan;

/**
 * Shim constants interface matching the public API of sanselan 0.97-incubator.
 *
 * <p>Part of the sanselan classpath shim for Apache Shindig 1.1-BETA5-incubating. See {@link
 * Sanselan} for context.
 */
public interface SanselanConstants {
  String PARAM_KEY_VERBOSE = "VERBOSE";
  String PARAM_KEY_FILENAME = "FILENAME";
  String PARAM_KEY_FORMAT = "FORMAT";
  String PARAM_KEY_COMPRESSION = "COMPRESSION";
  String BUFFERED_IMAGE_FACTORY = "BUFFERED_IMAGE_FACTORY";
  String PARAM_KEY_READ_THUMBNAILS = "READ_THUMBNAILS";
  String PARAM_KEY_STRICT = "STRICT";
  String PARAM_KEY_XMP_XML = "XMP_XML";
}
