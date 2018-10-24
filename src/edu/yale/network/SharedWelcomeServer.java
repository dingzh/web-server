package edu.yale.network;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.Thread.sleep;


public class SharedWelcomeServer implements WebServer {

    private final int port;
    private final Logger logger = Logger.getLogger(SequentialServer.class.getSimpleName());

    private final int cacheSize;
    private final int timeout;
    private final int threadPoolSize;
    private final Monitor monitor;
    private final HashMap<String, String> docRoots;


    SharedWelcomeServer(int port, int cacheSize, int threadPoolSize,
                        Monitor monitor, int timeout, HashMap<String, String> docRoots) {
        this.port = port;
        this.cacheSize = cacheSize;
        this.monitor = monitor;
        this.timeout = timeout * 1000;
        this.docRoots = docRoots;
        this.threadPoolSize = threadPoolSize;
    }

    public void start() {
        Thread[] threadPool = new Thread[threadPoolSize];
        try (ServerSocket server = new ServerSocket(port)) {
            logger.info("Server started at " + port);

            for (int i = 0; i < threadPoolSize; ++i) {
                RequestHandlerSharedWelcome requestHandler =
                        new RequestHandlerSharedWelcome(server, cacheSize, monitor, timeout, docRoots);
                threadPool[i] = new Thread(requestHandler);
                threadPool[i].start();
            }
            threadPool[0].join();
        } catch (InterruptedException ex) {
            logger.info("Server interrupted, existing.");
            for (int i = 0; i < threadPoolSize; ++i) {
                threadPool[i].interrupt();
                try {
                    threadPool[i].join(1000);
                    logger.info("Joining thread " + i);
                } catch (InterruptedException x) {
                    threadPool[i].stop();
                }
            }
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Fail open server socket, server exits: ", ex);
        }
    }
}
