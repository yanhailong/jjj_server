package com.jjg.game.slots.game.demonchild.dao;

import com.jjg.game.slots.data.SlotsPlayerGameData;
import com.jjg.game.slots.data.SlotsPlayerGameDataDTO;
import com.jjg.game.slots.game.demonchild.data.DemonChildPlayerGameData;
import com.jjg.game.slots.game.demonchild.data.DemonChildResultLib;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author 11
 * @date 2025/8/5 14:11
 */
 @Document
public class DemonChildPlayerGameDataDTO extends SlotsPlayerGameDataDTO {
    //缓存免费的结果库
    private DemonChildResultLib freeLib;
    //剩余的免费次数
    private int remainFreeCount;
    //当前的免费游戏数组中的下标值
    private int freeIndex;

    public DemonChildResultLib getFreeLib() {
        return freeLib;
    }

    public void setFreeLib(DemonChildResultLib freeLib) {
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
        if (t instanceof DemonChildPlayerGameData playerGameData) {
            int safeFreeIndex = Math.max(0, this.freeIndex);
            int safeRemainFreeCount = Math.max(0, this.remainFreeCount);
            playerGameData.setFreeIndex(new AtomicInteger(safeFreeIndex));
            playerGameData.setRemainFreeCount(new AtomicInteger(safeRemainFreeCount));
            playerGameData.setFreeLib(this.freeLib);
        }
        return t;
    }
}
