package com.jjg.game.slots.game.pegasusunbridle.data;

import com.jjg.game.slots.data.SlotsPlayerGameData;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * @author lm
 * @date 2025/12/2 17:27
 */
@Document
public class PegasusUnbridlePlayerGameData extends SlotsPlayerGameData {
    private int currentRandomIndex;
    private PegasusUnbridleResultLib fuMa;

    public PegasusUnbridleResultLib getFuMa() {
        return fuMa;
    }

    public void setFuMa(PegasusUnbridleResultLib fuMa) {
        this.fuMa = fuMa;
    }

    public int getCurrentRandomIndex() {
        return currentRandomIndex;
    }

    public void setCurrentRandomIndex(int currentRandomIndex) {
        this.currentRandomIndex = currentRandomIndex;
    }
}
