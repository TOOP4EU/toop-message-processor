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
package eu.toop.connector.servlet;

import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.annotation.Nonnull;
import javax.servlet.ServletContextEvent;
import javax.servlet.annotation.WebListener;

import com.helger.commons.debug.GlobalDebug;
import com.helger.commons.error.level.EErrorLevel;
import com.helger.commons.id.factory.GlobalIDFactory;
import com.helger.commons.id.factory.StringIDFromGlobalLongIDFactory;
import com.helger.commons.string.StringHelper;
import com.helger.web.servlets.scope.WebScopeListener;

import eu.toop.commons.dataexchange.TDETOOPDataRequestType;
import eu.toop.commons.dataexchange.TDETOOPDataResponseType;
import eu.toop.commons.exchange.ToopMessageBuilder;
import eu.toop.connector.api.TCConfig;
import eu.toop.connector.me.MEMDelegate;
import eu.toop.connector.me.MEPayload;
import eu.toop.connector.mp.MessageProcessorDCIncoming;
import eu.toop.connector.mp.MessageProcessorDPIncoming;
import eu.toop.kafkaclient.ToopKafkaClient;

/**
 * Global startup/shutdown listener for the whole web application. Extends from
 * {@link WebScopeListener} to ensure global scope is created and maintained.
 *
 * @author Philip Helger
 */
@WebListener
public class MPWebAppListener extends WebScopeListener {
  private String m_sLogPrefix;

  @Override
  public void contextInitialized (@Nonnull final ServletContextEvent aEvent) {
    super.contextInitialized (aEvent);

    GlobalIDFactory.setPersistentStringIDFactory (new StringIDFromGlobalLongIDFactory ("toop-mp-"));
    GlobalDebug.setDebugModeDirect (TCConfig.isGlobalDebug ());
    GlobalDebug.setProductionModeDirect (TCConfig.isGlobalProduction ());

    // Get my IP address for debugging
    try {
      m_sLogPrefix = "[" + InetAddress.getLocalHost ().getHostAddress () + "] ";
    } catch (final UnknownHostException ex) {
      m_sLogPrefix = "";
    }

    {
      // Init tracker client
      ToopKafkaClient.setEnabled (TCConfig.isToopTrackerEnabled ());
      final String sToopTrackerUrl = TCConfig.getToopTrackerUrl ();
      if (StringHelper.hasText (sToopTrackerUrl))
        ToopKafkaClient.defaultProperties ().put ("bootstrap.servers", sToopTrackerUrl);
    }

    ToopKafkaClient.send (EErrorLevel.INFO, () -> m_sLogPrefix + "TOOP Connector WebApp startup");

    // Register the handler need
    MEMDelegate.getInstance ().registerMessageHandler (aMEMessage -> {
      // Always use response, because it is the super set of request and response
      final MEPayload aPayload = aMEMessage.head ();
      if (aPayload != null) {
        // Extract from ASiC
        final Object aMsg = ToopMessageBuilder.parseRequestOrResponse (aPayload.getDataInputStream ());

        if (aMsg instanceof TDETOOPDataResponseType) {
          // This is the way from DP back to DC; we're in DC incoming mode
          ToopKafkaClient.send (EErrorLevel.INFO, () -> m_sLogPrefix + "TC got DC incoming request (4/4)");
          MessageProcessorDCIncoming.getInstance ().enqueue ((TDETOOPDataResponseType) aMsg);
        } else if (aMsg instanceof TDETOOPDataRequestType) {
          // This is the way from DC to DP; we're in DP incoming mode
          ToopKafkaClient.send (EErrorLevel.INFO, () -> m_sLogPrefix + "TC got DP incoming request (2/4)");
          MessageProcessorDPIncoming.getInstance ().enqueue ((TDETOOPDataRequestType) aMsg);
        } else
          ToopKafkaClient.send (EErrorLevel.ERROR, () -> m_sLogPrefix + "Unsuspported Message: " + aMsg);
      } else
        ToopKafkaClient.send (EErrorLevel.WARN, () -> m_sLogPrefix + "MEMessage contains no payload: " + aMEMessage);
    });

    ToopKafkaClient.send (EErrorLevel.INFO, m_sLogPrefix + "TOOP Connector started");
  }

  @Override
  public void contextDestroyed (@Nonnull final ServletContextEvent aEvent) {
    ToopKafkaClient.send (EErrorLevel.INFO, m_sLogPrefix + "TOOP Connector shutting down");

    // Shutdown tracker
    ToopKafkaClient.close ();

    super.contextDestroyed (aEvent);
  }
}
