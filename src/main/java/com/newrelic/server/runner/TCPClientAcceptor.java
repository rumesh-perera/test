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

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.newrelic.server.api.Log;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TCPClientAcceptor implements Runnable {

  private static final Logger log = LogManager.getLogger(TCPClientAcceptor.class);

  private ServerSocket serverSocket;
  private volatile AtomicInteger connectedClients;
  private int maxNumClients;
  private ExecutorService serverThreadPool;
  private BlockingDeque<Log> logQueue;
  private volatile AtomicBoolean serverState;
  private CountDownLatch shutdownLatch;

  public TCPClientAcceptor(Integer port,
                           Integer maxNumClients,
                           ExecutorService serverThreadPool,
                           BlockingDeque<Log> logQueue,
                           AtomicBoolean serverState,
                           CountDownLatch shutdownLatch,
                           AtomicInteger connectedClients) throws IOException {
    this.connectedClients = connectedClients;
    this.serverSocket = new ServerSocket(port);
    this.maxNumClients = maxNumClients;
    this.serverThreadPool = serverThreadPool;
    this.logQueue = logQueue;
    this.serverState = serverState;
    this.shutdownLatch = shutdownLatch;
  }

  public void close(){
    try {
      this.serverSocket.close();
    } catch (IOException e){
      //Ignore
    }
  }

  @Override
  public void run() {
    while (serverState.get() &&
            connectedClients.get() < maxNumClients) {
      try {
        Socket socket = serverSocket.accept();
        connectedClients.incrementAndGet();
        TCPLogProducer logProducer = new TCPLogProducer(socket, logQueue, shutdownLatch,
                serverState, connectedClients);
        serverThreadPool.submit(logProducer);
      } catch (IOException e) {
        // Ignore
      }
    }
    close();
  }
}
