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
import com.newrelic.server.utils.TCPServerUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;


public class FileLogConsumer implements Consumer<Log>, Runnable {

  private BlockingDeque<Log> logQueue;
  private volatile AtomicBoolean serverState;
  private volatile AtomicLong totalUnique;
  private volatile AtomicLong totalDuplicate;
  private PrintWriter fileWriter;
  private Integer PARTITION_SIZE = TCPServerConstants.MEMORY_MAPPED_PARTITION_SIZE;
  private Long PARTITION_BYTE_SIZE = TCPServerConstants.MEMORY_MAPPED_PARTITION_BYTE_SIZE;
  private MappedByteBuffer[] byteBuffers = new MappedByteBuffer[PARTITION_SIZE];
  private  FileChannel channel;
  private Integer INT_BYTE_SIZE = TCPServerConstants.INTEGER_BYTE_SIZE;

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
    this.fileWriter.println(log.getContent());
  }

  @Override
  public void close() {
    try {
      this.fileWriter.close();
      this.channel.close();
    }catch (IOException e){
      //Ignore
    }
  }


  @Override
  public void run() {
    while (serverState.get()) {
      try {
        Log log = logQueue.take();
        int number = Integer.valueOf(log.getContent());

        int index = (number % (PARTITION_BYTE_SIZE.intValue() / INT_BYTE_SIZE)) * INT_BYTE_SIZE;
        int partition = number / (PARTITION_BYTE_SIZE.intValue() / INT_BYTE_SIZE);

        int frequency = byteBuffers[partition].getInt(index);
        if (frequency == 0) {
          totalUnique.incrementAndGet();
          consume(log);
        } else {
          totalDuplicate.incrementAndGet();
        }
        byteBuffers[partition].putInt(index, frequency + 1);
      } catch (InterruptedException e) {
        //Ignore
      }
    }
  }
}
