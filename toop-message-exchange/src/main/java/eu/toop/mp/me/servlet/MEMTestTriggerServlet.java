/**
 * Copyright (C) 2018 toop.eu
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.toop.mp.me.servlet;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.annotation.Nonnull;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.mime.CMimeType;
import com.helger.commons.mime.IMimeType;
import com.helger.peppol.identifier.generic.participant.IParticipantIdentifier;

import eu.toop.commons.doctype.EToopDocumentType;
import eu.toop.commons.doctype.EToopProcess;
import eu.toop.mp.api.MPConfig;
import eu.toop.mp.api.MPSettings;
import eu.toop.mp.me.GatewayRoutingMetadata;
import eu.toop.mp.me.IMessageHandler;
import eu.toop.mp.me.MEMDelegate;
import eu.toop.mp.me.MEMessage;
import eu.toop.mp.me.MEPayload;
import eu.toop.mp.r2d2client.IR2D2Endpoint;
import eu.toop.mp.r2d2client.R2D2Endpoint;

/**
 * @author: myildiz
 * @date: 15.02.2018.
 */
@WebServlet("/memTest")
@Deprecated
public class MEMTestTriggerServlet extends HttpServlet {
  private static final Logger LOG = LoggerFactory.getLogger(MEMTestTriggerServlet.class);

  @Override
  protected void doGet(final HttpServletRequest req,
                       final HttpServletResponse resp) throws ServletException, IOException {
    resp.setStatus(HttpServletResponse.SC_OK);
    resp.getOutputStream().println("Starting the test. Will send an as4 message to the " + MPConfig.getMEMAS4ToPartyID());


    final GatewayRoutingMetadata metadata;
    try {
      metadata = new GatewayRoutingMetadata("iso6523-actorid-upis::0088:123456", EToopDocumentType.DOCTYPE1.getURIEncoded(),
          EToopProcess.PROC.getURIEncoded(), createSampleEndpoint());

      final String payloadId = "xmlpayload@dp";
      final IMimeType contentType = CMimeType.APPLICATION_XML;
      final byte[] payloadData = "<sample>xml</sample>".getBytes(StandardCharsets.ISO_8859_1);

      final MEPayload payload = new MEPayload(contentType, payloadId, payloadData);
      final MEMessage meMessage = new MEMessage(payload);

      MEMDelegate.getInstance().sendMessage(metadata, meMessage);

      final IMessageHandler handler = meMessage1 -> LOG.info("hooray! I Got a message");

      MEMDelegate.getInstance().registerMessageHandler(handler);
    } catch (final Exception e) {
      e.printStackTrace();
    }
  }


  @Nonnull
  private IR2D2Endpoint createSampleEndpoint() throws Exception {
    final IParticipantIdentifier identifier = MPSettings.getIdentifierFactory().createParticipantIdentifier("var1",
        "var2");

    final X509Certificate x509 = (X509Certificate) CertificateFactory.getInstance("X509")
        .generateCertificate(this.getClass()
            .getResourceAsStream("/testcert.der"));
    final R2D2Endpoint endpoint = new R2D2Endpoint(identifier, "protocol", "http://sampleendpointurl", x509);
    return endpoint;
  }
}
