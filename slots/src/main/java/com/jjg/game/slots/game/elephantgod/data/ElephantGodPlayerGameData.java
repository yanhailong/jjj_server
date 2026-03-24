package com.jjg.game.slots.game.elephantgod.data;

import com.jjg.game.slots.data.SlotsPlayerGameData;
import com.jjg.game.slots.data.SlotsPlayerGameDataDTO;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
public class ElephantGodPlayerGameData extends SlotsPlayerGameData {
    @Override
    public <T extends SlotsPlayerGameDataDTO> T converToDto(Class<T> cla) throws Exception {
        T dto = super.converToDto(cla);
        if (dto instanceof ElephantGodPlayerGameDataDTO gameDataDTO) {
            gameDataDTO.setFreeIndex(this.freeIndex == null ? 0 : this.freeIndex.get());
            gameDataDTO.setRemainFreeCount(this.remainFreeCount == null ? 0 : this.remainFreeCount.get());
            ElephantGodResultLib freeLib = this.freeLib instanceof ElephantGodResultLib lib ? lib : null;
            gameDataDTO.setFreeLib(freeLib);
        }
        if (dto instanceof ElephantGodPlayerGameDataRoomDTO gameDataDTO) {
            gameDataDTO.setFreeIndex(this.freeIndex == null ? 0 : this.freeIndex.get());
            gameDataDTO.setRemainFreeCount(this.remainFreeCount == null ? 0 : this.remainFreeCount.get());
            ElephantGodResultLib freeLib = this.freeLib instanceof ElephantGodResultLib lib ? lib : null;
            gameDataDTO.setFreeLib(freeLib);
        }
        return dto;
    }

}
