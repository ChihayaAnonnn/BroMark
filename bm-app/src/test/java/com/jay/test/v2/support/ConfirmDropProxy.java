package com.jay.test.v2.support;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Real AMQP TCP proxy: after arming, drops broker Basic.Ack and closes the link. */
public final class ConfirmDropProxy implements AutoCloseable {
  private final ServerSocket server = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
  private final ExecutorService workers = Executors.newFixedThreadPool(3);
  private volatile Socket client, broker;
  private final AtomicBoolean armed = new AtomicBoolean();
  public final CountDownLatch dropped = new CountDownLatch(1);

  public ConfirmDropProxy(String host, int port) throws IOException {
    workers.submit(
        () -> {
          try {
            client = server.accept();
            broker = new Socket(host, port);
            workers.submit(
                () -> {
                  try {
                    client.getInputStream().transferTo(broker.getOutputStream());
                  } catch (IOException ignored) {
                  }
                });
            var input = new DataInputStream(broker.getInputStream());
            var output = client.getOutputStream();
            while (!client.isClosed()) {
              byte[] header = input.readNBytes(7);
              if (header.length != 7) break;
              int size = java.nio.ByteBuffer.wrap(header, 3, 4).getInt();
              if (size < 0 || size > 16777216) throw new IOException("Invalid frame size");
              byte[] data = input.readNBytes(size + 1);
              if (data.length != size + 1) break;
              if (armed.get()
                  && header[0] == 1
                  && size >= 4
                  && data[0] == 0
                  && data[1] == 60
                  && data[2] == 0
                  && data[3] == 80) {
                dropped.countDown();
                client.close();
                broker.close();
                break;
              }
              output.write(header);
              output.write(data);
              output.flush();
            }
          } catch (IOException ignored) {
          }
        });
  }

  public int port() {
    return server.getLocalPort();
  }

  public void arm() {
    armed.set(true);
  }

  public void close() throws IOException {
    server.close();
    if (client != null) client.close();
    if (broker != null) broker.close();
    workers.shutdownNow();
  }
}
