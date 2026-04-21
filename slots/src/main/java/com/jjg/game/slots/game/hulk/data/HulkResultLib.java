package com.jjg.game.slots.game.hulk.data;

import com.jjg.game.slots.data.SlotsResultLib;

/**
 * @author 11
 * @date 2026/1/15
 */
public class HulkResultLib extends SlotsResultLib<HulkAwardLineInfo> {
    //触发局的倍数
    private long triggerTimes;

    public long getTriggerTimes() {
        return triggerTimes;
    }

    public void setTriggerTimes(long triggerTimes) {
        this.triggerTimes = triggerTimes;
    }
}
