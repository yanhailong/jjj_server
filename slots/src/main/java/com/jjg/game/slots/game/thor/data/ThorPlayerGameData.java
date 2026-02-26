package com.jjg.game.slots.game.thor.data;

import com.jjg.game.slots.data.SlotsPlayerGameData;
import com.jjg.game.slots.data.SlotsPlayerGameDataDTO;

/**
 * @author 11
 * @date 2025/12/1 18:01
 */
public class ThorPlayerGameData extends SlotsPlayerGameData {
    private boolean isFreeStart;

    public boolean isFreeStart() {
        return isFreeStart;
    }

    public void setFreeStart(boolean freeStart) {
        isFreeStart = freeStart;
    }
    @Override
    public <T extends SlotsPlayerGameDataDTO> T converToDto(Class<T> cla) throws Exception {
        T dto = super.converToDto(cla);
        if (dto instanceof ThorPlayerGameDataDTO gameDataDTO) {
            gameDataDTO.setFreeIndex(this.freeIndex == null ? 0 : this.freeIndex.get());
            gameDataDTO.setRemainFreeCount(this.remainFreeCount == null ? 0 : this.remainFreeCount.get());
            ThorResultLib freeLib = this.freeLib instanceof ThorResultLib lib ? lib : null;
            gameDataDTO.setFreeLib(freeLib);
        }
        if (dto instanceof ThorPlayerGameDataRoomDTO gameDataDTO) {
            gameDataDTO.setFreeIndex(this.freeIndex == null ? 0 : this.freeIndex.get());
            gameDataDTO.setRemainFreeCount(this.remainFreeCount == null ? 0 : this.remainFreeCount.get());
            ThorResultLib freeLib = this.freeLib instanceof ThorResultLib lib ? lib : null;
            gameDataDTO.setFreeLib(freeLib);
        }
        return dto;
    }
}
