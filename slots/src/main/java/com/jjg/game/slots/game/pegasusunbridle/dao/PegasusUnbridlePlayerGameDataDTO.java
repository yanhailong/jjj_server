package com.jjg.game.slots.game.pegasusunbridle.dao;

import com.jjg.game.slots.data.SlotsPlayerGameData;
import com.jjg.game.slots.data.SlotsPlayerGameDataDTO;
import com.jjg.game.slots.game.pegasusunbridle.data.PegasusUnbridlePlayerGameData;
import com.jjg.game.slots.game.pegasusunbridle.data.PegasusUnbridleResultLib;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * @author 11
 * @date 2025/8/5 14:11
 */
@Document
public class PegasusUnbridlePlayerGameDataDTO extends SlotsPlayerGameDataDTO {
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

    @Override
    public <T extends SlotsPlayerGameData> T converToGameData(Class<T> cla) throws Exception {
        T t = super.converToGameData(cla);
        if(t instanceof PegasusUnbridlePlayerGameData gameData){
            gameData.setCurrentRandomIndex(this.currentRandomIndex);
            gameData.setFuMa(this.fuMa);
        }
        return t;
    }
}
