package com.jjg.game.slots.game.tigerbringsriches.dao;

import com.jjg.game.slots.data.SlotsPlayerGameData;
import com.jjg.game.slots.data.SlotsPlayerGameDataRoomDTO;
import com.jjg.game.slots.game.tigerbringsriches.data.TigerBringsRichesPlayerGameData;
import com.jjg.game.slots.game.tigerbringsriches.data.TigerBringsRichesResultLib;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * @author 11
 * @date 2025/8/5 14:11
 * 注意：新增字段请同步到该 Room DTO，避免房间模式丢字段。
*/
@Document
public class TigerBringsRichesPlayerGameDataRoomDTO extends SlotsPlayerGameDataRoomDTO {
    private int currentRandomIndex;
    private TigerBringsRichesResultLib specialLib;

    public int getCurrentRandomIndex() {
        return currentRandomIndex;
    }

    public void setCurrentRandomIndex(int currentRandomIndex) {
        this.currentRandomIndex = currentRandomIndex;
    }

    public TigerBringsRichesResultLib getSpecialLib() {
        return specialLib;
    }

    public void setSpecialLib(TigerBringsRichesResultLib specialLib) {
        this.specialLib = specialLib;
    }

    @Override
    public <T extends SlotsPlayerGameData> T converToGameData(Class<T> cla) throws Exception {
        T t = super.converToGameData(cla);
        if (t instanceof TigerBringsRichesPlayerGameData playerGameData) {
            playerGameData.setCurrentRandomIndex(this.currentRandomIndex);
            playerGameData.setSpecialLib(this.specialLib);
        }
        return t;
    }
}
