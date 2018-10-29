package edu.yale.network.reactiveUtil;

import edu.yale.network.reactiveUtil.IChannelHandler;

import java.nio.channels.SelectionKey;
import java.io.IOException;

public interface IReadWriteHandler extends IChannelHandler {

    void handleRead(SelectionKey key) throws IOException;

    void handleWrite(SelectionKey key) throws IOException;

    int getInitOps();
}


