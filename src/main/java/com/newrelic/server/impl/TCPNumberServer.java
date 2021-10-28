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
package com.newrelic.server.impl;

import com.newrelic.server.api.Log;
import com.newrelic.server.api.Server;
import com.newrelic.server.runner.FileLogConsumer;
import com.newrelic.server.runner.TCPClientAcceptor;
import com.newrelic.server.runner.TCPServerReporter;
import com.newrelic.server.utils.TCPServerConstants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * TCPNumberServer main class that implement TCP based number server.
 */
public class TCPNumberServer implements Server {

  private static final Logger log = LogManager.getLogger(TCPNumberServer.class);

  private BlockingDeque<Log> logQueue;
  private ExecutorService serverThreadPool;
  private ScheduledExecutorService reportExecutor;
  private CountDownLatch shutdownLatch;
  private volatile AtomicBoolean serverState;
  private String logFileLocation = TCPServerConstants.DEFAULT_LOG_FILE_LOCATION;
  private FileLogConsumer fileLogConsumer;
  private volatile AtomicInteger connectedClients = new AtomicInteger(0);
  private TCPClientAcceptor tcpClientAcceptor;
  private volatile AtomicLong totalUnique = new AtomicLong(0L);
  private volatile AtomicLong totalDuplicated = new AtomicLong(0L);

  public TCPNumberServer(CountDownLatch shutdownLatch) throws IOException {
    this.logQueue = new LinkedBlockingDeque<>();
    this.serverThreadPool = Executors.newFixedThreadPool(TCPServerConstants.SERVER_POOL_SIZE);
    this.reportExecutor = Executors.newSingleThreadScheduledExecutor();
    this.shutdownLatch = shutdownLatch;
    this.serverState = new AtomicBoolean(true);
    this.fileLogConsumer = new FileLogConsumer(logFileLocation, logQueue, serverState, totalUnique, totalDuplicated);
  }

  public TCPNumberServer(CountDownLatch shutdownLatch, String logFileLocation) throws IOException {
    this.logQueue = new LinkedBlockingDeque<>(TCPServerConstants.LOG_QUEUE_CAPACITY);
    this.serverThreadPool = Executors.newFixedThreadPool(TCPServerConstants.SERVER_POOL_SIZE);
    this.reportExecutor = Executors.newSingleThreadScheduledExecutor();
    this.shutdownLatch = shutdownLatch;
    this.serverState = new AtomicBoolean(true);
    this.logFileLocation = logFileLocation;
    this.fileLogConsumer = new FileLogConsumer(logFileLocation, logQueue, serverState, totalUnique, totalDuplicated);
  }

  @Override
  public void start() throws IOException {
    this.tcpClientAcceptor = new TCPClientAcceptor(TCPServerConstants.SERVER_PORT,
            TCPServerConstants.SERVER_MAX_CLIENT_SIZE,
            serverThreadPool, logQueue, serverState, shutdownLatch, connectedClients);
    this.serverThreadPool.submit(tcpClientAcceptor);
    this.serverThreadPool.submit(fileLogConsumer);
    this.reportExecutor.scheduleAtFixedRate(new TCPServerReporter(serverState, totalUnique, totalDuplicated),
            0, TCPServerConstants.SERVER_REPORT_DURATION, TimeUnit.SECONDS);
    log.info("TCPNumberServer server started successfully on port {}.", TCPServerConstants.SERVER_PORT);
  }

  @Override
  public void shutdown() {
    log.info("TCPNumberServer server shutdown initiated.");
    this.serverState.set(false);
    this.tcpClientAcceptor.close();
    this.serverThreadPool.shutdown();
    this.reportExecutor.shutdown();
    try {
      this.serverThreadPool.awaitTermination(TCPServerConstants.GRACEFUL_TERMINATION_DURATION,
              TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      log.error("Waiting on termination interrupted for thread pool serverThreadPool.", e);
    }
    try {
      this.reportExecutor.awaitTermination(TCPServerConstants.GRACEFUL_TERMINATION_DURATION,
              TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      log.error("Waiting on termination interrupted for thread pool reportExecutor.", e);
    }

    // consume the rest of the remaining logs in log queue
    if (!logQueue.isEmpty()) {
      while (!logQueue.isEmpty()) {
        Log log = logQueue.poll();
        fileLogConsumer.consume(log);
      }
    }
    this.fileLogConsumer.close();
    log.info("TCPNumberServer server shutdown completed.");
  }


  public Integer getConnectedClientsCount() {
    return this.connectedClients.get();
  }

  public Boolean isLogQueueEmpty() {
    return this.logQueue.isEmpty();
  }

}
