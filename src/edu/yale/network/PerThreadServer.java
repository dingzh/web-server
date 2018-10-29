package edu.yale.network;

import edu.yale.network.Util.Monitor;
import edu.yale.network.requesthandlers.RequestHandlerSequential;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;


public class PerThreadServer extends WebServer {

    private final int port;
    private final Logger logger = Logger.getLogger(PerThreadServer.class.getSimpleName());

    private final int cacheSize;
    private final int timeout;
    private final Monitor monitor;
    private final HashMap<String, String> docRoots;

    PerThreadServer(int port, int cacheSize, int threadPoolSize,
                    Monitor monitor, int timeout, HashMap<String, String> docRoots) {
        super(port, cacheSize, threadPoolSize, monitor, timeout, docRoots);
        this.port = port;
        this.cacheSize = cacheSize;
        this.monitor = monitor;
        this.timeout = timeout * 1000;
        this.docRoots = docRoots;
    }

    public void start() {
        try (ServerSocket server = new ServerSocket(port)) {
            logger.info("Server started at " + port);

            while (true) {
                Socket connection = server.accept();
                RequestHandlerSequential requestHandlerSequential =
                        new RequestHandlerSequential(connection, cacheSize, monitor, timeout, docRoots);
                Thread t = new Thread(requestHandlerSequential);
                t.start();
            }
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Server exits: ", ex);
        }
    }
}
