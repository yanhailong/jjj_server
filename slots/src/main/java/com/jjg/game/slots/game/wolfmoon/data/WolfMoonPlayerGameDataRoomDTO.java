package com.jjg.game.slots.game.wolfmoon.data;

import com.jjg.game.slots.data.SlotsPlayerGameData;
import com.jjg.game.slots.data.SlotsPlayerGameDataRoomDTO;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.concurrent.atomic.AtomicInteger;

@Document
public class WolfMoonPlayerGameDataRoomDTO extends SlotsPlayerGameDataRoomDTO {
    private WolfMoonResultLib freeLib;
    private int remainFreeCount;
    private int freeIndex;
    private int freeMultiplyValue;

    public WolfMoonResultLib getFreeLib() {
        return freeLib;
    }

    public void setFreeLib(WolfMoonResultLib freeLib) {
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

    public int getFreeMultiplyValue() {
        return freeMultiplyValue;
    }

    public void setFreeMultiplyValue(int freeMultiplyValue) {
        this.freeMultiplyValue = freeMultiplyValue;
    }

    @Override
    public <T extends SlotsPlayerGameData> T converToGameData(Class<T> cla) throws Exception {
        T t = super.converToGameData(cla);
        if (t instanceof WolfMoonPlayerGameData data) {
            data.setFreeIndex(new AtomicInteger(Math.max(0, this.freeIndex)));
            data.setRemainFreeCount(new AtomicInteger(Math.max(0, this.remainFreeCount)));
            data.setFreeLib(this.freeLib);
            data.setFreeMultiplyValue(Math.max(0, this.freeMultiplyValue));
        }
        return t;
    }
}
