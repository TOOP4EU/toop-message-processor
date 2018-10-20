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
package eu.toop.connector.mp;

import java.io.Serializable;

import javax.annotation.Nonnull;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;

import com.helger.asic.SignatureHelper;
import com.helger.commons.concurrent.collector.IConcurrentPerformer;
import com.helger.commons.error.level.EErrorLevel;
import com.helger.commons.io.stream.NonBlockingByteArrayOutputStream;
import com.helger.httpclient.HttpClientManager;

import eu.toop.commons.dataexchange.TDETOOPErrorMessageType;
import eu.toop.commons.dataexchange.TDETOOPResponseType;
import eu.toop.commons.exchange.ToopMessageBuilder;
import eu.toop.connector.api.TCConfig;
import eu.toop.connector.api.http.TCHttpClientFactory;
import eu.toop.kafkaclient.ToopKafkaClient;

/**
 * The nested performer class that does the hard work in step 4/4.
 *
 * @author Philip Helger
 */
final class MessageProcessorDCIncomingPerformer implements IConcurrentPerformer <Serializable>
{
  private void _handleResponse (@Nonnull final TDETOOPResponseType aResponse) throws Exception
  {
    final String sRequestID = aResponse.getDataRequestIdentifier ().getValue ();
    final String sLogPrefix = "[" + sRequestID + "] ";
    ToopKafkaClient.send (EErrorLevel.INFO, () -> sLogPrefix + "Received DC Incoming Request (4/4)");

    // Forward to the DC at /to-dc interface
    final TCHttpClientFactory aHCFactory = new TCHttpClientFactory ();

    try (final HttpClientManager aMgr = new HttpClientManager (aHCFactory))
    {
      final SignatureHelper aSH = new SignatureHelper (TCConfig.getKeystoreType (),
                                                       TCConfig.getKeystorePath (),
                                                       TCConfig.getKeystorePassword (),
                                                       TCConfig.getKeystoreKeyAlias (),
                                                       TCConfig.getKeystoreKeyPassword ());

      try (final NonBlockingByteArrayOutputStream aBAOS = new NonBlockingByteArrayOutputStream ())
      {
        ToopMessageBuilder.createResponseMessageAsic (aResponse, aBAOS, aSH);

        // Send to DC (see ToDCServlet in toop-interface)
        final String sDestinationUrl = TCConfig.getMPToopInterfaceDCUrl ();

        ToopKafkaClient.send (EErrorLevel.INFO, () -> "Posting signed ASiC response to " + sDestinationUrl);

        final HttpPost aPost = new HttpPost (sDestinationUrl);
        aPost.setEntity (new ByteArrayEntity (aBAOS.toByteArray ()));
        aMgr.execute (aPost);
      }
    }
  }

  private void _handleErrorMessage (@Nonnull final TDETOOPErrorMessageType aErrorMsg) throws Exception
  {
    final String sRequestID = aErrorMsg.getDataRequestIdentifier ().getValue ();
    final String sLogPrefix = "[" + sRequestID + "] ";
    ToopKafkaClient.send (EErrorLevel.INFO, () -> sLogPrefix + "Received DC Incoming Error Message (4/4)");

    // Forward to the DC at /to-dc interface
    final TCHttpClientFactory aHCFactory = new TCHttpClientFactory ();

    try (final HttpClientManager aMgr = new HttpClientManager (aHCFactory))
    {
      final SignatureHelper aSH = new SignatureHelper (TCConfig.getKeystoreType (),
                                                       TCConfig.getKeystorePath (),
                                                       TCConfig.getKeystorePassword (),
                                                       TCConfig.getKeystoreKeyAlias (),
                                                       TCConfig.getKeystoreKeyPassword ());

      try (final NonBlockingByteArrayOutputStream aBAOS = new NonBlockingByteArrayOutputStream ())
      {
        ToopMessageBuilder.createErrorMessageAsic (aErrorMsg, aBAOS, aSH);

        // Send to DC (see ToDCServlet in toop-interface)
        final String sDestinationUrl = TCConfig.getMPToopInterfaceDCUrl ();

        ToopKafkaClient.send (EErrorLevel.INFO, () -> "Posting signed ASiC response to " + sDestinationUrl);

        final HttpPost aPost = new HttpPost (sDestinationUrl);
        aPost.setEntity (new ByteArrayEntity (aBAOS.toByteArray ()));
        aMgr.execute (aPost);
      }
    }
  }

  public void runAsync (@Nonnull final Serializable aResponse) throws Exception
  {
    if (aResponse instanceof TDETOOPResponseType)
      _handleResponse ((TDETOOPResponseType) aResponse);
    else
      _handleErrorMessage ((TDETOOPErrorMessageType) aResponse);
  }
}