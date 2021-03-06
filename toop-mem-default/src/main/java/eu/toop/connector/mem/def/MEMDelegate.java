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
package eu.toop.connector.mem.def;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.UsedViaReflection;
import com.helger.commons.error.level.EErrorLevel;
import com.helger.commons.url.URLHelper;
import com.helger.scope.singleton.AbstractGlobalSingleton;

import eu.toop.commons.error.EToopErrorCode;
import eu.toop.connector.api.TCConfig;
import eu.toop.connector.api.as4.MEException;
import eu.toop.connector.api.as4.MEMessage;
import eu.toop.connector.mem.def.notifications.IMessageHandler;
import eu.toop.connector.mem.def.notifications.IRelayResultHandler;
import eu.toop.connector.mem.def.notifications.ISubmissionResultHandler;
import eu.toop.connector.mem.def.notifications.InternalRelayResultHandler;
import eu.toop.connector.mem.def.notifications.InternalSubmissionResultHandler;
import eu.toop.connector.mem.def.notifications.RelayResult;
import eu.toop.connector.mem.def.notifications.SubmissionResult;
import eu.toop.kafkaclient.ToopKafkaClient;

/**
 * The API Entry class for the Message Exchange API.
 *
 * @author myildiz at 15.02.2018.
 */
@NotThreadSafe
public class MEMDelegate extends AbstractGlobalSingleton {

  private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(MEMDelegate.class);
  private final List<IMessageHandler> messageHandlers = new ArrayList<>();
  private final List<IRelayResultHandler> relayResultHandlers = new ArrayList<>();
  private final List<ISubmissionResultHandler> submissionResultHandlers = new ArrayList<>();

  private final InternalRelayResultHandler internalRelayResultHandler;
  private final InternalSubmissionResultHandler internalSRHandler;


  @UsedViaReflection
  public MEMDelegate() {
    //register this both as a submission result listener
    //and a notification listener in order to watch the process of
    //a send message call

    internalRelayResultHandler = new InternalRelayResultHandler();
    internalSRHandler = new InternalSubmissionResultHandler();

    relayResultHandlers.add(internalRelayResultHandler);
    submissionResultHandlers.add(internalSRHandler);
  }

  @Nonnull
  public static MEMDelegate getInstance() {
    return getGlobalSingleton(MEMDelegate.class);
  }

