package com.jjg.game.slots.game.tenfoldgoldenbull.data;

import com.jjg.game.slots.data.SlotsPlayerGameData;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * @author lm
 * @date 2025/12/2 17:27
 */
@Document
public class TenFoldGoldenBullPlayerGameData extends SlotsPlayerGameData {
    private int currentRandomIndex;
    private TenFoldGoldenBullResultLib luckyBull;

    public TenFoldGoldenBullResultLib getLuckyBull() {
        return luckyBull;
    }

    public void setLuckyBull(TenFoldGoldenBullResultLib luckyBull) {
        this.luckyBull = luckyBull;
    }

    public int getCurrentRandomIndex() {
        return currentRandomIndex;
    }

    public void setCurrentRandomIndex(int currentRandomIndex) {
        this.currentRandomIndex = currentRandomIndex;
    }
}
