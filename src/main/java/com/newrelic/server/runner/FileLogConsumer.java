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

import com.newrelic.server.api.Consumer;
import com.newrelic.server.api.Log;
import com.newrelic.server.utils.TCPServerConstants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * FileLogConsumer consumes logs from log queue and writes to log file.
 */
public class FileLogConsumer implements Consumer<Log>, Runnable {

  private static final Logger log = LogManager.getLogger(FileLogConsumer.class);

  private BlockingDeque<Log> logQueue;
  private volatile AtomicBoolean serverState;
  private PrintWriter fileWriter;
  private Integer PARTITION_SIZE = TCPServerConstants.MEMORY_MAPPED_PARTITION_SIZE;
  private Long PARTITION_BYTE_SIZE = TCPServerConstants.MEMORY_MAPPED_PARTITION_BYTE_SIZE;
  // use of MappedByteBuffer prevents JVM going out of memory by allocating memory for 1000000000 integers
  private MappedByteBuffer[] byteBuffers = new MappedByteBuffer[PARTITION_SIZE];
  private FileChannel channel;
  private Long INT_BYTE_SIZE = TCPServerConstants.INTEGER_BYTE_SIZE;
  private volatile AtomicLong totalUnique;
  private volatile AtomicLong totalDuplicate;

  public FileLogConsumer(String logfile,
                         BlockingDeque<Log> logQueue,
                         AtomicBoolean serverState,
                         AtomicLong totalUnique,
                         AtomicLong totalDuplicate) throws IOException {
    this.logQueue = logQueue;
    this.serverState = serverState;
    this.fileWriter = new PrintWriter(new FileWriter(logfile));
    this.totalUnique = totalUnique;
    this.totalDuplicate = totalDuplicate;

    File tempStore = File.createTempFile("file", "memory");
    tempStore.deleteOnExit();
    this.channel = new RandomAccessFile(tempStore, "rw").getChannel();

    for (int counter = 0; counter < 4; counter++) {
      byteBuffers[counter] = this.channel.map(FileChannel.MapMode.READ_WRITE,
              counter * PARTITION_BYTE_SIZE, PARTITION_BYTE_SIZE);
    }
  }

  @Override
  public void consume(Log log) {
    int number = Integer.valueOf(log.getContent());

    int index = (number % (PARTITION_BYTE_SIZE.intValue() / INT_BYTE_SIZE.intValue())) * INT_BYTE_SIZE.intValue();
    int partition = number / (PARTITION_BYTE_SIZE.intValue() / INT_BYTE_SIZE.intValue());

    int frequency = byteBuffers[partition].getInt(index);
    if (frequency == 0) {
      this.totalUnique.incrementAndGet();
      this.fileWriter.println(log.getContent());
    } else {
      this.totalDuplicate.incrementAndGet();
    }
    this.byteBuffers[partition].putInt(index, frequency + 1);
  }

  @Override
  public void close() {
    try {
      this.fileWriter.close();
      this.channel.close();
    } catch (IOException e) {
      log.error("Exception occurred at the close of log consumer.", e);
    }
  }

  @Override
  public void run() {
    while (serverState.get()) {
      try {
        Log log = logQueue.poll(2, TimeUnit.SECONDS);
        if (log == null) {
          continue;
        }
        consume(log);
      } catch (InterruptedException e) {
        log.error("Exception occurred when consuming log from log queue.", e);
      }
    }
  }

}
