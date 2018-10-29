package edu.yale.network.reactiveUtil;

import edu.yale.network.reactiveUtil.IChannelHandler;

import java.nio.channels.SelectionKey;
import java.io.IOException;

public interface IAcceptHandler extends IChannelHandler {
    void handleAccept(SelectionKey key) throws IOException;
}