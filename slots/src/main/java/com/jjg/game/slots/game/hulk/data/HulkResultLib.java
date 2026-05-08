package com.jjg.game.slots.game.hulk.data;

import com.jjg.game.slots.data.SlotsResultLib;

/**
 * @author 11
 * @date 2026/1/15
 */
public class HulkResultLib extends SlotsResultLib<HulkAwardLineInfo> {
    //触发局的倍数
    private long triggerTimes;
    //增加的免费次数
    private int addFreeCount;

    public long getTriggerTimes() {
        return triggerTimes;
    }

    public void setTriggerTimes(long triggerTimes) {
        this.triggerTimes = triggerTimes;
    }

    public int getAddFreeCount() {
        return addFreeCount;
    }

    public void setAddFreeCount(int addFreeCount) {
        this.addFreeCount = addFreeCount;
    }
}
