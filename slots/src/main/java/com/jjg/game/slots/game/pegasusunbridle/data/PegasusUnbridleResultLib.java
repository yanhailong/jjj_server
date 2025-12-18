package com.jjg.game.slots.game.pegasusunbridle.data;

import com.jjg.game.slots.data.SlotsResultLib;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * @author lm
 * @date 2025/12/2 17:30
 */
@Document
public class PegasusUnbridleResultLib extends SlotsResultLib<PegasusUnbridleAwardLineInfo> {
    private int jackpotId;

    public int getJackpotId() {
        return jackpotId;
    }

    public void setJackpotId(int jackpotId) {
        this.jackpotId = jackpotId;
    }
}
