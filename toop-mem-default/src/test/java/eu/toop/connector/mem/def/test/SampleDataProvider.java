/**
 * Copyright (C) 2018-2020 toop.eu
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
package eu.toop.connector.mem.def.test;

import com.helger.commons.io.ByteArrayWrapper;
import com.helger.commons.mime.CMimeType;
import com.helger.commons.mime.IMimeType;
import eu.toop.commons.codelist.EPredefinedDocumentTypeIdentifier;
import eu.toop.commons.codelist.EPredefinedProcessIdentifier;
import eu.toop.connector.api.as4.MEException;
import eu.toop.connector.api.as4.MEMessage;
import eu.toop.connector.api.as4.MEPayload;
import eu.toop.connector.mem.def.EActingSide;
import eu.toop.connector.mem.def.GatewayRoutingMetadata;

import javax.annotation.Nonnull;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

/**
 * @author yerlibilgin
 */
public class SampleDataProvider {

  private static KeyStore domibusKeystore;

  public static X509Certificate readDomibusCert(String alias) {
    try {
      if (domibusKeystore == null) {
        //multithread initialiation danger... yes no big deal.
        domibusKeystore = KeyStore.getInstance("JKS");
        domibusKeystore.load(SampleDataProvider.class.getResourceAsStream("/dev-gw-jks/domibus-toop-keys.jks"), "test123".toCharArray());
      }

      return (X509Certificate) domibusKeystore.getCertificate(alias);

    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  @Nonnull
  public static X509Certificate readCert(final EActingSide actingSide) {
    try {
      //If I am DC, use dp certificate or vice versa
      final String certName = actingSide == EActingSide.DC ? "/freedonia.crt" : "/elonia.crt";
      return (X509Certificate) CertificateFactory.getInstance("X509")
          .generateCertificate(SampleDataProvider.class
              .getResourceAsStream(certName));
    } catch (final CertificateException e) {
      throw new MEException(e.getMessage(), e);
    }
  }

  public static GatewayRoutingMetadata createGatewayRoutingMetadata(final EActingSide actingSide, final String receivingGWURL) {
    X509Certificate aCert = readCert(actingSide);
    return createGatewayRoutingMetadata(actingSide, receivingGWURL, aCert);
  }

  public static GatewayRoutingMetadata createGatewayRoutingMetadata(EActingSide actingSide, String targetURL, X509Certificate targetCert) {
    final GatewayRoutingMetadata metadata = new GatewayRoutingMetadata("iso6523-actorid-upis::0088:123456",
        EPredefinedDocumentTypeIdentifier.REQUEST_REGISTEREDORGANIZATION_LIST.getURIEncoded(),
        EPredefinedProcessIdentifier.DATAREQUESTRESPONSE.getURIEncoded(), targetURL, targetCert, actingSide);

    return metadata;
  }

  public static MEMessage createSampleMessage() {
    final String payloadId = "xmlpayload@dp";
    final IMimeType contentType = CMimeType.APPLICATION_XML;

    final MEPayload payload = new MEPayload(contentType, payloadId, ByteArrayWrapper.create("<sample>that is a sample xml</sample>", StandardCharsets.ISO_8859_1));
    return MEMessage.create(payload);
  }
}
