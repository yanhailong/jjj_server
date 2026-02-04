package com.jjg.game.slots.game.steamAge.data;

import com.jjg.game.slots.data.SlotsPlayerGameData;
import com.jjg.game.slots.data.SlotsPlayerGameDataDTO;

/**
 * @author lihaocao
 * @date 2025/12/2 17:27
 */
public class SteamAgePlayerGameData extends SlotsPlayerGameData {
    @Override
    public <T extends SlotsPlayerGameDataDTO> T converToDto(Class<T> cla) throws Exception {
        T dto = super.converToDto(cla);
        if (dto instanceof SteamAgePlayerGameDataDTO gameDataDTO) {
            gameDataDTO.setFreeIndex(this.freeIndex == null ? 0 : this.freeIndex.get());
            gameDataDTO.setRemainFreeCount(this.remainFreeCount == null ? 0 : this.remainFreeCount.get());
            SteamAgeResultLib freeLib = this.freeLib instanceof SteamAgeResultLib lib ? lib : null;
            gameDataDTO.setFreeLib(freeLib);
        }
        return dto;
    }
}
