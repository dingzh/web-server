package edu.yale.network.reactiveUtil;

import edu.yale.network.Util.Monitor;

import java.util.HashMap;

public class ServerReadWriteHandlerFactory implements IReadWriteHandlerFactory {

    private final int cacheSize;
    private final int timeout;
    private final Monitor monitor;
    private final HashMap<String, String> docRoots;

    public ServerReadWriteHandlerFactory(int cacheSize, int timeout, Monitor monitor, HashMap<String, String> docRoots) {
        this.cacheSize = cacheSize;
        this.timeout = timeout;
        this.monitor = monitor;
        this.docRoots = docRoots;
    }

    public IReadWriteHandler createHandler() {
        return new ServerReadWriteHandler(cacheSize, timeout, monitor, docRoots);
    }
}
