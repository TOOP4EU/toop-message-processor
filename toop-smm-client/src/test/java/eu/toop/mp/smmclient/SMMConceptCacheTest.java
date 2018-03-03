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
package eu.toop.mp.smmclient;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import eu.toop.mp.api.CMP;

/**
 * Test class for class SMMConceptCache.
 *
 * @author Philip Helger
 */
public final class SMMConceptCacheTest {
  @Test
  public void testRemoteQuery () throws IOException {
    // The only existing mapping we have atm
    MappedValueList aMVL = SMMConceptCache.remoteQueryAllMappedValues (CMP.NS_TOOP, CMP.NS_FREEDONIA);
    assertNotNull (aMVL);
    assertFalse (aMVL.isEmpty ());

    // The only existing mapping we have atm
    aMVL = SMMConceptCache.remoteQueryAllMappedValues (CMP.NS_FREEDONIA, CMP.NS_TOOP);
    assertNotNull (aMVL);
    assertFalse (aMVL.isEmpty ());
  }
}