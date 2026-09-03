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

import java.util.Arrays;
import java.util.List;

/**
 * Shim class matching the public API of sanselan 0.97-incubator's {@code
 * org.apache.sanselan.ImageFormat}.
 *
 * <p>Part of the sanselan classpath shim for Apache Shindig 1.1-BETA5-incubating. Apache Shindig's
 * {@code BaseOptimizer} static initializer populates a {@code Map<String, ImageFormat>} with
 * PNG/GIF/JPEG entries referencing these constants. See {@link Sanselan} for context.
 */
public class ImageFormat {
  public final String name;
  public final String extension;
  public final boolean actual;

  public static final ImageFormat IMAGE_FORMAT_UNKNOWN = new ImageFormat("UNKNOWN", false);
  public static final ImageFormat IMAGE_FORMAT_PNG = new ImageFormat("PNG", true);
  public static final ImageFormat IMAGE_FORMAT_GIF = new ImageFormat("GIF", true);
  public static final ImageFormat IMAGE_FORMAT_ICO = new ImageFormat("ICO", true);
  public static final ImageFormat IMAGE_FORMAT_TIFF = new ImageFormat("TIFF", true);
  public static final ImageFormat IMAGE_FORMAT_JPEG = new ImageFormat("JPEG", true);
  public static final ImageFormat IMAGE_FORMAT_BMP = new ImageFormat("BMP", true);
  public static final ImageFormat IMAGE_FORMAT_PSD = new ImageFormat("PSD", true);
  public static final ImageFormat IMAGE_FORMAT_PBM = new ImageFormat("PBM", true);
  public static final ImageFormat IMAGE_FORMAT_PGM = new ImageFormat("PGM", true);
  public static final ImageFormat IMAGE_FORMAT_PPM = new ImageFormat("PPM", true);
  public static final ImageFormat IMAGE_FORMAT_PNM = new ImageFormat("PNM", true);
  public static final ImageFormat IMAGE_FORMAT_TGA = new ImageFormat("TGA", true);
  public static final ImageFormat IMAGE_FORMAT_JBIG2 = new ImageFormat("JBIG2", true);

  private static final List<ImageFormat> ALL_FORMATS =
      Arrays.asList(
          IMAGE_FORMAT_UNKNOWN,
          IMAGE_FORMAT_PNG,
          IMAGE_FORMAT_GIF,
          IMAGE_FORMAT_ICO,
          IMAGE_FORMAT_TIFF,
          IMAGE_FORMAT_JPEG,
          IMAGE_FORMAT_BMP,
          IMAGE_FORMAT_PSD,
          IMAGE_FORMAT_PBM,
          IMAGE_FORMAT_PGM,
          IMAGE_FORMAT_PPM,
          IMAGE_FORMAT_PNM,
          IMAGE_FORMAT_TGA,
          IMAGE_FORMAT_JBIG2);

  private ImageFormat(String name) {
    this(name, true);
  }

  private ImageFormat(String name, boolean actual) {
    this.name = name;
    this.extension = name.toLowerCase();
    this.actual = actual;
  }

  @Override
  public String toString() {
    return name;
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ImageFormat)) {
      return false;
    }
    return name.equals(((ImageFormat) other).name);
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }

  public static ImageFormat[] getAllFormats() {
    return ALL_FORMATS.toArray(new ImageFormat[0]);
  }
}
