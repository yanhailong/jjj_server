package com.jjg.game.slots.game.captainjack.dao;

import com.jjg.game.slots.data.SlotsPlayerGameData;
import com.jjg.game.slots.data.SlotsPlayerGameDataDTO;
import com.jjg.game.slots.game.captainjack.data.CaptainJackPlayerGameData;
import com.jjg.game.slots.game.captainjack.data.CaptainJackResultLib;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author 11
 * @date 2025/8/5 14:11
 */
public class CaptainJackPlayerGameDataDTO extends SlotsPlayerGameDataDTO {
    //寻宝次数
    private int alreadyDigCount;
    //当前依赖的寻宝libId
    private CaptainJackResultLib resultLib;
    //缓存免费的结果库
    protected CaptainJackResultLib freeLib;
    //剩余的免费次数
    protected int remainFreeCount;
    //当前的免费游戏数组中的下标值
    protected int freeIndex;

    public int getAlreadyDigCount() {
        return alreadyDigCount;
    }

    public void setAlreadyDigCount(int alreadyDigCount) {
        this.alreadyDigCount = alreadyDigCount;
    }

    public CaptainJackResultLib getResultLib() {
        return resultLib;
    }

    public void setResultLib(CaptainJackResultLib resultLib) {
        this.resultLib = resultLib;
    }

    public CaptainJackResultLib getFreeLib() {
        return freeLib;
    }

    public void setFreeLib(CaptainJackResultLib freeLib) {
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
        if (t instanceof CaptainJackPlayerGameData playerGameData) {
            int safeAlreadyDigCount = Math.max(0, this.alreadyDigCount);
            int safeFreeIndex = Math.max(0, this.freeIndex);
            int safeRemainFreeCount = Math.max(0, this.remainFreeCount);
            playerGameData.setAlreadyDigCount(new AtomicInteger(safeAlreadyDigCount));
            playerGameData.setFreeIndex(new AtomicInteger(safeFreeIndex));
            playerGameData.setRemainFreeCount(new AtomicInteger(safeRemainFreeCount));
            playerGameData.setFreeLib(this.freeLib);
            playerGameData.setResultLib(this.resultLib);
        }
        return t;
    }
}
