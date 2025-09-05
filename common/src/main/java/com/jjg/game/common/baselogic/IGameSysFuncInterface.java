package com.jjg.game.common.baselogic;

/**
 * 游戏系统顶层接口
 *
 * @author 2CL
 */
public interface IGameSysFuncInterface {

    /**
     * 功能执行顺序
     */
    default int executeOrder() {
        return Integer.MIN_VALUE;
    }
}
