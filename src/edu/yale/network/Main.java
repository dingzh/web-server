package edu.yale.network;

import edu.yale.network.Util.ConfParser;
import edu.yale.network.Util.Monitor;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

public class Main {

    public static void main(String[] args) {
	// write your code here
        if (args.length != 3 || !args[1].equals("-config")) {
            System.out.println("Usage: \n%java <servername> -config <config_file_name>");
            return ;
        }

        String webServerStr = args[0];
        Path confPath = Paths.get(args[2]);

        ConfParser conf;
        try {
            conf = new ConfParser(confPath);
        } catch (IOException ex) {
            System.out.println("Fail reading configuration: " + confPath.toString());
            return ;
        }

        int port = conf.getPort();
        int timeout = conf.getTimeout();
        int cacheSize = conf.getCacheSize();
        int threadPoolSize = conf.getThreadPoolSize();
        String monitorStr = conf.getMonitor();

        Monitor monitor;
        try {
            Class monitorClass = Class.forName("edu.yale.network." + monitorStr);
            monitor = (Monitor) monitorClass.newInstance();
            System.out.println("Monitor selected: " + monitorClass.getSimpleName());
        } catch (Exception ex) {
            System.out.println(String.format("Monitor %s not found.", monitorStr));
            return ;
        }

        HashMap<String, String> docRoots = conf.getDocRoots();

//        WebServer ws = new SequentialServer(port, cacheSize, monitor, timeout, docRoots);
        WebServer ws;
        try {
            Class webServerClass = Class.forName("edu.yale.network." + webServerStr);
            ws = (WebServer) webServerClass
                    .getDeclaredConstructor(Integer.TYPE, Integer.TYPE, Integer.TYPE, Monitor.class, Integer.TYPE, HashMap.class)
                    .newInstance(port, cacheSize, threadPoolSize, monitor, timeout, docRoots);
        } catch (Exception ex) {
            System.out.println("Server not implemented: " + webServerStr);
            return ;
        }

        ws.start();
    }
}
