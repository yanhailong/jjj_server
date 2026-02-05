package com.jjg.game.slots.game.demonchild.data;

import com.jjg.game.slots.data.SlotsResultLib;

/**
 * @author lm
 * @date 2025/12/2 17:30
 */
public class DemonChildResultLib extends SlotsResultLib<DemonChildAwardLineInfo> {
    //增加的免费次数
    private int addFreeCount;

    public int getAddFreeCount() {
        return addFreeCount;
    }

    public void setAddFreeCount(int addFreeCount) {
        this.addFreeCount = addFreeCount;
    }
}
