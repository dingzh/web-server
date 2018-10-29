package edu.yale.network.requesthandlers;

import edu.yale.network.SharedQueueServer;
import edu.yale.network.Util.Monitor;

import java.net.Socket;
import java.util.HashMap;
import java.util.Queue;
import java.util.logging.Logger;



public class RequestHandlerSharedQueue extends RequestHandlerBase implements Runnable {
    // thread safe : logger, synchronize on cache, other variable are not shared
    private static final Logger logger = Logger.getLogger(RequestHandlerSequential.class.getCanonicalName());
    private final Queue<Socket> connPool;

    public RequestHandlerSharedQueue(Queue<Socket> connPool, int cacheSize, Monitor monitor, int timeout, HashMap<String, String> docRoots) {
        super(cacheSize, monitor, timeout, docRoots);
        this.connPool = connPool;
    }

    @Override
    public void run() {
        while (true) {
            Socket client;
            synchronized (connPool) {
                while (connPool.isEmpty()) {
                    try {
                        connPool.wait();
                    } catch (InterruptedException ex) {
                        logger.warning("Thread interrupted, existing. ");
                        return ;
                    }
                }
                client = connPool.remove();
            }
            process(client, SharedQueueServer.class.getSimpleName());
        }
    }

}

