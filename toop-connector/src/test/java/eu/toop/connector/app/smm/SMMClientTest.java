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
package eu.toop.connector.app.smm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.collection.CollectionHelper;
import com.helger.commons.collection.impl.CommonsTreeSet;
import com.helger.commons.collection.impl.ICommonsOrderedSet;
import com.helger.commons.collection.impl.ICommonsSortedSet;
import com.helger.commons.string.StringHelper;
import com.helger.xml.microdom.IMicroDocument;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.MicroDocument;
import com.helger.xml.microdom.serialize.MicroWriter;

import eu.toop.commons.concept.ConceptValue;
import eu.toop.commons.usecase.EToopConcept;
import eu.toop.connector.api.smm.IMappedValueList;
import eu.toop.connector.api.smm.ISMMClient;
import eu.toop.connector.api.smm.ISMMConceptProvider;
import eu.toop.connector.api.smm.ISMMMultiMappingCallback;
import eu.toop.connector.api.smm.ISMMUnmappableCallback;
import eu.toop.connector.api.smm.MappedValue;
import eu.toop.connector.api.smm.MappedValueList;

/**
 * Test class for class {@link SMMClient}.
 *
 * @author Philip Helger
 */
public final class SMMClientTest
{
  private static final Logger LOGGER = LoggerFactory.getLogger (SMMClientTest.class);

  private static final ConceptValue CONCEPT_TOOP_1 = new ConceptValue (CMockSMM.NS_TOOP, "CompanyCode");
  private static final ConceptValue CONCEPT_FR_1 = new ConceptValue (CMockSMM.NS_FREEDONIA, "FreedoniaCompanyCode");

  // Use with cache and remote
  private static final ISMMConceptProvider [] CP = new ISMMConceptProvider [] { new SMMConceptProviderGRLCWithCache (),
                                                                                new SMMConceptProviderGRLCRemote () };
  private static final ISMMUnmappableCallback UCB = (sLogPrefix, aSourceNamespace, aSourceValue, aDestNamespace) -> {
    // Do nothing
  };
  private static final ISMMMultiMappingCallback MMCB = (sLogPrefix,
                                                        aSourceNamespace,
                                                        aSourceValue,
                                                        aDestNamespace,
                                                        aMappings) -> {
    // Do nothing
  };

  @Test
  public void testEmpty () throws IOException
  {
    for (final ISMMConceptProvider aCP : CP)
    {
      LOGGER.info ("Starting testEmpty");
      final ISMMClient aClient = new SMMClient ();
      final IMappedValueList ret = aClient.performMapping (CMockSMM.LOG_PREFIX, CMockSMM.NS_FREEDONIA, aCP, UCB, MMCB);
      assertNotNull (ret);
      assertTrue (ret.isEmpty ());
      assertEquals (0, ret.size ());
    }
  }

  @Test
  public void testOneMatch () throws IOException
  {
    for (final ISMMConceptProvider aCP : CP)
    {
      LOGGER.info ("Starting testOneMatch");
      final ISMMClient aClient = new SMMClient ();
      aClient.addConceptToBeMapped (CONCEPT_TOOP_1);
      final IMappedValueList ret = aClient.performMapping (CMockSMM.LOG_PREFIX, CMockSMM.NS_FREEDONIA, aCP, UCB, MMCB);
      assertNotNull (ret);
      assertEquals (1, ret.size ());

      // Check concept has a 1:1 mapping
      final IMappedValueList aMapped = ret.getAllBySource (x -> x.equals (CONCEPT_TOOP_1));
      assertNotNull (aMapped);
      assertEquals (1, aMapped.size ());
      final MappedValue aValue = aMapped.getFirst ();
      assertNotNull (aValue);
      assertEquals (CONCEPT_TOOP_1, aValue.getSource ());
      assertEquals (CONCEPT_FR_1, aValue.getDestination ());
    }
  }

