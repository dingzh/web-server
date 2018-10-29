package edu.yale.network;

import edu.yale.network.Util.Monitor;
import edu.yale.network.Util.ServerConf;

import java.util.HashMap;

public abstract class WebServer {
    protected final int port;
    protected final int cacheSize;
    protected final int timeout;
    protected final int threadPoolSize;
    protected final Monitor monitor;
    protected final HashMap<String, String> docRoots;

    WebServer(int port, int cacheSize, int threadPoolSize,
              Monitor monitor, int timeout, HashMap<String, String> docRoots) {
        this.port = port;
        this.cacheSize = cacheSize;
        this.monitor = monitor;
        this.timeout = timeout * 1000;
        this.docRoots = docRoots;
        this.threadPoolSize = threadPoolSize;
    }

    public ServerConf getServerConf() {
        return new ServerConf(cacheSize, threadPoolSize, monitor, timeout, docRoots);
    }

    public abstract void start();
}
