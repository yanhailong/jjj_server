package com.jjg.game.slots.game.demonchild.data;

import com.jjg.game.slots.data.SlotsResultLib;

/**
 * @author lm
 * @date 2025/12/2 17:30
 */
public class DemonChildResultLib extends SlotsResultLib<DemonChildAwardLineInfo> {
    //免费总次数
    private int freeTotalCount;

    public int getFreeTotalCount() {
        return freeTotalCount;
    }

    public void setFreeTotalCount(int freeTotalCount) {
        this.freeTotalCount = freeTotalCount;
    }
}
