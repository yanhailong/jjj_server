package com.jjg.game.slots.game.thor.data;

import com.jjg.game.slots.data.SlotsPlayerGameDataIndexedDTO;
import com.jjg.game.slots.data.SlotsPlayerGameData;
import com.jjg.game.slots.data.SlotsPlayerGameDataDTO;
import com.jjg.game.slots.game.moneyrabbit.data.MoneyRabbitPlayerGameData;
import com.jjg.game.slots.game.moneyrabbit.data.MoneyRabbitResultLib;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author 11
 * @date 2025/12/1 18:01
 */
@Document
public class ThorPlayerGameDataDTO extends SlotsPlayerGameDataIndexedDTO {
    //缓存免费的结果库
    private ThorResultLib freeLib;
    //剩余的免费次数
    private int remainFreeCount;
    //当前的免费游戏数组中的下标值
    private int freeIndex;

    private boolean isFreeStart;

    public boolean isFreeStart() {
        return isFreeStart;
    }

    public void setFreeStart(boolean freeStart) {
        isFreeStart = freeStart;
    }

    public ThorResultLib getFreeLib() {
        return freeLib;
    }

    public void setFreeLib(ThorResultLib freeLib) {
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
        if (t instanceof ThorPlayerGameData playerGameData) {
            int safeFreeIndex = Math.max(0, this.freeIndex);
            int safeRemainFreeCount = Math.max(0, this.remainFreeCount);
            playerGameData.setFreeIndex(new AtomicInteger(safeFreeIndex));
            playerGameData.setRemainFreeCount(new AtomicInteger(safeRemainFreeCount));
            playerGameData.setFreeLib(this.freeLib);
            playerGameData.setFreeStart(this.isFreeStart);
        }
        return t;
    }
}
