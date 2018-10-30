package edu.yale.network.reactiveUtil;

import java.io.IOException;
import java.nio.channels.SelectionKey;

public interface IAcceptHandler extends IChannelHandler {
    void handleAccept(SelectionKey key) throws IOException;
}