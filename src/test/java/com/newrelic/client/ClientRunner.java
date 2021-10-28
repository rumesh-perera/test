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

package com.newrelic.client;

/**
 * Test TCP client runner.
 */
public class ClientRunner implements Runnable {

  private TCPTestClient client;
  private Integer uniqueCount = 10000000;

  public ClientRunner(TCPTestClient client) {
    this.client = client;
  }

  public ClientRunner(TCPTestClient client, Integer uniqueCount) {
    this.client = client;
    this.uniqueCount = uniqueCount;
  }

  @Override
  public void run() {
    for (int counter = 0; counter < this.uniqueCount; counter++) {
      String number = String.valueOf(counter);
      int leadingZeros = 9 - number.toCharArray().length;
      StringBuffer buffer = new StringBuffer();
      for (int i = 0; i < leadingZeros; i++) {
        buffer.append(0);
      }
      buffer.append(number);
      this.client.sendMessage(buffer.toString());
    }
  }

}
