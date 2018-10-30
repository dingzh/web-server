package edu.yale.network.requesthandlers;

import edu.yale.network.SharedWelcomeServer;
import edu.yale.network.Util.Monitor;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;


public class RequestHandlerSharedWelcome extends RequestHandlerBase implements Runnable {
    private static final Logger logger = Logger.getLogger(RequestHandlerSequential.class.getCanonicalName());

    private final ServerSocket welcome;
    public RequestHandlerSharedWelcome(ServerSocket welcome, int cacheSize, Monitor monitor, int timeout, HashMap<String, String> docRoots) {
        super(cacheSize, monitor, timeout, docRoots);
        this.welcome = welcome;
    }

    @Override
    public void run() {
        while (!Thread.interrupted()) {
            Socket connection = null;
            synchronized (welcome) {
                try {
                    connection = welcome.accept();
                } catch (IOException ex) {
                    logger.log(Level.SEVERE, "Fail accepting client socket.", ex);
                }
            }
            if (connection != null) process(connection, SharedWelcomeServer.class.getSimpleName());
        }
    }
}

