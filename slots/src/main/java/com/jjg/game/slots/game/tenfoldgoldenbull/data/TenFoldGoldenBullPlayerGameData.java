package com.jjg.game.slots.game.tenfoldgoldenbull.data;

import com.jjg.game.slots.data.SlotsPlayerGameData;
import com.jjg.game.slots.data.SlotsPlayerGameDataDTO;
import com.jjg.game.slots.game.tenfoldgoldenbull.dao.TenFoldGoldenBullPlayerGameDataDTO;
import com.jjg.game.slots.game.tenfoldgoldenbull.dao.TenFoldGoldenBullPlayerGameDataRoomDTO;

/**
 * @author lm
 * @date 2025/12/2 17:27
 */
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

    @Override
    public <T extends SlotsPlayerGameDataDTO> T converToDto(Class<T> cla) throws Exception {
        T dto = super.converToDto(cla);
        if (dto instanceof TenFoldGoldenBullPlayerGameDataDTO gameDataDTO) {
            gameDataDTO.setCurrentRandomIndex(this.currentRandomIndex);
            gameDataDTO.setLuckyBull(this.luckyBull);
        }
        if (dto instanceof TenFoldGoldenBullPlayerGameDataRoomDTO gameDataDTO) {
            gameDataDTO.setCurrentRandomIndex(this.currentRandomIndex);
            gameDataDTO.setLuckyBull(this.luckyBull);
        }
        return dto;
    }
}
