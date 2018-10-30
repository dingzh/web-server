package edu.yale.network;

import edu.yale.network.Util.Monitor;
import edu.yale.network.requesthandlers.RequestHandlerSharedWelcome;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;



public class SharedWelcomeServer extends WebServer {

    private final Logger logger = Logger.getLogger(SequentialServer.class.getSimpleName());

    SharedWelcomeServer(int port, int cacheSize, int threadPoolSize,
                        Monitor monitor, int timeout, HashMap<String, String> docRoots) {
        super(port, cacheSize, threadPoolSize, monitor, timeout, docRoots);
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
            Thread.currentThread().join();
        } catch (InterruptedException ex) {
            logger.info("Server interrupted, existing.");
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Fail open server socket, server exits: ", ex);
        } finally {
            for (int i = 0; i < threadPoolSize; ++i) {
                threadPool[i].interrupt();
                try {
                    threadPool[i].join(1000);
                    logger.info("Joining thread " + i);
                } catch (InterruptedException x) {
                    threadPool[i].stop();
                }
            }
        }
    }
}
