package edu.yale.network.reactiveUtil;

import java.io.IOException;
import java.nio.channels.SelectionKey;

public interface IReadWriteHandler extends IChannelHandler {

    void handleRead(SelectionKey key) throws IOException;

    void handleWrite(SelectionKey key) throws IOException;

    int getInitOps();
}


