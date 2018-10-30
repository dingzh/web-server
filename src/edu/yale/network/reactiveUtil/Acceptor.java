package edu.yale.network.reactiveUtil;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class Acceptor implements IAcceptHandler {

    private IReadWriteHandlerFactory srwf;

    public Acceptor(IReadWriteHandlerFactory srwf) {
        this.srwf = srwf;
    }

    public void handleException() {
        // TODO what is this
        System.out.println("handleException(): of Acceptor");
    }

    public void handleAccept(SelectionKey key) throws IOException {
        ServerSocketChannel server = (ServerSocketChannel) key.channel();

        // extract the ready connection
        SocketChannel client = server.accept();
        client.configureBlocking(false);

        /*
         * register the new connection with *read* events/operations
         * SelectionKey clientKey = client.register( selector,
         * SelectionKey.OP_READ);// | SelectionKey.OP_WRITE);
         */
        IReadWriteHandler rwH = srwf.createHandler();
        int ops = rwH.getInitOps();
        SelectionKey clientKey = client.register(key.selector(), ops);
        clientKey.attach(rwH);
    } // end of handleAccept

} // end of class
