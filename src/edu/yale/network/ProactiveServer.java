package edu.yale.network;

import edu.yale.network.Util.Attachment;
import edu.yale.network.Util.Monitor;
import edu.yale.network.Util.ServerConf;
import edu.yale.network.proactiveUtil.ConnectedHandler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.util.HashMap;
import java.util.logging.Logger;

public class ProactiveServer extends WebServer{
    private final Logger logger = Logger.getLogger(ProactiveServer.class.getSimpleName());
    private final ServerConf serverConf;

    ProactiveServer(int port, int cacheSize, int threadPoolSize,
                    Monitor monitor, int timeout, HashMap<String, String> docRoots) {
        super(port, cacheSize, threadPoolSize, monitor, timeout, docRoots);
        serverConf = super.getServerConf();
    }

    @Override
    public void start() {
        try (AsynchronousServerSocketChannel listener = AsynchronousServerSocketChannel.open()) {
            listener.bind(new InetSocketAddress(port));
            Attachment attachment = new Attachment(serverConf, listener);
            ConnectedHandler connectedHandler = new ConnectedHandler(serverConf, listener) ;
            listener.accept(attachment, connectedHandler);

            Thread.currentThread().join();
        } catch (IOException ex) {
            logger.severe("Cannot open AsynchrosousServerSocket.");
        } catch (Exception ex) {
            logger.severe("Fail accepting client");
        }
    }
}
