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
package eu.toop.connector.app.servlet;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.annotation.Nonnull;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.error.level.EErrorLevel;
import com.helger.commons.mime.CMimeType;

import eu.toop.commons.dataexchange.v140.TDETOOPResponseType;
import eu.toop.commons.exchange.AsicReadEntry;
import eu.toop.commons.exchange.ToopMessageBuilder140;
import eu.toop.connector.api.TCConfig;
import eu.toop.connector.app.TCDumpHelper;
import eu.toop.connector.app.mp.MPTrigger;
import eu.toop.kafkaclient.ToopKafkaClient;

/**
 * This method is called by the <code>toop-interface</code> project in the
 * direction DP to DC.<br>
 * The input is an ASiC archive that contains a {@link TDETOOPResponseType}. If
 * extracted successfully it is put in the DP outgoing queue for further
 * processing.
 *
 * @author Philip Helger
 */
@WebServlet ("/from-dp")
public class FromDPServlet extends HttpServlet
{
  @Override
  protected void doPost (@Nonnull final HttpServletRequest aHttpServletRequest,
                         @Nonnull final HttpServletResponse aHttpServletResponse) throws ServletException, IOException
  {
    ToopKafkaClient.send (EErrorLevel.INFO, () -> "MP got /from-dp HTTP request (3/4)");

    final TCUnifiedResponse aUR = new TCUnifiedResponse (aHttpServletRequest);

    // Parse POST data
    // No IToopDataResponse contained here
    final ICommonsList <AsicReadEntry> aAttachments = new CommonsArrayList <> ();
    final TDETOOPResponseType aResponseMsg = ToopMessageBuilder140.parseResponseMessage (TCDumpHelper.getDumpInputStream (aHttpServletRequest.getInputStream (),
                                                                                                                          TCConfig.getDebugFromDPDumpPathIfEnabled (),
                                                                                                                          "from-dp.asic"),
                                                                                         aAttachments::add);

    if (aResponseMsg == null)
    {
      // The message content is invalid
      ToopKafkaClient.send (EErrorLevel.ERROR,
                            () -> "The /from-dp request does not contain an ASiC archive or the ASiC archive does not contain a TOOP Response Message!");
      aUR.setContentAndCharset ("The provided ASIC container could not be interpreted as a valid TOOP response.",
                                StandardCharsets.UTF_8);
      aUR.setMimeType (CMimeType.TEXT_PLAIN);
      aUR.setStatus (HttpServletResponse.SC_BAD_REQUEST);
    }
    else
    {
      // Enqueue to processor and we're good
      MPTrigger.fromDP_3_of_4 (aResponseMsg, aAttachments);

      // Done - no content
      aUR.setStatus (HttpServletResponse.SC_NO_CONTENT);
    }

    // Done
    aUR.applyToResponse (aHttpServletResponse);
  }
}
