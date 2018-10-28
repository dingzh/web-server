package edu.yale.network;

import java.nio.channels.SelectionKey;
import java.io.IOException;

public interface IReadWriteHandler extends IChannelHandler {

    void handleRead(SelectionKey key) throws IOException;

    void handleWrite(SelectionKey key) throws IOException;

    int getInitOps();
}


