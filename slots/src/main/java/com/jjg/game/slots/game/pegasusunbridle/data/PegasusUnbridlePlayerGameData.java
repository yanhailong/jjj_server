package com.jjg.game.slots.game.pegasusunbridle.data;

import com.jjg.game.slots.data.SlotsPlayerGameData;
import com.jjg.game.slots.data.SlotsPlayerGameDataDTO;
import com.jjg.game.slots.game.pegasusunbridle.dao.PegasusUnbridlePlayerGameDataDTO;
import com.jjg.game.slots.game.pegasusunbridle.dao.PegasusUnbridlePlayerGameDataRoomDTO;
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

    @Override
    public <T extends SlotsPlayerGameDataDTO> T converToDto(Class<T> cla) throws Exception {
        T t = super.converToDto(cla);
        if (t instanceof PegasusUnbridlePlayerGameDataDTO data) {
            data.setCurrentRandomIndex(this.currentRandomIndex);
            data.setFuMa(this.fuMa);
        }
        if (t instanceof PegasusUnbridlePlayerGameDataRoomDTO data) {
            data.setCurrentRandomIndex(this.currentRandomIndex);
            data.setFuMa(this.fuMa);
        }
        return t;
    }
}
