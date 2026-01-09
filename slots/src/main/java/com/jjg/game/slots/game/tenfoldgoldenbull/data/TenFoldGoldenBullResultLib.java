package com.jjg.game.slots.game.tenfoldgoldenbull.data;

import com.jjg.game.slots.data.SlotsResultLib;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lm
 * @date 2025/12/2 17:30
 */
@Document
public class TenFoldGoldenBullResultLib extends SlotsResultLib<TenFoldGoldenBullAwardLineInfo> {
    private int jackpotId;
    private List<TenFoldGoldenBullResultLib> randomResult;

    public int getJackpotId() {
        return jackpotId;
    }

    public void setJackpotId(int jackpotId) {
        this.jackpotId = jackpotId;
    }

    public List<TenFoldGoldenBullResultLib> getRandomResult() {
        return randomResult;
    }

    public void setRandomResult(List<TenFoldGoldenBullResultLib> randomResult) {
        this.randomResult = randomResult;
    }

    public void addRandomResult(TenFoldGoldenBullResultLib result){
        if (randomResult == null) {
            randomResult = new ArrayList<>();
        }
        randomResult.add(result);
    }

}

