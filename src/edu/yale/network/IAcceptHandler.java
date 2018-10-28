package edu.yale.network;

import java.nio.channels.SelectionKey;
import java.io.IOException;

interface IAcceptHandler extends IChannelHandler {
    void handleAccept(SelectionKey key) throws IOException;
}