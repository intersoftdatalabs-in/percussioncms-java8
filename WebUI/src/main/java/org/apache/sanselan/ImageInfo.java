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
 * Shim class matching the public API of sanselan 0.97-incubator's {@code
 * org.apache.sanselan.ImageInfo}.
 *
 * <p>Part of the sanselan classpath shim for Apache Shindig 1.1-BETA5-incubating. Public constants
 * are preserved so that any shindig class that references them resolves cleanly. No metadata is
 * actually parsed by this shim; the constructor throws to make that explicit. See {@link Sanselan}
 * for context.
 */
public class ImageInfo {
  // Color type constants (preserved from the sanselan public API).
  public static final int COLOR_TYPE_BW = 0;
  public static final int COLOR_TYPE_GRAYSCALE = 1;
  public static final int COLOR_TYPE_RGB = 2;
  public static final int COLOR_TYPE_CMYK = 3;
  public static final int COLOR_TYPE_OTHER = 4;
  public static final int COLOR_TYPE_UNKNOWN = 5;

  // Compression algorithm constants (preserved from the sanselan public API).
  public static final String COMPRESSION_ALGORITHM_UNKNOWN = "UNKNOWN";
  public static final String COMPRESSION_ALGORITHM_NONE = "NONE";
  public static final String COMPRESSION_ALGORITHM_LZW = "LZW";
  public static final String COMPRESSION_ALGORITHM_PACKBITS = "PACKBITS";
  public static final String COMPRESSION_ALGORITHM_JPEG = "JPEG";
  public static final String COMPRESSION_ALGORITHM_RLE = "RLE";
  public static final String COMPRESSION_ALGORITHM_FLATE = "FLATE";
  public static final String COMPRESSION_ALGORITHM_CCITT = "CCITT";
  public static final String COMPRESSION_ALGORITHM_THUNDERSCAN = "THUNDERSCAN";
  public static final String COMPRESSION_ALGORITHM_NEWSCAN = "NEWSCAN";
  public static final String COMPRESSION_ALGORITHM_CCITT_T4 = "CCITT_T4";
  public static final String COMPRESSION_ALGORITHM_CCITT_T6 = "CCITT_T6";
  public static final String COMPRESSION_ALGORITHM_LZMA = "LZMA";
  public static final String COMPRESSION_ALGORITHM_ZLIB = "ZLIB";
  public static final String COMPRESSION_ALGORITHM_JBIG2 = "JBIG2";

  public ImageInfo(
      String formatDetails,
      int bitsPerPixel,
      java.util.ArrayList<String> comments,
      ImageFormat format,
      String formatName,
      int height,
      String mimeType,
      int numberOfImages,
      int physicalHeightDpi,
      float physicalHeightInch,
      int physicalWidthDpi,
      float physicalWidthInch,
      int width,
      boolean isProgressive,
      boolean isTransparent,
      boolean usesPalette,
      int colorType,
      String compressionAlgorithm) {
    throw new UnsupportedOperationException(
        "Sanselan image metadata parsing is disabled in the T2.17 shim (issue #186).");
  }
}
