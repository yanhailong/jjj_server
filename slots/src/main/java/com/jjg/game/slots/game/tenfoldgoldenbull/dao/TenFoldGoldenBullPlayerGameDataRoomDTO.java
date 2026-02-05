package com.jjg.game.slots.game.tenfoldgoldenbull.dao;

import com.jjg.game.slots.data.SlotsPlayerGameData;
import com.jjg.game.slots.data.SlotsPlayerGameDataRoomDTO;
import com.jjg.game.slots.game.tenfoldgoldenbull.data.TenFoldGoldenBullPlayerGameData;
import com.jjg.game.slots.game.tenfoldgoldenbull.data.TenFoldGoldenBullResultLib;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * @author 11
 * @date 2025/8/5 14:11
 * 注意：新增字段请同步到该 Room DTO，避免房间模式丢字段。
*/
@Document
public class TenFoldGoldenBullPlayerGameDataRoomDTO extends SlotsPlayerGameDataRoomDTO {
    private int currentRandomIndex;
    private TenFoldGoldenBullResultLib luckyBull;

    public int getCurrentRandomIndex() {
        return currentRandomIndex;
    }

    public void setCurrentRandomIndex(int currentRandomIndex) {
        this.currentRandomIndex = currentRandomIndex;
    }

    public TenFoldGoldenBullResultLib getLuckyBull() {
        return luckyBull;
    }

    public void setLuckyBull(TenFoldGoldenBullResultLib luckyBull) {
        this.luckyBull = luckyBull;
    }

    @Override
    public <T extends SlotsPlayerGameData> T converToGameData(Class<T> cla) throws Exception {
        T t = super.converToGameData(cla);
        if (t instanceof TenFoldGoldenBullPlayerGameData playerGameData) {
            playerGameData.setCurrentRandomIndex(this.currentRandomIndex);
            playerGameData.setLuckyBull(this.luckyBull);
        }
        return t;
    }
}
