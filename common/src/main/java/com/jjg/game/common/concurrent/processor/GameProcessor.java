package com.jjg.game.common.concurrent.processor;

import com.jjg.game.common.concurrent.BaseProcessor;

/**
 * 游戏房间逻辑线程
 *
 * @author 2CL
 */
public class GameProcessor extends BaseProcessor {
    public GameProcessor() {
        super("game-thread");
    }
}
