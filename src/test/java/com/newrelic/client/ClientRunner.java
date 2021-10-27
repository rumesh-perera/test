package com.newrelic.client;

public class ClientRunner implements Runnable {

  private TCPTestClient client;

  public  ClientRunner(TCPTestClient client){
    this.client = client;
  }

  @Override
  public void run() {
    for (int counter = 0; counter < 10000000; counter++) {
      String number = String.valueOf(counter);
      int leadingZeros = 9 - number.toCharArray().length;
      StringBuffer buffer = new StringBuffer();
      for (int i = 0; i < leadingZeros; i++) {
        buffer.append(0);
      }
      buffer.append(number);
      client.sendMessage(buffer.toString());
    }
  }
}
