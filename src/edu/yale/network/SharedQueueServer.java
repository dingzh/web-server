package edu.yale.network;

import edu.yale.network.Util.Monitor;
import edu.yale.network.requesthandlers.RequestHandlerSharedQueue;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;


public class SharedQueueServer extends WebServer {

    private final Logger logger = Logger.getLogger(SequentialServer.class.getSimpleName());

    SharedQueueServer(int port, int cacheSize, int threadPoolSize,
                      Monitor monitor, int timeout, HashMap<String, String> docRoots) {
        super(port, cacheSize, threadPoolSize, monitor, timeout, docRoots);
    }

    public void start() {
        ExecutorService threadPool = Executors.newFixedThreadPool(threadPoolSize);

        try (ServerSocket server = new ServerSocket(port)) {
            logger.info("Server started at " + port);
            Queue<Socket> connSocketPool = new ArrayDeque<>();

            for (int i = 0; i < threadPoolSize; ++i) {
                RequestHandlerSharedQueue requestHandler =
                        new RequestHandlerSharedQueue(connSocketPool, cacheSize, monitor, timeout, docRoots);
                threadPool.execute(requestHandler);
            }

            while (true) {
                Socket connection = server.accept();
                synchronized (connSocketPool) {
                    connSocketPool.add(connection);
                    connSocketPool.notifyAll();
                }
            }
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Server exits: ", ex);
        } finally {
            threadPool.shutdownNow();
        }
    }
}
