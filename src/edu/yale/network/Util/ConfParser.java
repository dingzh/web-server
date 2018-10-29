package edu.yale.network.Util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;

public class ConfParser {
    private final int port;
    private final int timeout;
    private final int cacheSize;
    private final int threadPoolSize;
    private final String monitorStr;
    private HashMap<String, String> conf = new HashMap<>();
    private HashMap<String, String> docRoots = new HashMap<>();

    public ConfParser(Path confPath) throws IOException{

        List<String> lines = Files.readAllLines(confPath);
        boolean virtualHost = false;
        String serverName = null;
        String docRoot = null;
        for (String line : lines) {
            if (line.equals("")) continue;
            line = line.trim();
            if (line.startsWith("<VirtualHost")) {
                virtualHost = true;
                continue ;
            }
            if (line.startsWith("</VirtualHost")) {
                virtualHost = false;
                if (docRoots.isEmpty()) { docRoots.put("default.host", docRoot); }
                docRoots.put(serverName, docRoot);
                continue ;
            }

            String[] tokens = line.split("\\s+");
            if (tokens.length != 2) throw new IOException("Illegal conf: " + line);
            if (virtualHost) {
                switch (tokens[0]) {
                    case "DocumentRoot":
                        docRoot = tokens[1];
                        break;
                    case "ServerName":
                        serverName = tokens[1];
                        break;
                    default:
                        throw new IOException("Illegal conf: " + line);
                }
            } else {
                conf.put(tokens[0].toLowerCase(), tokens[1]);
            }
        }

        // validate all parameters
        if (docRoots.isEmpty()) {
            throw new IOException("Illegal conf: missing virtual hosts.");
        }

        String threadPoolSizeStr = conf.getOrDefault("threadpoolsize", "4");
        threadPoolSize = Integer.parseInt(threadPoolSizeStr);
        if (threadPoolSize < 1) throw new IOException("Illegal conf: threadpool size = " + threadPoolSize);

        String portStr = conf.getOrDefault("listen", "6789");
        port = Integer.parseInt(portStr);

        String cacheSizeStr = conf.getOrDefault("cachesize", "8096");
        cacheSize = Integer.parseInt(cacheSizeStr);

        String timeoutStr = conf.getOrDefault("incompletetimeout","3");
        timeout = Integer.parseInt(timeoutStr);

        monitorStr = conf.getOrDefault("monitor", "DummyMonitor");
    }

    public int getTimeout() { return timeout; }

    public int getPort() { return port; }

    public int getCacheSize() { return cacheSize; }

    public int getThreadPoolSize() { return threadPoolSize; }

    public String getMonitor() { return monitorStr; }

    public HashMap<String, String> getDocRoots() { return docRoots; }
}
