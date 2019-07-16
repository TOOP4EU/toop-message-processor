/**
 * Copyright (C) 2018-2019 toop.eu
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

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.mime.CMimeType;
import com.helger.commons.mime.MimeType;
import com.helger.commons.string.StringHelper;
import com.helger.commons.url.ISimpleURL;
import com.helger.servlet.response.UnifiedResponse;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;
import com.helger.xml.microdom.IMicroDocument;
import com.helger.xml.microdom.serialize.MicroWriter;
import com.helger.xml.namespace.MapBasedNamespaceContext;
import com.helger.xml.serialize.write.XMLWriterSettings;
import com.helger.xservlet.handler.simple.IXServletSimpleHandler;

import eu.toop.connector.app.searchdp.ISearchDPCallback;
import eu.toop.connector.app.searchdp.SearchDPHandler;
import eu.toop.connector.app.searchdp.SearchDPInputParams;

/**
 * Main handler for the /search-dp servlet
 *
 * @author Philip Helger
 */
final class SearchDPXServletHandler implements IXServletSimpleHandler
{
  static final Logger LOGGER = LoggerFactory.getLogger (SearchDPXServletHandler.class);

  public void handleRequest (@Nonnull final IRequestWebScopeWithoutResponse aRequestScope,
                             @Nonnull final UnifiedResponse aUnifiedResponse) throws Exception
  {
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("/search-dp" + aRequestScope.getPathWithinServlet () + " executed");

    // Extract the parameters
    final SearchDPInputParams aInputParams = SearchDPHandler.extractInputParams (aRequestScope.getPathWithinServlet ());
    if (!aInputParams.hasCountryCode ())
    {
      // Mandatory fields are missing
      aUnifiedResponse.disableCaching ();
      aUnifiedResponse.setStatus (HttpServletResponse.SC_BAD_REQUEST);
    }
    else
    {
      // Search result callback
      final ISearchDPCallback aCallback = new ISearchDPCallback ()
      {
        public void onQueryDirectoryError (@Nonnull final ISimpleURL aQueryURL)
        {
          aUnifiedResponse.disableCaching ();
          aUnifiedResponse.setStatus (HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }

        public void onQueryDirectorySuccess (@Nonnull final IMicroDocument aDoc)
        {
          // Return "as is"
          final XMLWriterSettings aXWS = new XMLWriterSettings ();
          final MapBasedNamespaceContext aNSCtx = new MapBasedNamespaceContext ();
          if (StringHelper.hasText (aDoc.getDocumentElement ().getNamespaceURI ()))
          {
            aNSCtx.setDefaultNamespaceURI (aDoc.getDocumentElement ().getNamespaceURI ());
          }
          aXWS.setNamespaceContext (aNSCtx);
          final String sResponse = MicroWriter.getNodeAsString (aDoc, aXWS);

          if (LOGGER.isInfoEnabled ())
            LOGGER.info ("Returning " + sResponse.length () + " characters of successul XML response back");

          // Put XML on response
          aUnifiedResponse.disableCaching ();
          aUnifiedResponse.setMimeType (new MimeType (CMimeType.APPLICATION_XML).addParameter (CMimeType.PARAMETER_NAME_CHARSET,
                                                                                               aXWS.getCharset ()
                                                                                                   .name ()));
          aUnifiedResponse.setContentAndCharset (sResponse, aXWS.getCharset ());
        }
      };

      // Perform the search
      SearchDPHandler.performSearch (aInputParams, aCallback);
    }
  }
}
