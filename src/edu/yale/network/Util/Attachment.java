package edu.yale.network.Util;

import java.nio.channels.AsynchronousServerSocketChannel;

public class Attachment {
    public ServerConf serverConf;
    public AsynchronousServerSocketChannel server;
    public Attachment(ServerConf serverConf, AsynchronousServerSocketChannel server) {
        this.server = server;
        this.serverConf = serverConf;
    }
}
