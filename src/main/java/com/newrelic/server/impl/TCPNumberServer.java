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

import java.io.IOException;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class TCPNumberServer implements Server {

  private BlockingDeque<Log> logQueue;
  private ExecutorService serverThreadPool;
  private ScheduledExecutorService reportExecutor;
  private CountDownLatch shutdownLatch;
  private ConcurrentHashMap<Integer, AtomicLong> integerCache;
  private volatile AtomicBoolean serverState;
  private String logFileLocation = TCPServerConstants.DEFAULT_LOG_FILE_LOCATION;
  private FileLogConsumer fileLogConsumer;

  public TCPNumberServer(CountDownLatch shutdownLatch) throws IOException {
    this.logQueue = new LinkedBlockingDeque<>();
    this.serverThreadPool = Executors.newFixedThreadPool(TCPServerConstants.SERVER_POOL_SIZE);
    this.reportExecutor  = Executors.newSingleThreadScheduledExecutor();
    this.shutdownLatch = shutdownLatch;
    this.integerCache = new ConcurrentHashMap<>(TCPServerConstants.MAX_NINE_DIGIT_INTEGER);
    this.serverState = new AtomicBoolean(true);
    this.fileLogConsumer = new FileLogConsumer(logFileLocation, logQueue, shutdownLatch, serverState);
  }

  public TCPNumberServer(CountDownLatch shutdownLatch, String logFileLocation) throws IOException {
    this.logQueue = new LinkedBlockingDeque<>();
    this.serverThreadPool = Executors.newFixedThreadPool(TCPServerConstants.SERVER_POOL_SIZE);
    this.reportExecutor  = Executors.newSingleThreadScheduledExecutor();
    this.shutdownLatch = shutdownLatch;
    this.integerCache = new ConcurrentHashMap<>(TCPServerConstants.MAX_NINE_DIGIT_INTEGER);
    this.serverState = new AtomicBoolean(true);
    this.logFileLocation = logFileLocation;
    this.fileLogConsumer = new FileLogConsumer(logFileLocation, logQueue, shutdownLatch, serverState);
  }


  public void start() throws IOException {
    serverThreadPool.submit(new TCPClientAcceptor(TCPServerConstants.SERVER_PORT,
            TCPServerConstants.SERVER_MAX_CLIENT_SIZE,
            serverThreadPool, logQueue, serverState, shutdownLatch));
    serverThreadPool.submit(fileLogConsumer);
    reportExecutor.scheduleAtFixedRate(new TCPServerReporter(integerCache, serverState),
            TCPServerConstants.SERVER_REPORT_DURATION,
            TCPServerConstants.SERVER_REPORT_DURATION, TimeUnit.SECONDS);
  }

  public void shutdown(){
    this.serverState.set(false);
    this.serverThreadPool.shutdown();
    this.reportExecutor.shutdown();
    try {
      this.serverThreadPool.awaitTermination(TCPServerConstants.GRACEFUL_TERMINATION_DURATION,
              TimeUnit.MILLISECONDS);
    } catch (InterruptedException e){
      // Ignore
    }
    try {
      this.reportExecutor.awaitTermination(TCPServerConstants.GRACEFUL_TERMINATION_DURATION,
              TimeUnit.MILLISECONDS);
    } catch (InterruptedException e){
      // Ignore
    }

    if (!logQueue.isEmpty()) {
      while (!logQueue.isEmpty()) {
        Log log = logQueue.poll();
        fileLogConsumer.consume(log);
      }
    }
    fileLogConsumer.close();
  }


}
