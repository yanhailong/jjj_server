package com.jjg.game.slots.game.mahjiongwin.data;

import com.jjg.game.slots.data.SlotsPlayerGameData;
import com.jjg.game.slots.data.SlotsPlayerGameDataDTO;

/**
 * @author 11
 * @date 2025/8/1 17:27
 */
public class MahjiongWinPlayerGameData extends SlotsPlayerGameData {
    @Override
    public <T extends SlotsPlayerGameDataDTO> T converToDto(Class<T> cla) throws Exception {
        T dto = super.converToDto(cla);
        if (dto instanceof MahjiongWinPlayerGameDataDTO gameDataDTO) {
            gameDataDTO.setFreeIndex(this.freeIndex == null ? 0 : this.freeIndex.get());
            gameDataDTO.setRemainFreeCount(this.remainFreeCount == null ? 0 : this.remainFreeCount.get());
            MahjiongWinResultLib freeLib = this.freeLib instanceof MahjiongWinResultLib lib ? lib : null;
            gameDataDTO.setFreeLib(freeLib);
        }
        return dto;
    }
}
