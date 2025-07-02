package com.jjg.game.common.concurrent.priority;

/**
 * 玩家在线程中的优先级
 *
 * @author 2CL
 */
public class PlayerPriority {

    /**
     * 优先级 默认为0 如果此值设置>0 则表示要做优先级排序
     */
    private int priority;

    public PlayerPriority(int priority) {
        this.priority = priority;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }
}
