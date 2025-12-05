package com.jjg.game.slots.game.thor.data;

import com.jjg.game.slots.data.SlotsResultLib;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * @author 11
 * @date 2025/12/1 18:01
 */
@Document
public class ThorResultLib extends SlotsResultLib<ThorAwardLineInfo> {
    //本次触发的jackpotId
    private int jackpotId;

    public int getJackpotId() {
        return jackpotId;
    }

    public void setJackpotId(int jackpotId) {
        this.jackpotId = jackpotId;
    }
}
