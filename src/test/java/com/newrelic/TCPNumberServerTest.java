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

import com.newrelic.client.ClientRunner;
import com.newrelic.client.TCPTestClient;
import com.newrelic.server.impl.TCPNumberServer;
import com.newrelic.server.utils.TCPServerConstants;
import com.newrelic.server.utils.TCPServerUtils;
import org.junit.Test;
import org.junit.Assert;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Test cases for technical challenge.
 */
public class TCPNumberServerTest {

  @Test
  public void testTCPServerDigitParser() {
    Assert.assertEquals("999999999 is a valid digit sequence",
            TCPServerUtils.isValidNineDigitString("999999999"), true);
    Assert.assertEquals("99999999 is NOT valid digit sequence",
            TCPServerUtils.isValidNineDigitString("99999999"), false);
    Assert.assertEquals("0 is NOT valid digit sequence",
            TCPServerUtils.isValidNineDigitString("0"), false);
    Assert.assertEquals("1 is NOT valid digit sequence",
            TCPServerUtils.isValidNineDigitString("1"), false);
  }

  @Test
  public void testTCPServerNumberOfClients() throws IOException {
    TCPNumberServer numberServer = new TCPNumberServer(new CountDownLatch(1));
    numberServer.start();
    List<TCPTestClient> testClients = new ArrayList<>();
    while (true) {
      try {
        TCPTestClient testClient = new TCPTestClient(TCPServerConstants.LOCALHOST, TCPServerConstants.SERVER_PORT);
        testClients.add(testClient);
      } catch (IOException e) {
        break;
      }
    }
    Assert.assertEquals("Max Concurrent Clients for Number Server is 5.",
            5, numberServer.getConnectedClientsCount().intValue());
    numberServer.shutdown();
    testClients.forEach(client -> {
      client.stopConnection();
    });
  }

  @Test
  public void testTCPServerClientTermination() throws IOException, InterruptedException {
    CountDownLatch shutdownLatch = new CountDownLatch(1);
    TCPNumberServer numberServer = new TCPNumberServer(shutdownLatch);
    numberServer.start();
    List<TCPTestClient> testClients = new ArrayList<>();
    while (true) {
      try {
        TCPTestClient testClient = new TCPTestClient(TCPServerConstants.LOCALHOST, TCPServerConstants.SERVER_PORT);
        testClients.add(testClient);
      } catch (IOException e) {
        // break when first client disconnection
        break;
      }
    }
    Assert.assertEquals("Max Concurrent Clients for Number Server is 5.",
            5, numberServer.getConnectedClientsCount().intValue());
    //invalid sequence
    testClients.forEach(client -> {
      client.sendMessage(TCPServerConstants.TERMINATE_SEQUENCE);
    });
    shutdownLatch.await();

    Assert.assertTrue("Received TCP Server shutdown signal from latch. " +
            " Otherwise latch is forever waited until server is shutdown.", true);
    numberServer.shutdown();
    Assert.assertEquals("All TCP clients must be disconnected on termination.",
            0, numberServer.getConnectedClientsCount().intValue());
    testClients.forEach(client -> {
      client.stopConnection();
    });
  }

  @Test
  public void testTCPServerResilientOnDataLoses() throws IOException, InterruptedException {
    CountDownLatch shutdownLatch = new CountDownLatch(1);
    TCPNumberServer numberServer = new TCPNumberServer(shutdownLatch);
    numberServer.start();
    ExecutorService service = Executors.newFixedThreadPool(TCPServerConstants.SERVER_POOL_SIZE);
    int count = TCPServerConstants.SERVER_POOL_SIZE;
    while (count >= 0) {
      try {
        TCPTestClient testClient = new TCPTestClient(TCPServerConstants.LOCALHOST, TCPServerConstants.SERVER_PORT);
        ClientRunner runner = new ClientRunner(testClient);
        service.submit(runner);
      } catch (IOException e) {
        break;
      }
      count--;
    }
    service.shutdown();
    service.awaitTermination(10, TimeUnit.SECONDS);
    numberServer.shutdown();
    Assert.assertEquals("When TCP server shutdown is invoked and completed. " +
                    "Log queue should be empty and flushed to logfile. Thus avoiding data loses.",
            true, numberServer.isLogQueueEmpty());
  }

  @Test
  public void testTCPServerFileLogs() throws IOException, InterruptedException {
    TCPNumberServer numberServer = new TCPNumberServer(new CountDownLatch(1));
    numberServer.start();
    ExecutorService service = Executors.newFixedThreadPool(TCPServerConstants.SERVER_POOL_SIZE);
    int count = TCPServerConstants.SERVER_POOL_SIZE;
    while (count >= 0) {
      try {
        TCPTestClient testClient = new TCPTestClient(TCPServerConstants.LOCALHOST, TCPServerConstants.SERVER_PORT);
        ClientRunner runner = new ClientRunner(testClient, 5);
        service.submit(runner);
      } catch (IOException e) {
        break;
      }
      count--;
    }
    service.shutdown();
    service.awaitTermination(100, TimeUnit.SECONDS);
    numberServer.shutdown();
    BufferedReader reader = new BufferedReader(new FileReader(
            new File(TCPServerConstants.DEFAULT_LOG_FILE_LOCATION)));
    String line;
    int counter = 0;
    int totalLines = 0;
    while ((line = reader.readLine()) != null) {
      counter = Integer.valueOf(line) + counter;
      totalLines++;
    }
    Assert.assertEquals("Total lines of integers written to log file.", 5, totalLines);
    Assert.assertEquals("Total summation of integers written to log file.", 10, counter);
    reader.close();
  }

}
