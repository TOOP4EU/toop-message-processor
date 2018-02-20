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
package eu.toop.mp.me;

import java.io.IOException;
import java.net.URL;
import java.util.Properties;

/**
 * The properties of the gateway that we talk to. Assuming only one gateway for now
 *
 * @author: myildiz
 * @date: 15.02.2018.
 */
public class MessageExchangeEndpointConfig {
  /**
   * The URL of the gateway
   */
  public static URL GW_URL;

  /**
   * The party id of the Gateway
   */
  public static String GW_PARTY_ID;

  /**
   * The party id of Message exchange module
   */
  public static String ME_PARTY_ID;

  /**
   * The role of the gateway
   */
  public static String GW_PARTY_ROLE;

  /**
   * The role of the Message exchange module
   */
  public static String ME_PARTY_ROLE;

  /**
   * Submit action name
   */
  public static String SUBMIT_ACTION;

  /**
   * Submit service name
   */
  public static String SUBMIT_SERVICE;

  /**
   * the full name of the message exchange module
   */
  public static String ME_NAME;

  static {
    //initialize settings, read from resource
    Properties properties = new Properties();
    try {
      properties.load(MessageExchangeEndpointConfig.class.getResourceAsStream("/message-exchange.properties"));

      GW_URL = new URL(properties.getProperty("GW_URL"));
      GW_PARTY_ID = properties.getProperty("GW_PARTY_ID");
      ME_PARTY_ID = properties.getProperty("ME_PARTY_ID");
      GW_PARTY_ROLE = properties.getProperty("GW_PARTY_ROLE");
      ME_PARTY_ROLE = properties.getProperty("ME_PARTY_ROLE");
      SUBMIT_ACTION = properties.getProperty("SUBMIT_ACTION");
      SUBMIT_SERVICE = properties.getProperty("SUBMIT_SERVICE");
      ME_NAME = properties.getProperty("ME_NAME");

    } catch (IOException e) {
      e.printStackTrace();
      throw new RuntimeException("Error during initialization of the gateway configuration");
    }
  }

  /**
   * @return the name of the MP to be displayed in the ebms message id domain part
   */
  public static String getMEMName() {
    return ME_NAME;
  }
}