  /**
   * The V1 message sending interface for the message exchange module
   *
   * @param gatewayRoutingMetadata The container for the endpoint information and docid/procid
   * @param meMessage              the payloads and their metadata to be sent to the gateway.
   */
  public void sendMessage(final GatewayRoutingMetadata gatewayRoutingMetadata, final MEMessage meMessage) {
    if (LOG.isDebugEnabled()) {
      LOG.debug(
          "Send message called for procid: " + gatewayRoutingMetadata.getProcessId() + " docid: " + gatewayRoutingMetadata
              .getDocumentTypeId());
      LOG.debug("Convert gateway routing metadata to submission data");
    }
    final SubmissionMessageProperties submissionData = EBMSUtils.inferSubmissionData(gatewayRoutingMetadata);
    if (LOG.isDebugEnabled())
      LOG.debug("Create SOAP Message based on the submission data and the payloads");
    final SOAPMessage soapMessage = EBMSUtils.convert2MEOutboundAS4Message(submissionData, meMessage);
    if (LOG.isTraceEnabled()) {
      LOG.trace(SoapUtil.describe(soapMessage));
    }

    String messageID;
    try {
      messageID = SoapXPathUtil.getSingleNodeTextContent(soapMessage.getSOAPHeader(), "//:MessageInfo/:MessageId");
    } catch (final SOAPException e) {
      throw new MEException(e);
    }

    if (LOG.isDebugEnabled()) {
      LOG.debug("New soap message ID " + messageID);
      LOG.debug("Send soap message " + messageID);
    }

    if (LOG.isDebugEnabled()) {
      LOG.debug("\n" + SoapUtil.describe(soapMessage));
    }

    EBMSUtils.sendSOAPMessage(soapMessage, URLHelper.getAsURL(TCConfig.getMEMAS4Endpoint()));
    if (LOG.isDebugEnabled())
      LOG.debug("SOAP Message " + messageID + " sent");

    final long timeout = TCConfig.getGatewayNotificationWaitTimeout();
    //now that we have sent the object, first wait for the submission result
    if (LOG.isDebugEnabled())
      LOG.debug("Wait for SubmissionResult for " + messageID);
    final SubmissionResult submissionResult = (SubmissionResult) internalSRHandler.obtainNotification(messageID, timeout);

    LOG.info("SubmissionResult " + submissionResult.getResult());
    if (submissionResult.getResult() != ResultType.RECEIPT) {
      if (LOG.isErrorEnabled()) {
        LOG.error("SubmitMessageId: " + submissionResult.getErrorCode());
        LOG.error("C2-C3 MessageId: " + submissionResult.getRefToMessageID());
        LOG.error("ErrorCode: " + submissionResult.getErrorCode());
        LOG.error("Description: " + submissionResult.getDescription());
      }

      String errorMesage = "Error from AS4 transmission: EToopErrorCode.ME_002 -- " +
          "EBMS ERROR CODE: [" + submissionResult.getErrorCode() + "]\n";

      ToopKafkaClient.send(EErrorLevel.ERROR, () -> errorMesage);
      throw new MEException(EToopErrorCode.ME_002, errorMesage);

    }

    if (LOG.isDebugEnabled())
      LOG.debug("Wait for RelayResult for " + messageID);
    final RelayResult relayResult = (RelayResult) internalRelayResultHandler
        .obtainNotification(submissionResult.getMessageID(), timeout);

    if (LOG.isInfoEnabled())
      LOG.info("RelayResult " + relayResult.getResult());

    if (relayResult.getResult() != ResultType.RECEIPT) {
      if (LOG.isErrorEnabled()) {
        LOG.error("SubmitMessageId: " + relayResult.getErrorCode());
        LOG.error("C2-C3 MessageId: " + relayResult.getRefToMessageID());
        LOG.error("ErrorCode: " + relayResult.getErrorCode());
        LOG.error("Severity: " + relayResult.getSeverity());
        LOG.error("ShortDescription: " + relayResult.getShortDescription());
        LOG.error("Description: " + relayResult.getDescription());
      }

      String errorMesage = "Error from AS4 transmission: " +
          "EBMS ERROR CODE: " + relayResult.getErrorCode() +
          "\nSeverity: " + relayResult.getSeverity() +
          "\nShort Description: " + relayResult.getShortDescription();

      ToopKafkaClient.send(EErrorLevel.ERROR, () -> errorMesage);
      if ("EBMS:0301".equals(relayResult.getErrorCode()))
        throw new MEException(EToopErrorCode.ME_003, errorMesage);
      throw new MEException(EToopErrorCode.ME_004, errorMesage);
    }
  }

  /**
   * Register a new message handler to handle the inbound messages from the AS4 gateway. <p> Duplicate checking skipped
   * for now. So if you register a handler twice, its handle method will be called twice.
   *
   * @param aMessageHandler message handler to be added
   */
  public void registerMessageHandler(@Nonnull final IMessageHandler aMessageHandler) {
    ValueEnforcer.notNull(aMessageHandler, "MessageHandler");
    messageHandlers.add(aMessageHandler);
  }

  /**
   * Remove a message handler from this delegate
   *
   * @param aMessageHandler Message handler to be removed
   */
  public void deregisterMessageHandler(@Nonnull final IMessageHandler aMessageHandler) {
    ValueEnforcer.notNull(aMessageHandler, "MessageHandler");
    messageHandlers.remove(aMessageHandler);
  }

