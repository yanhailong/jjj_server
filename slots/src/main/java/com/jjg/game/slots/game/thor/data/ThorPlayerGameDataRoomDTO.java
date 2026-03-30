package com.jjg.game.slots.game.thor.data;

import com.jjg.game.slots.data.SlotsPlayerGameData;
import com.jjg.game.slots.data.SlotsPlayerGameDataRoomDTO;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thor房间模式玩家数据
 * @author lm
 * @date 2026/2/4
 * 注意：新增字段请同步到该 Room DTO，避免房间模式丢字段。
*/
@Document
public class ThorPlayerGameDataRoomDTO extends SlotsPlayerGameDataRoomDTO {
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
