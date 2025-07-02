package com.jjg.game.common.concurrent.processor;

import com.jjg.game.common.concurrent.BaseProcessor;

/**
 * 大厅逻辑线程
 *
 * @author 2CL
 */
public class HallProcessor extends BaseProcessor {
    public HallProcessor() {
        super("hall-thread");
    }
}
