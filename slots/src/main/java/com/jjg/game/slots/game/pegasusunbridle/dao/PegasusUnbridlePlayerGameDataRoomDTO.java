package com.jjg.game.slots.game.pegasusunbridle.dao;

import com.jjg.game.slots.data.SlotsPlayerGameDataRoomDTO;
import com.jjg.game.slots.game.pegasusunbridle.data.PegasusUnbridleResultLib;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * PegasusUnbridle房间模式玩家数据
 *
 * @author lm
 * @date 2026/2/4
 * 注意：新增字段请同步到该 Room DTO，避免房间模式丢字段。
*/
@Document
public class PegasusUnbridlePlayerGameDataRoomDTO extends SlotsPlayerGameDataRoomDTO {
    private int currentRandomIndex;
    private PegasusUnbridleResultLib fuMa;

    public int getCurrentRandomIndex() {
        return currentRandomIndex;
    }

    public void setCurrentRandomIndex(int currentRandomIndex) {
        this.currentRandomIndex = currentRandomIndex;
    }

    public PegasusUnbridleResultLib getFuMa() {
        return fuMa;
    }

    public void setFuMa(PegasusUnbridleResultLib fuMa) {
        this.fuMa = fuMa;
    }
}
