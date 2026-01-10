package com.jjg.game.slots.game.tigerbringsriches.data;

import com.jjg.game.slots.data.SlotsPlayerGameData;

/**
 * @author lm
 * @date 2025/12/2 17:27
 */
public class TigerBringsRichesPlayerGameData extends SlotsPlayerGameData {
    private int currentRandomIndex;
    private TigerBringsRichesResultLib specialLib;

    public TigerBringsRichesResultLib getSpecialLib() {
        return specialLib;
    }

    public void setSpecialLib(TigerBringsRichesResultLib specialLib) {
        this.specialLib = specialLib;
    }

    public int getCurrentRandomIndex() {
        return currentRandomIndex;
    }

    public void setCurrentRandomIndex(int currentRandomIndex) {
        this.currentRandomIndex = currentRandomIndex;
    }
}
