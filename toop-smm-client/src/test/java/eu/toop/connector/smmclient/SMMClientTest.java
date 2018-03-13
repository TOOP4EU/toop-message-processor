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
package eu.toop.connector.smmclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.toop.commons.concept.ConceptValue;
import eu.toop.commons.doctype.EToopDocumentType;
import eu.toop.connector.api.CTC;
import eu.toop.connector.smmclient.IMappedValueList;
import eu.toop.connector.smmclient.MappedValue;
import eu.toop.connector.smmclient.SMMClient;

/**
 * Test class for class {@link SMMClient}.
 *
 * @author Philip Helger
 */
public final class SMMClientTest {
  private static final Logger s_aLogger = LoggerFactory.getLogger (SMMClientTest.class);

  private static final String LOG_PREFIX = "[unit test] ";
  private static final String NS_TOOP = EToopDocumentType.DOCTYPE_REGISTERED_ORGANIZATION_REQUEST.getSharedToopSMMNamespace ();
  private static final ConceptValue CONCEPT_TOOP_1 = new ConceptValue (NS_TOOP, "CompanyCode");
  private static final ConceptValue CONCEPT_FR_1 = new ConceptValue (CTC.NS_FREEDONIA, "FreedoniaBusinessCode");

  @Test
  public void testEmpty () throws IOException {
    s_aLogger.info ("Starting testEmpty");
    final SMMClient aClient = new SMMClient ();
    final IMappedValueList ret = aClient.performMapping (LOG_PREFIX, CTC.NS_FREEDONIA);
    assertNotNull (ret);
    assertTrue (ret.isEmpty ());
    assertEquals (0, ret.size ());
  }

  @Test
  public void testOneMatch () throws IOException {
    s_aLogger.info ("Starting testOneMatch");
    final SMMClient aClient = new SMMClient ();
    aClient.addConceptToBeMapped (CONCEPT_TOOP_1);
    final IMappedValueList ret = aClient.performMapping (LOG_PREFIX, CTC.NS_FREEDONIA);
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

  @Test
  public void testOneMatchOneNotFound () throws IOException {
    s_aLogger.info ("Starting testOneMatchOneNotFound");
    final SMMClient aClient = new SMMClient ();
    aClient.addConceptToBeMapped (CONCEPT_TOOP_1);
    aClient.addConceptToBeMapped (NS_TOOP, "NonExistingField");
    aClient.addConceptToBeMapped ("SourceNamespace", "NonExistingField");
    final IMappedValueList ret = aClient.performMapping (LOG_PREFIX, CTC.NS_FREEDONIA);
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

  @Test
  public void testNoMappingNeeded () throws IOException {
    s_aLogger.info ("Starting testNoMappingNeeded");
    final SMMClient aClient = new SMMClient ();
    aClient.addConceptToBeMapped (CONCEPT_FR_1);
    final IMappedValueList ret = aClient.performMapping (LOG_PREFIX, CTC.NS_FREEDONIA);
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