  /**
   * Register a new notification handler to handle the notifications. <p> Duplicate checking skipped for now. So if you
   * register a handler twice, its handle method will be called twice.
   *
   * @param notificationHandler message handler to be added
   */
  public void registerNotificationHandler(@Nonnull final IRelayResultHandler notificationHandler) {
    ValueEnforcer.notNull(notificationHandler, "NotificationHandler");
    relayResultHandlers.add(notificationHandler);
  }

  /**
   * Remove a notification handler from this delegate
   *
   * @param notificationHandler Message handler to be removed
   */
  public void deregisterNotificationHandler(@Nonnull final IRelayResultHandler notificationHandler) {
    ValueEnforcer.notNull(notificationHandler, "NotificationHandler");
    relayResultHandlers.remove(notificationHandler);
  }

  /**
   * Register a new submission result handler to handle the SubmissionResult's. <p> Duplicate checking skipped for now.
   * So if you register a handler twice, its handle method will be called twice.
   *
   * @param submissionResultHandler submission result handler to be added
   */
  public void registerSubmissionResultHandler(@Nonnull final ISubmissionResultHandler submissionResultHandler) {
    ValueEnforcer.notNull(submissionResultHandler, "SubmissionResultHandler");
    submissionResultHandlers.add(submissionResultHandler);
  }

  /**
   * Remove a submissionResultHandler handler from this delegate
   *
   * @param submissionResultHandler submissionResultHandler to be removed
   */
  public void deregisterSubmissionResultHandler(@Nonnull final ISubmissionResultHandler submissionResultHandler) {
    ValueEnforcer.notNull(submissionResultHandler, "SubmissionResultHandler");
    submissionResultHandlers.remove(submissionResultHandler);
  }

  /**
   * Dispatch the received inbound message form the AS4 gateway to the handlers
   *
   * @param message message to be dispatched
   */
  public void dispatchInboundMessage(@Nonnull final SOAPMessage message) {
    if (LOG.isInfoEnabled())
      LOG.info("Received a Deliver message\n" + //
          "   Inbound  AS4  Message ID: " + EBMSUtils.getMessageId(message));
    try {
      // Do it only once
      final MEMessage aMEMessage = EBMSUtils.soap2MEMessage(message);
      for (final IMessageHandler messageHandler : messageHandlers) {
        messageHandler.handleMessage(aMEMessage);
      }
    } catch (final Exception e) {
      throw new MEException("Error handling message " + message, e);
    }
  }

  /**
   * Dispatch the received RelayResult to the registered listeners
   *
   * @param notification Relay result
   */
  public void dispatchRelayResult(final SOAPMessage notification) {
    try {
      // Do it only once
      final RelayResult relayResult = EBMSUtils.soap2RelayResult(notification);

      if (LOG.isInfoEnabled())
        LOG.info("RelayResult for \n" + //
            "   Outbound AS4  Message ID: " + relayResult.getRefToMessageID() + "\n" + //
            "   MessageID:                " + relayResult.getMessageID());

      for (final IRelayResultHandler notificationHandler : relayResultHandlers) {
        notificationHandler.handleNotification(relayResult);
      }
    } catch (final Exception e) {
      throw new MEException("Error handling message " + notification, e);
    }
  }

  /**
   * Dispatch the received SubmissioNResult to the registered listeners
   *
   * @param submissionResult Submission result SOAP message
   */
  public void dispatchSubmissionResult(final SOAPMessage submissionResult) {
    try {
      // Do it only once
      final SubmissionResult sSubmissionResult = EBMSUtils.soap2SubmissionResult(submissionResult);

      if (LOG.isInfoEnabled())
        LOG.info("SubmissionResult for \n" + //
            "   SubmitMessage Message ID: " + sSubmissionResult.getRefToMessageID() + "\n" +
            "   Outbound AS4  Message ID: " + sSubmissionResult.getMessageID());

      for (final ISubmissionResultHandler submissionResultHandler : submissionResultHandlers) {
        submissionResultHandler.handleSubmissionResult(sSubmissionResult);
      }
    } catch (final Exception e) {
      throw new MEException("Error handling message " + submissionResult, e);
    }
  }
}
