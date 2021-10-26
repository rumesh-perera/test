package com.newrelic;

import com.newrelic.server.impl.TCPNumberServer;
import com.newrelic.server.utils.TCPServerConstants;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.CountDownLatch;

public class TCPServerRunner {

  public static void main(String[] args) throws Exception {
    File tmpStore = File.createTempFile("counter", "dat");
    tmpStore.deleteOnExit();

    FileChannel channel = new RandomAccessFile(tmpStore, "rw").getChannel();

    MappedByteBuffer lk = channel.map(FileChannel.MapMode.READ_WRITE, 0, 8);


    lk.

    String logFileLocation = System.getProperty(TCPServerConstants.LOG_FILE_JVM_PARAM,
            TCPServerConstants.DEFAULT_LOG_FILE_LOCATION);

//    Integer[] bigInt = new Integer[999999999];
//
//
//    File f = new File("./junk.txt");
//    f.createNewFile();
//    RandomAccessFile file = new RandomAccessFile("./junk.txt", "rw");
//
//    file.seek(10*0);
//    file.write("000000000\n".getBytes());
//
//    file.seek(10*999999999);
//    file.write("999999999\n".getBytes());
//
//    file.seek(0);
//
//    String fi = file.readLine();
//   fi = file.readLine();


   //  prevent main thread from existing
    CountDownLatch shutdownLatch = new CountDownLatch(1);

    TCPNumberServer server = new TCPNumberServer(shutdownLatch);
    Runtime.getRuntime()
            .addShutdownHook(new Thread(()-> server.shutdown()));

    server.start();

    shutdownLatch.await();
  }


}
