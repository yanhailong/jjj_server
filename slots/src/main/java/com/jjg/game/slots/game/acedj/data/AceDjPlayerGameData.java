package com.jjg.game.slots.game.acedj.data;

import com.jjg.game.slots.data.SlotsPlayerGameData;
import com.jjg.game.slots.data.SlotsPlayerGameDataDTO;

/**
 * @author lihaocao
 * @date 2025/12/2 17:27
 */
public class AceDjPlayerGameData extends SlotsPlayerGameData {
    @Override
    public <T extends SlotsPlayerGameDataDTO> T converToDto(Class<T> cla) throws Exception {
        T dto = super.converToDto(cla);
        if (dto instanceof AceDjPlayerGameDataDTO gameDataDTO) {
            gameDataDTO.setFreeIndex(this.freeIndex == null ? 0 : this.freeIndex.get());
            gameDataDTO.setRemainFreeCount(this.remainFreeCount == null ? 0 : this.remainFreeCount.get());
            AceDjResultLib freeLib = this.freeLib instanceof AceDjResultLib lib ? lib : null;
            gameDataDTO.setFreeLib(freeLib);
        }
        return dto;
    }
}
