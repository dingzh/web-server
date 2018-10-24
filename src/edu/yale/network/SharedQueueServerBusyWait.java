package edu.yale.network;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;


public class SharedQueueServerBusyWait implements WebServer {

    private final int port;
    private final Logger logger = Logger.getLogger(SequentialServer.class.getSimpleName());

    private final int cacheSize;
    private final int timeout;
    private final int threadPoolSize;
    private final Monitor monitor;
    private final HashMap<String, String> docRoots;


    SharedQueueServerBusyWait (int port, int cacheSize, int threadPoolSize,
                      Monitor monitor, int timeout, HashMap<String, String> docRoots) {
        this.port = port;
        this.cacheSize = cacheSize;
        this.monitor = monitor;
        this.timeout = timeout * 1000;
        this.docRoots = docRoots;
        this.threadPoolSize = threadPoolSize;
    }

    public void start() {
        ExecutorService threadPool = Executors.newFixedThreadPool(threadPoolSize);

        try (ServerSocket server = new ServerSocket(port)) {
            logger.info("Server started at " + port);
            Queue<Socket> connSocketPool = new ArrayDeque<>();

            for (int i = 0; i < threadPoolSize; ++i) {
                RequestHandlerSharedQueueBusyWait requestHandler =
                        new RequestHandlerSharedQueueBusyWait(connSocketPool, cacheSize, monitor, timeout, docRoots);
                threadPool.execute(requestHandler);
            }

            while (true) {
                Socket connection = server.accept();
                synchronized (connSocketPool) {
                    connSocketPool.add(connection);
                }
            }
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Server exits: ", ex);
        } finally {
            threadPool.shutdownNow();
        }
    }
}


