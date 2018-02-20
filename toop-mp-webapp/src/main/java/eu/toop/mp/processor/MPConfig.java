/**
 * Copyright (C) 2018 toop.eu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.toop.mp.processor;

import java.io.File;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import com.helger.asic.SignatureHelper;
import com.helger.commons.io.file.FileHelper;

/**
 * Message Processor configuration
 *
 * @author Philip Helger
 *
 */
@Immutable
public final class MPConfig {
  private static final SignatureHelper SH = new SignatureHelper (FileHelper.getInputStream (new File ("src/main/resources/demo-keystore.jks")),
                                                                 "password", null, "password");

  private MPConfig () {
  }

  /**
   *
   * @return The {@link SignatureHelper} singleton.
   */
  @Nonnull
  public static SignatureHelper getSignatureHelper () {
    return SH;
  }
}
