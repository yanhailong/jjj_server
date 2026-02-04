package com.jjg.game.slots.game.frozenThrone.data;

import com.jjg.game.slots.data.SlotsPlayerGameData;
import com.jjg.game.slots.data.SlotsPlayerGameDataDTO;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author lihaocao
 * @date 2025/8/5 16:14
 */
public class FrozenThronePlayerGameDataDTO extends SlotsPlayerGameDataDTO {
    //缓存免费的结果库
    private FrozenThroneResultLib freeLib;
    //剩余的免费次数
    private int remainFreeCount;
    //当前的免费游戏数组中的下标值
    private int freeIndex;

    public FrozenThroneResultLib getFreeLib() {
        return freeLib;
    }

    public void setFreeLib(FrozenThroneResultLib freeLib) {
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
        if (t instanceof FrozenThronePlayerGameData playerGameData) {
            int safeFreeIndex = Math.max(0, this.freeIndex);
            int safeRemainFreeCount = Math.max(0, this.remainFreeCount);
            playerGameData.setFreeIndex(new AtomicInteger(safeFreeIndex));
            playerGameData.setRemainFreeCount(new AtomicInteger(safeRemainFreeCount));
            playerGameData.setFreeLib(this.freeLib);
        }
        return t;
    }
}
