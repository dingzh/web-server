package edu.yale.network.requesthandlers;

import edu.yale.network.SequentialServer;
import edu.yale.network.Util.Monitor;

import java.net.Socket;
import java.util.HashMap;
import java.util.logging.Logger;


public class RequestHandlerSequential extends RequestHandlerBase implements Runnable {
    private static final Logger logger = Logger.getLogger(RequestHandlerSequential.class.getCanonicalName());

    private final Socket connection;
    public RequestHandlerSequential(Socket connection, int cacheSize, Monitor monitor, int timeout, HashMap<String, String> docRoots) {
        super(cacheSize, monitor, timeout, docRoots);
        this.connection = connection;
    }

    @Override
    public void run() {
        process(connection, SequentialServer.class.getSimpleName());
    }

}

