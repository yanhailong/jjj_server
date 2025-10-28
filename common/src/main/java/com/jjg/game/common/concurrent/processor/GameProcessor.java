package com.jjg.game.common.concurrent.processor;

import com.jjg.game.common.concurrent.BaseFuncProcessor;

/**
 * 游戏房间逻辑线程
 *
 * @author 2CL
 */
public class GameProcessor extends BaseFuncProcessor {
    public GameProcessor(int threadId) {
        super("game-thread-" + threadId);
    }

}
