package com.jjg.game.slots.game.angrybirds.dao;

import com.jjg.game.slots.data.SlotsPlayerGameData;
import com.jjg.game.slots.data.SlotsPlayerGameDataIndexedDTO;
import com.jjg.game.slots.game.angrybirds.data.AngryBirdsPlayerGameData;
import com.jjg.game.slots.game.angrybirds.data.AngryBirdsResultLib;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author lm
 * @date 2025/8/5 14:11
 */
@Document
public class AngryBirdsPlayerGameDataDTO extends SlotsPlayerGameDataIndexedDTO {
    //缓存免费的结果库
    protected AngryBirdsResultLib freeLib;
    //剩余的免费次数
    protected int remainFreeCount;
    //当前的免费游戏数组中的下标值
    protected int freeIndex;

    public AngryBirdsResultLib getFreeLib() {
        return freeLib;
    }

    public void setFreeLib(AngryBirdsResultLib freeLib) {
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
        if (t instanceof AngryBirdsPlayerGameData playerGameData) {
            int safeFreeIndex = Math.max(0, this.freeIndex);
            int safeRemainFreeCount = Math.max(0, this.remainFreeCount);
            playerGameData.setFreeIndex(new AtomicInteger(safeFreeIndex));
            playerGameData.setRemainFreeCount(new AtomicInteger(safeRemainFreeCount));
            playerGameData.setFreeLib(this.freeLib);
        }
        return t;
    }
}
