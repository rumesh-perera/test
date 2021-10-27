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

import com.newrelic.server.impl.ServerReport;
import com.newrelic.server.utils.TCPServerUtils;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class TCPServerReporter implements Runnable {

  private ServerReport lastReport;
  private volatile AtomicBoolean serverState;
  private volatile AtomicLong totalUnique;
  private volatile AtomicLong totalDuplicate;

  public TCPServerReporter(AtomicBoolean serverState,
                           AtomicLong totalUnique,
                           AtomicLong totalDuplicate) {
    this.lastReport = new ServerReport(0L, 0L,
            0L, 0L);
    this.serverState = serverState;
    this.totalUnique = totalUnique;
    this.totalDuplicate = totalDuplicate;
  }

  @Override
  public void run() {
    if (serverState.get()) {
      lastReport = TCPServerUtils.generateReport(lastReport, totalUnique, totalDuplicate);
      System.out.println(lastReport.print());
    }
  }

}
