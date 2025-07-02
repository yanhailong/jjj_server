package com.jjg.game.common.concurrent;

/**
 * 线程执行接口
 *
 * @author 2CL
 */
@FunctionalInterface
public interface IProcessorHandler {

    /**
     * handler 执行接口
     */
    void action() throws Exception;
}
