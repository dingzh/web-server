package edu.yale.network;


import edu.yale.network.Util.Monitor;
import edu.yale.network.reactiveUtil.Acceptor;
import edu.yale.network.reactiveUtil.Dispatcher;
import edu.yale.network.reactiveUtil.IReadWriteHandlerFactory;
import edu.yale.network.reactiveUtil.ServerReadWriteHandlerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ReactiveServer extends WebServer {

    private final Logger logger = Logger.getLogger(ReactiveServer.class.getSimpleName());

    public ReactiveServer(int port, int cacheSize, int threadPoolSize,
                          Monitor monitor, int timeout, HashMap<String, String> docRoots) {
        super(port, cacheSize, threadPoolSize, monitor, timeout, docRoots);
    }

    public void start() {
        Dispatcher dispatcher = new Dispatcher();
        ServerSocketChannel sch = openServerChannel(port);

        IReadWriteHandlerFactory srwFactory = new ServerReadWriteHandlerFactory(cacheSize, timeout, monitor, docRoots);
        Acceptor acceptor = new Acceptor(srwFactory);

        try {
            SelectionKey key = sch.register(dispatcher.selector(), SelectionKey.OP_ACCEPT);
            key.attach(acceptor);

            // start dispatcher
//            Thread dispatcherThread = new Thread(dispatcher);
//            dispatcherThread.start();

            dispatcher.run();
//            dispatcherThread.join();
        } catch (IOException ex) {
            logger.severe("Cannot register and start server.");
//        } catch (InterruptedException ex) {
        }
    }

    private ServerSocketChannel openServerChannel(int port) {
        ServerSocketChannel serverChannel = null;
        try {
            serverChannel = ServerSocketChannel.open();

            // extract server socket of the server channel and bind the port
            ServerSocket ss = serverChannel.socket();
            InetSocketAddress address = new InetSocketAddress(port);
            ss.bind(address);
            serverChannel.configureBlocking(false);

            logger.info("Server listening for connections on port " + port);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Fail to open server socket channel.", ex);
            System.exit(1);
        } // end of catch
        return serverChannel;
    }
}
