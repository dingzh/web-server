package edu.yale.network.Util;

import edu.yale.network.Util.Monitor;

import java.util.HashMap;

public class ServerConf {

    public final int cacheSize;
    public final int timeout;
    public final int threadPoolSize;
    public final Monitor monitor;
    public final HashMap<String, String> docRoots;

    public ServerConf(int cacheSize, int threadPoolSize,
               Monitor monitor, int timeout, HashMap<String, String> docRoots) {
        this.cacheSize = cacheSize;
        this.monitor = monitor;
        this.timeout = timeout;
        this.docRoots = docRoots;
        this.threadPoolSize = threadPoolSize;
    }
}
