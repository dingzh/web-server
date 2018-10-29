package edu.yale.network.reactiveUtil;

import edu.yale.network.reactiveUtil.IReadWriteHandler;

public interface IReadWriteHandlerFactory {
    IReadWriteHandler createHandler();
}
