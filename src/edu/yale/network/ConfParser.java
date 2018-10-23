package edu.yale.network;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;

class ConfParser {
    private final int port;
    private final int timeout;
    private final int cacheSize;
    private final String monitorStr;
    private HashMap<String, String> conf = new HashMap<>();
    private HashMap<String, String> docRoots = new HashMap<>();

    ConfParser(Path confPath) throws IOException{

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
                if (tokens[0].equals("DocumentRoot")) docRoot = tokens[1];
                else if (tokens[0].equals("ServerName")) serverName = tokens[1];
                else throw new IOException("Illegal conf: " + line);
            } else {
                conf.put(tokens[0].toLowerCase(), tokens[1]);
            }
        }

        // validate all parameters
        if (docRoots.isEmpty()) {
            throw new IOException("Illegal conf: missing virtual hosts.");
        }

        String portStr = conf.getOrDefault("listen", "6789");
        port = Integer.parseInt(portStr);

        String cacheSizeStr = conf.getOrDefault("cachesize", "8096");
        cacheSize = Integer.parseInt(cacheSizeStr);

        String timeoutStr = conf.getOrDefault("incompletetimeout","3");
        timeout = Integer.parseInt(timeoutStr);

        monitorStr = conf.getOrDefault("monitor", "DummyMonitor");
    }

    int getTimeout() {
        return timeout;
    }

    int getPort() {
        return port;
    }

    int getCacheSize() {
        return cacheSize;
    }

    String getMonitor() {
        return monitorStr;
    }

    HashMap<String, String> getDocRoots() {
        return docRoots;
    }
}
