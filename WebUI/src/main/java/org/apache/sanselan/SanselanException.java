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
 * Shim base exception matching the public API of sanselan 0.97-incubator.
 *
 * <p><strong>T2.17 hardening (issue #186):</strong> this class is part of the documented sanselan
 * classpath shim that satisfies Apache Shindig 1.1-BETA5-incubating's {@code
 * BaseOptimizer$SanselanOutputter} class-resolution requirements. The shim is intentionally a no-op
 * stub; image-rewriting via sanselan is not supported in this project. See {@link Sanselan} for the
 * rationale.
 */
public class SanselanException extends Exception {
  private static final long serialVersionUID = 1L;

  public SanselanException(String message) {
    super(message);
  }

  public SanselanException(String message, Exception cause) {
    super(message, cause);
  }
}
