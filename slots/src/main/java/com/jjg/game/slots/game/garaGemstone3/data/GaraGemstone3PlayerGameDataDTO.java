package com.jjg.game.slots.game.garaGemstone3.data;

import com.jjg.game.slots.data.SlotsPlayerGameData;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.concurrent.atomic.AtomicInteger;

@Document
public class GaraGemstone3PlayerGameDataDTO extends SlotsPlayerGameDataDTO {
    //缓存免费的结果库
    private GaraGemstone3ResultLib freeLib;
    //剩余的免费次数
    private int remainFreeCount;
    //当前的免费游戏数组中的下标值
    private int freeIndex;

    public GaraGemstone3ResultLib getFreeLib() {
        return freeLib;
    }

    public void setFreeLib(GaraGemstone3ResultLib freeLib) {
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
        if (t instanceof GaraGemstone3PlayerGameData playerGameData) {
            int safeFreeIndex = Math.max(0, this.freeIndex);
            int safeRemainFreeCount = Math.max(0, this.remainFreeCount);
            playerGameData.setFreeIndex(new AtomicInteger(safeFreeIndex));
            playerGameData.setRemainFreeCount(new AtomicInteger(safeRemainFreeCount));
            playerGameData.setFreeLib(this.freeLib);
        }
        return t;
    }
}
