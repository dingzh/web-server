package edu.yale.network;

import edu.yale.network.Util.Monitor;
import edu.yale.network.requesthandlers.RequestHandlerSequential;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SequentialServer extends WebServer {

    private final Logger logger = Logger.getLogger(SequentialServer.class.getSimpleName());

    SequentialServer(int port, int cacheSize, int threadPoolSize,
                     Monitor monitor, int timeout, HashMap<String, String> docRoots) {
        super(port, cacheSize, threadPoolSize, monitor, timeout, docRoots);
    }

    public void start() {
        try (ServerSocket server = new ServerSocket(port)) {
            logger.info("Server started at " + port);

            while (true) {
                Socket connection = server.accept();
                RequestHandlerSequential requestHandlerSequential =
                        new RequestHandlerSequential(connection, cacheSize, monitor, timeout, docRoots);
                requestHandlerSequential.run();
            }
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Server exits: ", ex);
        }
    }
}
