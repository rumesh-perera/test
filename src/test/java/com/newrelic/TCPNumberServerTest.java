/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.newrelic;

import com.newrelic.client.TCPTestClient;
import com.newrelic.server.impl.TCPNumberServer;
import com.newrelic.server.utils.TCPServerUtils;
import org.junit.Test;
import org.junit.Assert;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

public class TCPNumberServerTest {


    @Test
    public void testTCPServerDigitParser() {
      Assert.assertEquals("999999999 is a valid digit sequence",
              TCPServerUtils.isValidNineDigitString("999999999"), true);
      Assert.assertEquals("99999999 is NOT valid digit sequence",
              TCPServerUtils.isValidNineDigitString("99999999"), false);
      Assert.assertEquals("0 is NOT valid digit sequence",
              TCPServerUtils.isValidNineDigitString("0"), false);
    }


  @Test
  public void testTCPServerNumberOfClients() throws IOException {
    TCPNumberServer numberServer = new TCPNumberServer(new CountDownLatch(1));

    numberServer.start();
    int maxNumClients = 0;
    while (true){
      TCPTestClient testClient;
      try {
        testClient = new TCPTestClient("localhost", 4000);
        maxNumClients++;
      } catch (IOException e){
        break;
      }
    }
    numberServer.shutdown();
    Assert.assertEquals("Max Concurrent Clients for Number Server is 5",
            maxNumClients, 5);

  }


}
