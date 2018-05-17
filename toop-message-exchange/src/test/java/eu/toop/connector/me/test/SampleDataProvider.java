package eu.toop.connector.me.test;

import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.annotation.Nonnull;

import com.helger.commons.mime.CMimeType;
import com.helger.commons.mime.IMimeType;
import com.helger.peppol.identifier.generic.participant.IParticipantIdentifier;

import eu.toop.commons.codelist.EPredefinedDocumentTypeIdentifier;
import eu.toop.commons.codelist.EPredefinedProcessIdentifier;
import eu.toop.connector.api.TCSettings;
import eu.toop.connector.me.EActingSide;
import eu.toop.connector.me.GatewayRoutingMetadata;
import eu.toop.connector.me.MEException;
import eu.toop.connector.me.MEMessage;
import eu.toop.connector.me.MEPayload;
import eu.toop.connector.r2d2client.IR2D2Endpoint;
import eu.toop.connector.r2d2client.R2D2Endpoint;

/**
 * @author yerlibilgin
 */
public class SampleDataProvider {

  @Nonnull
  public static IR2D2Endpoint createSampleEndpoint(final EActingSide actingSide, final String c3URL) {
    final IParticipantIdentifier identifier = TCSettings.getIdentifierFactory().createParticipantIdentifier("var1",
        "var2");

    final X509Certificate x509;
    try {
      //If I am DC, use dp certificate or vice versa
      final String certName = actingSide == EActingSide.DC ? "/freedonia.crt" : "/elonia.crt" ;
      x509 = (X509Certificate) CertificateFactory.getInstance("X509")
          .generateCertificate(SampleDataProvider.class
              .getResourceAsStream(certName));
    } catch (final CertificateException e) {
      throw new MEException(e.getMessage(), e);
    }

    final R2D2Endpoint endpoint = new R2D2Endpoint(identifier, "protocol", c3URL, x509);
    return endpoint;
  }

  public static GatewayRoutingMetadata createGatewayRoutingMetadata(final EActingSide actingSide,
      final String receivingGWURL) {
    final GatewayRoutingMetadata metadata = new GatewayRoutingMetadata("iso6523-actorid-upis::0088:123456",
        EPredefinedDocumentTypeIdentifier.REQUEST_REGISTEREDORGANIZATION.getURIEncoded(),
        EPredefinedProcessIdentifier.DATAREQUESTRESPONSE.getURIEncoded(), createSampleEndpoint(actingSide, receivingGWURL), actingSide);

    return metadata;
  }

  public static MEMessage createSampleMessage() {
    final String payloadId = "xmlpayload@dp";
    final IMimeType contentType = CMimeType.APPLICATION_XML;
    final byte[] payloadData = "<sample>xml</sample>".getBytes(StandardCharsets.ISO_8859_1);

    final MEPayload payload = new MEPayload(contentType, payloadId, payloadData);
    return new MEMessage(payload);
  }


}