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
package eu.toop.mp.r2d2client;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import com.helger.peppol.identifier.factory.IIdentifierFactory;
import com.helger.peppol.identifier.factory.SimpleIdentifierFactory;
import com.helger.peppol.sml.ESML;
import com.helger.peppol.url.EsensURLProvider;
import com.helger.peppol.url.IPeppolURLProvider;

/**
 * This class contains global settings for the R2D2 client.
 *
 * @author Philip Helger, BRZ, AT
 */
@Immutable
public final class R2D2Settings
{
  private R2D2Settings ()
  {}

  /**
   * Get the PEPPOL Directory URL to be used.
   *
   * @param bProduction
   *        <code>true</code> for production system, <code>false</code> for test
   *        system.
   * @return A new URL and never <code>null</code>. Never ends with a "/".
   */
  @Nonnull
  public static String getPEPPOLDirectoryURL (final boolean bProduction)
  {
    // TODO use correct URLs
    return "http://directory.central.toop";
  }

  /**
   * Get the SML to be used.
   *
   * @param bProduction
   *        <code>true</code> for SML, <code>false</code> for SMK.
   * @return Never <code>null</code>.
   */
  @Nonnull
  public static ESML getSML (final boolean bProduction)
  {
    return bProduction ? ESML.DIGIT_PRODUCTION : ESML.DIGIT_TEST;
  }

  @Nonnull
  public static IIdentifierFactory getIdentifierFactory ()
  {
    return SimpleIdentifierFactory.INSTANCE;
  }

  @Nonnull
  public static IPeppolURLProvider getSMPUrlProvider ()
  {
    return EsensURLProvider.INSTANCE;
  }
}