  @Test
  public void testOneMatchOneNotFound () throws IOException
  {
    for (final ISMMConceptProvider aCP : CP)
    {
      LOGGER.info ("Starting testOneMatchOneNotFound");
      final ISMMClient aClient = new SMMClient ();
      aClient.addConceptToBeMapped (CONCEPT_TOOP_1);
      aClient.addConceptToBeMapped (CMockSMM.NS_TOOP, "NonExistingField");
      aClient.addConceptToBeMapped ("SourceNamespace", "NonExistingField");
      final IMappedValueList ret = aClient.performMapping (CMockSMM.LOG_PREFIX, CMockSMM.NS_FREEDONIA, aCP, UCB, MMCB);
      assertNotNull (ret);
      assertEquals (1, ret.size ());

      // Check concept has a 1:1 mapping
      final IMappedValueList aMapped = ret.getAllBySource (x -> x.equals (CONCEPT_TOOP_1));
      assertNotNull (aMapped);
      assertEquals (1, aMapped.size ());
      final MappedValue aValue = aMapped.getFirst ();
      assertNotNull (aValue);
      assertEquals (CONCEPT_TOOP_1, aValue.getSource ());
      assertEquals (CONCEPT_FR_1, aValue.getDestination ());
    }
  }

  @Test
  public void testNoMappingNeeded () throws IOException
  {
    for (final ISMMConceptProvider aCP : CP)
    {
      LOGGER.info ("Starting testNoMappingNeeded");
      final ISMMClient aClient = new SMMClient ();
      aClient.addConceptToBeMapped (CONCEPT_FR_1);
      final IMappedValueList ret = aClient.performMapping (CMockSMM.LOG_PREFIX, CMockSMM.NS_FREEDONIA, aCP, UCB, MMCB);
      assertNotNull (ret);
      assertEquals (1, ret.size ());

      // Check concept has a 1:1 mapping
      final IMappedValueList aMapped = ret.getAllBySource (x -> x.equals (CONCEPT_FR_1));
      assertNotNull (aMapped);
      assertEquals (1, aMapped.size ());
      final MappedValue aValue = aMapped.getFirst ();
      assertNotNull (aValue);
      assertEquals (CONCEPT_FR_1, aValue.getSource ());
      assertEquals (CONCEPT_FR_1, aValue.getDestination ());
    }
  }

  @Test
  public void testCreateAllMappings () throws IOException
  {
    final IMicroDocument aDoc = new MicroDocument ();
    final IMicroElement eRoot = aDoc.appendElement ("root");
    // Get all namespaces
    final ICommonsOrderedSet <String> aNSs = SMMConceptProviderGRLCRemote.remoteQueryAllNamespaces (CMockSMM.LOG_PREFIX);
    final ISMMConceptProvider aCP = new SMMConceptProviderGRLCRemote ();
    final ICommonsSortedSet <String> aToopConcepts = new CommonsTreeSet <> ();

    // For all namespaces
    for (final String sSrc : aNSs)
      if (!sSrc.equals (CMockSMM.NS_TOOP))
      {
        final IMicroElement eValueList = eRoot.appendElement ("value-list");
        eValueList.setAttribute ("srcns", sSrc);
        eValueList.setAttribute ("dstns", CMockSMM.NS_TOOP);

        // Get all mappings to TOOP
        final MappedValueList aMVL = aCP.getAllMappedValues (CMockSMM.LOG_PREFIX, sSrc, CMockSMM.NS_TOOP);
        for (final MappedValue aItem : CollectionHelper.getSorted (aMVL,
                                                                   Comparator.comparing (x -> x.getSource ()
                                                                                               .getValue ())))
        {
          final IMicroElement eItem = eValueList.appendElement ("item");
          eItem.setAttribute ("srcval", aItem.getSource ().getValue ());

          final String sTOOPConcept = aItem.getDestination ().getValue ();
          eItem.setAttribute ("dstval", sTOOPConcept);

          aToopConcepts.add (sTOOPConcept);

          if (EToopConcept.getFromIDOrNull (sTOOPConcept) == null)
            LOGGER.warn ("The TOOP concept '" + sTOOPConcept + "' is unknown!");
        }
      }
    MicroWriter.writeToFile (aDoc, new File ("src/test/resources/existing-smm-mappings.xml"));
    LOGGER.info ("done");

    LOGGER.info ("All TOOP concepts:\n" + StringHelper.getImploded ('\n', aToopConcepts));
  }
}
