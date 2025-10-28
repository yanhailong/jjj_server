package com.jjg.game.common.concurrent.processor;

import com.jjg.game.common.concurrent.BaseFuncProcessor;

/**
 * 大厅逻辑线程
 *
 * @author 2CL
 */
public class HallProcessor extends BaseFuncProcessor {
    public HallProcessor(int threadId) {
        super("hall-thread-" + threadId);
    }

}
