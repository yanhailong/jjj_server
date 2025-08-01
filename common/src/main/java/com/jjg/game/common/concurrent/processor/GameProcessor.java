package com.jjg.game.common.concurrent.processor;

import com.jjg.game.common.concurrent.BaseFuncProcessor;
import com.jjg.game.common.concurrent.BaseHandler;

/**
 * 游戏房间逻辑线程
 *
 * @author 2CL
 */
public class GameProcessor extends BaseFuncProcessor {
    public GameProcessor(int threadId) {
        super("game-thread-" + threadId);
    }

    @Override
    public void executeHandler(BaseHandler<?> handler) {
        super.executeHandler(handler);
    }
}
