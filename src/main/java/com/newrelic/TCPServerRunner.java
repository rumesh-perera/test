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

import com.newrelic.server.impl.TCPNumberServer;
import com.newrelic.server.utils.TCPServerConstants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.CountDownLatch;

/**
 * TCPServerRunner is the main class which initiates TCPNumberServer main thread.
 */
public class TCPServerRunner {

  private static final Logger log = LogManager.getLogger(TCPServerRunner.class);

  public static void main(String[] args) throws Exception {
    String logFileLocation = System.getProperty(TCPServerConstants.LOG_FILE_JVM_PARAM,
            TCPServerConstants.DEFAULT_LOG_FILE_LOCATION);

    log.info("Initializing TCPNumberServer.");
    CountDownLatch shutdownLatch = new CountDownLatch(1);

    TCPNumberServer server = new TCPNumberServer(shutdownLatch, logFileLocation);
    // adding shutdown hook prevent JVM existing before completing server.shutdown();
    Runtime.getRuntime()
            .addShutdownHook(new Thread(() -> {
              server.shutdown();
              log.info("Shutdown hook triggered and Server shutdown gracefully.");
            }));

    server.start();

    // prevent main thread from existing to complete rest of spawned threads.
    shutdownLatch.await();
    // shutdown server gracefully
    server.shutdown();
  }


}
