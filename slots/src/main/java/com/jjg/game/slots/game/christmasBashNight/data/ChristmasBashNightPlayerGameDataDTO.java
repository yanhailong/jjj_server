package com.jjg.game.slots.game.christmasBashNight.data;

import com.jjg.game.slots.data.SlotsPlayerGameData;
import com.jjg.game.slots.data.SlotsPlayerGameDataDTO;
import com.jjg.game.slots.data.SlotsPlayerGameDataIndexedDTO;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author lihaocao
 * @date 2025/8/5 16:14
 */
public class ChristmasBashNightPlayerGameDataDTO extends SlotsPlayerGameDataIndexedDTO {
    //缓存免费的结果库
    private ChristmasBashNightResultLib freeLib;
    //剩余的免费次数
    private int remainFreeCount;
    //当前的免费游戏数组中的下标值
    private int freeIndex;

    public ChristmasBashNightResultLib getFreeLib() {
        return freeLib;
    }

    public void setFreeLib(ChristmasBashNightResultLib freeLib) {
        this.freeLib = freeLib;
    }

    public int getRemainFreeCount() {
        return remainFreeCount;
    }

    public void setRemainFreeCount(int remainFreeCount) {
        this.remainFreeCount = remainFreeCount;
    }

    public int getFreeIndex() {
        return freeIndex;
    }

    public void setFreeIndex(int freeIndex) {
        this.freeIndex = freeIndex;
    }

    @Override
    public <T extends SlotsPlayerGameData> T converToGameData(Class<T> cla) throws Exception {
        T t = super.converToGameData(cla);
        if (t instanceof ChristmasBashNightPlayerGameData playerGameData) {
            int safeFreeIndex = Math.max(0, this.freeIndex);
            int safeRemainFreeCount = Math.max(0, this.remainFreeCount);
            playerGameData.setFreeIndex(new AtomicInteger(safeFreeIndex));
            playerGameData.setRemainFreeCount(new AtomicInteger(safeRemainFreeCount));
            playerGameData.setFreeLib(this.freeLib);
        }
        return t;
    }
}
