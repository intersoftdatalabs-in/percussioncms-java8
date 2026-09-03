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

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

/**
 * Shim class for the sanselan main entry point. Replaces the {@code org.apache.sanselan.Sanselan}
 * class that Apache Shindig 1.1-BETA5-incubating's {@code BaseOptimizer$SanselanOutputter} imports.
 *
 * <p><strong>T2.17 hardening (issue #186):</strong> the project's runtime classpath does not ship
 * the full sanselan 0.97-incubator library (only a 10-class subset of API surface classes are
 * checked in under {@code WebUI/war/WEB-INF/classes/org/apache/sanselan/}). The {@code Sanselan}
 * class itself is missing from that subset, so a {@code NoClassDefFoundError} would propagate from
 * {@code BaseOptimizer$SanselanOutputter.toBytes} whenever a gadget's image-rewriter tries to use
 * the sanselan output path.
 *
 * <p>This shim provides a {@code Sanselan} class with the {@code writeImage} static method that
 * shindig calls. Every method throws {@link UnsupportedOperationException} wrapped in the
 * documented exception type so the failure mode is explicit rather than a cryptic {@code
 * NoClassDefFoundError}. The dashboard itself loads fine (it never calls this method); only the
 * gadget image-rewriter path is affected, and that path has been non-functional since the sanselan
 * subset was introduced.
 */
public final class Sanselan {

  private Sanselan() {
    // Utility class - no instances.
  }

  /**
   * Mirrors the public {@code org.apache.sanselan.Sanselan.writeImage} signature used by Shindig's
   * {@code BaseOptimizer$SanselanOutputter.toBytes()}. Always throws; image rewriting via sanselan
   * is not supported in this project.
   */
  public static void writeImage(
      BufferedImage src, OutputStream os, ImageFormat format, Map<String, Object> params)
      throws ImageWriteException, IOException {
    throw new UnsupportedOperationException(
        "Sanselan image writing is disabled in the T2.17 shim (issue #186). "
            + "The shindig gadget image-rewriter path requires the full sanselan 0.97-incubator "
            + "library which is not in the runtime classpath.");
  }
}
