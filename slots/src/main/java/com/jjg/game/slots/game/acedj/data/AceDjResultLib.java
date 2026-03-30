package com.jjg.game.slots.game.acedj.data;

import com.jjg.game.slots.data.SlotsResultLib;

import java.util.List;

/**
 * @author lihaocao
 * @date 2025/12/2 17:30
 */
public class AceDjResultLib extends SlotsResultLib<AceDjAwardLineInfo> {
    //本次触发的jackpotId
    private int jackpotId;
    //消除补齐的信息
    private List<AceDjAddIconInfo> addIconInfos;
    //增加的免费次数
    private int addFreeCount;
    //初始化 wild倍数
    private List<Integer> wildTimes;

    public List<Integer> getWildTimes() {
        return wildTimes;
    }

    public void setWildTimes(List<Integer> wildTimes) {
        this.wildTimes = wildTimes;
    }

    public int getJackpotId() {
        return jackpotId;
    }

    public void setJackpotId(int jackpotId) {
        this.jackpotId = jackpotId;
    }

    public List<AceDjAddIconInfo> getAddIconInfos() {
        return addIconInfos;
    }

    public void setAddIconInfos(List<AceDjAddIconInfo> addIconInfos) {
        this.addIconInfos = addIconInfos;
    }

    public int getAddFreeCount() {
        return addFreeCount;
    }

    public void setAddFreeCount(int addFreeCount) {
        this.addFreeCount = addFreeCount;
    }
}
