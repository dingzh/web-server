package edu.yale.network;

import java.io.IOException;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

public class ResponseSentHandler implements CompletionHandler<Integer, AsynchronousSocketChannel> {
    @Override
    public void completed(Integer result, AsynchronousSocketChannel attachment) {
        try {attachment.close();} catch (IOException ex) {}
    }

    @Override
    public void failed(Throwable exc, AsynchronousSocketChannel attachment) {
        try {attachment.close();} catch (IOException ex) {}
    }
}
