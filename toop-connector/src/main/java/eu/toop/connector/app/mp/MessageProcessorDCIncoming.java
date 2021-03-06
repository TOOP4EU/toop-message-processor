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
package eu.toop.connector.app.mp;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.UsedViaReflection;
import com.helger.commons.concurrent.BasicThreadFactory;
import com.helger.commons.concurrent.ExecutorServiceHelper;
import com.helger.commons.concurrent.collector.ConcurrentCollectorSingle;
import com.helger.commons.error.level.EErrorLevel;
import com.helger.commons.state.ESuccess;
import com.helger.scope.IScope;
import com.helger.web.scope.singleton.AbstractGlobalWebSingleton;

import eu.toop.commons.exchange.ToopResponseWithAttachments140;
import eu.toop.kafkaclient.ToopKafkaClient;

/**
 * The global message processor that handles DP to DC (= DC incoming) requests
 * (step 4/4). This is only the queue and it spawns external threads for
 * processing the incoming data.
 *
 * @author Philip Helger
 */
public final class MessageProcessorDCIncoming extends AbstractGlobalWebSingleton
{
  private static final Logger LOGGER = LoggerFactory.getLogger (MessageProcessorDCIncoming.class);

  // Just to have custom named threads....
  private static final ThreadFactory s_aThreadFactory = new BasicThreadFactory.Builder ().setNamingPattern ("MP-DC-In-%d")
                                                                                         .setDaemon (true)
                                                                                         .build ();
  private final ConcurrentCollectorSingle <ToopResponseWithAttachments140> m_aCollector = new ConcurrentCollectorSingle <> ();
  private final ExecutorService m_aExecutorPool;

  @Deprecated
  @UsedViaReflection
  public MessageProcessorDCIncoming ()
  {
    m_aCollector.setPerformer (new MessageProcessorDCIncomingPerformer ());
    m_aExecutorPool = Executors.newSingleThreadExecutor (s_aThreadFactory);
    m_aExecutorPool.submit (m_aCollector::collect);
  }

  /**
   * The global accessor method.
   *
   * @return The one and only instance. Never <code>null</code>.
   */
  @Nonnull
  public static MessageProcessorDCIncoming getInstance ()
  {
    return getGlobalSingleton (MessageProcessorDCIncoming.class);
  }

  @Override
  protected void onDestroy (@Nonnull final IScope aScopeInDestruction) throws Exception
  {
    // Avoid another enqueue call
    m_aCollector.stopQueuingNewObjects ();

    // Shutdown executor service
    ExecutorServiceHelper.shutdownAndWaitUntilAllTasksAreFinished (m_aExecutorPool);
  }

  /**
   * Queue a new Toop Response message.
   *
   * @param aMsg
   *        The data to be queued. May not be <code>null</code>.
   * @return {@link ESuccess}. Never <code>null</code>.
   */
  @Nonnull
  public ESuccess enqueue (@Nonnull final ToopResponseWithAttachments140 aMsg)
  {
    ValueEnforcer.notNull (aMsg, "Msg");

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Enqueueing new object for step 4/4: " + aMsg);

    try
    {
      m_aCollector.queueObject (aMsg);
      return ESuccess.SUCCESS;
    }
    catch (final IllegalStateException ex)
    {
      // Queue is stopped!
      ToopKafkaClient.send (EErrorLevel.ERROR, () -> "Cannot enqueue " + aMsg, ex);
      return ESuccess.FAILURE;
    }
  }
}
