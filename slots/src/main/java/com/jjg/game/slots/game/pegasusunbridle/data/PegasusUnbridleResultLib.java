package com.jjg.game.slots.game.pegasusunbridle.data;

import com.alibaba.fastjson.JSONObject;
import com.jjg.game.slots.data.SlotsResultLib;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lm
 * @date 2025/12/2 17:30
 */
@Document
public class PegasusUnbridleResultLib extends SlotsResultLib<PegasusUnbridleAwardLineInfo> {
    private int jackpotId;
    private List<PegasusUnbridleResultLib> randomResult;

    public int getJackpotId() {
        return jackpotId;
    }

    public void setJackpotId(int jackpotId) {
        this.jackpotId = jackpotId;
    }

    public List<PegasusUnbridleResultLib> getRandomResult() {
        return randomResult;
    }

    public void setRandomResult(List<PegasusUnbridleResultLib> randomResult) {
        this.randomResult = randomResult;
    }

    public void addRandomResult(PegasusUnbridleResultLib result){
        if (randomResult == null) {
            randomResult = new ArrayList<>();
        }
        randomResult.add(result);
    }

}

