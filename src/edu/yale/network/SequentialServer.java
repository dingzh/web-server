package edu.yale.network;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SequentialServer implements WebServer {

    private final int port;
    private final Logger logger = Logger.getLogger(SequentialServer.class.getSimpleName());

    private final int cacheSize;
    private final int timeout;
    private final Monitor monitor;
    private final HashMap<String, String> docRoots;

    SequentialServer(int port, int cacheSize, int threadPoolSize,
                     Monitor monitor, int timeout, HashMap<String, String> docRoots) {
        this.port = port;
        this.cacheSize = cacheSize;
        this.monitor = monitor;
        this.timeout = timeout * 1000; // convert to millis
        this.docRoots = docRoots;
    }

    public void start() {
        try (ServerSocket server = new ServerSocket(port)) {
            logger.info("Server started at " + port);

            while (true) {
                Socket connection = server.accept();
                RequestHandler requestHandler =
                        new RequestHandler(connection, cacheSize, monitor, timeout, docRoots);
                requestHandler.process(connection);
            }
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Server exits: ", ex);
        }
    }
}
