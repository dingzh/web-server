package edu.yale.network;

import java.util.HashMap;

public class ServerReadWriteHandlerFactory implements IReadWriteHandlerFactory {

    private final int cacheSize;
    private final int timeout;
    private final Monitor monitor;
    private final HashMap<String, String> docRoots;

    ServerReadWriteHandlerFactory(int cacheSize, int timeout, Monitor monitor, HashMap<String, String> docRoots) {
        this.cacheSize = cacheSize;
        this.timeout = timeout;
        this.monitor = monitor;
        this.docRoots = docRoots;
    }

    public IReadWriteHandler createHandler() {
        return new ServerReadWriteHandler(cacheSize, timeout, monitor, docRoots);
    }
}
