package edu.yale.network.reactiveUtil;


import edu.yale.network.ReactiveServer;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Logger;

public class Dispatcher implements Runnable {

    private final Logger logger = Logger.getLogger(ReactiveServer.class.getSimpleName());
    private Selector selector;

    public Dispatcher() {
        // create selector
        try {
            selector = Selector.open();
        } catch (IOException ex) {
            System.out.println("Cannot create selector.");
            ex.printStackTrace();
            System.exit(1);
        } // end of catch
    } // end of Dispatcher

    public Selector selector() {
        return selector;
    }
    /*
     * public SelectionKey registerNewSelection(SelectableChannel channel,
     * IChannelHandler handler, int ops) throws ClosedChannelException {
     * SelectionKey key = channel.register(selector, ops); key.attach(handler);
     * return key; } // end of registerNewChannel
     *
     * public SelectionKey keyFor(SelectableChannel channel) { return
     * channel.keyFor(selector); }
     *
     * public void deregisterSelection(SelectionKey key) throws IOException {
     * key.cancel(); }
     *
     * public void updateInterests(SelectionKey sk, int newOps) {
     * sk.interestOps(newOps); }
     */

    public void run() {
        logger.info("Starting dispatcher thread.");
        while (true) {
            try {
                selector.select();
            } catch (IOException ex) {
                logger.severe("Exception thrown selecting ready events.");
                break;
            }

            Set<SelectionKey> readyKeys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = readyKeys.iterator();

            // iterate over all events
            while (iterator.hasNext()) {

                SelectionKey key = iterator.next();
                iterator.remove();

                try {
                    if (key.isAcceptable()) { // a new connection is ready to be
                        // accepted
                        IAcceptHandler aH = (IAcceptHandler) key.attachment();
                        aH.handleAccept(key);
                    } // end of isAcceptable

                    if (key.isReadable() || key.isWritable()) {
                        IReadWriteHandler rwH = (IReadWriteHandler) key.attachment();

                        if (key.isReadable()) {
                            rwH.handleRead(key);
                        } else {
                            rwH.handleWrite(key);
                        }
                    } // end of readwrite
                } catch (IOException ex) {
                    logger.severe("Exception thrown handling key " + key);
                    key.cancel();
                    try { key.channel().close(); } catch (IOException cex) { }
                } // end of catch

            } // end of while (iterator.hasNext()) {

        } // end of while (true)
    } // end of run
}
