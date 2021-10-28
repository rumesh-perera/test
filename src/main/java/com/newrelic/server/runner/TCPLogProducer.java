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

package com.newrelic.server.runner;

import com.newrelic.server.api.Log;
import com.newrelic.server.api.Producer;
import com.newrelic.server.impl.TextLog;
import com.newrelic.server.utils.TCPServerConstants;
import com.newrelic.server.utils.TCPServerUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * TCPLogProducer reads data from TCP socket and produces to log queue.
 */
public class TCPLogProducer implements Producer<Log>, Runnable {

  private static final Logger log = LogManager.getLogger(TCPLogProducer.class);

  private Socket socket;
  private BufferedReader socketReader;
  private BlockingDeque<Log> logQueue;
  private CountDownLatch shutdownLatch;
  private volatile AtomicBoolean serverState;
  private volatile AtomicInteger connectedClients;

  public TCPLogProducer(Socket socket,
                        BlockingDeque<Log> logQueue,
                        CountDownLatch shutdownLatch,
                        AtomicBoolean serverState,
                        AtomicInteger connectedClients) throws IOException {
    this.socket = socket;
    this.logQueue = logQueue;
    this.socketReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    this.shutdownLatch = shutdownLatch;
    this.serverState = serverState;
    this.connectedClients = connectedClients;
  }

  @Override
  public void produce(Log producedLog) {
    try {
      logQueue.put(producedLog);
    } catch (InterruptedException e) {
      log.error("Exception occurred when consuming log from log queue.", e);
    }
  }

  @Override
  public void close() {
    try {
      this.socket.close();
      this.socketReader.close();
    } catch (IOException e) {
      log.error("Exception occurred at closing Log producer.", e);
    }
  }

  @Override
  public void run() {
    while (serverState.get()) {
      try {
        String textLine = this.socketReader.readLine();
        // check for valid digit sequence.
        if (TCPServerUtils.isValidNineDigitString(textLine)) {
          Log log = new TextLog(textLine);
          produce(log);
          // check for terminate sequence.
        } else if (TCPServerConstants.TERMINATE_SEQUENCE.equals(textLine)) {
          close();
          this.shutdownLatch.countDown();
          break;
        } else {
          // unknown sequence and client connection is terminated.
          close();
          break;
        }
      } catch (IOException e) {
        log.error("Exception occurred when reading logs on tcp socket.", e);
        // continue to next iteration
        continue;
      }
    }
    this.connectedClients.decrementAndGet();
  }

}
