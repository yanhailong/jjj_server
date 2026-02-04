package com.jjg.game.slots.game.tigerbringsriches.data;

import com.jjg.game.slots.data.SlotsPlayerGameData;
import com.jjg.game.slots.data.SlotsPlayerGameDataDTO;
import com.jjg.game.slots.game.tigerbringsriches.dao.TigerBringsRichesPlayerGameDataDTO;

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

    @Override
    public <T extends SlotsPlayerGameDataDTO> T converToDto(Class<T> cla) throws Exception {
        T dto = super.converToDto(cla);
        if (dto instanceof TigerBringsRichesPlayerGameDataDTO gameDataDTO) {
            gameDataDTO.setCurrentRandomIndex(this.currentRandomIndex);
            gameDataDTO.setSpecialLib(this.specialLib);
        }
        return dto;
    }
}
