package com.jjg.game.slots.game.hulk.data;

import com.jjg.game.slots.data.SlotsPlayerGameData;
import com.jjg.game.slots.data.SlotsPlayerGameDataDTO;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * @author 11
 * @date 2026/1/15
 */
@Document
public class HulkPlayerGameData extends SlotsPlayerGameData {
    @Override
    public <T extends SlotsPlayerGameDataDTO> T converToDto(Class<T> cla) throws Exception {
        T dto = super.converToDto(cla);
        if (dto instanceof HulkPlayerGameDataDTO gameDataDTO) {
            gameDataDTO.setFreeIndex(this.freeIndex == null ? 0 : this.freeIndex.get());
            gameDataDTO.setRemainFreeCount(this.remainFreeCount == null ? 0 : this.remainFreeCount.get());
            HulkResultLib freeLib = this.freeLib instanceof HulkResultLib lib ? lib : null;
            gameDataDTO.setFreeLib(freeLib);
        } else if (dto instanceof HulkPlayerGameDataRoomDTO gameDataDTO) {
            gameDataDTO.setFreeIndex(this.freeIndex == null ? 0 : this.freeIndex.get());
            gameDataDTO.setRemainFreeCount(this.remainFreeCount == null ? 0 : this.remainFreeCount.get());
            HulkResultLib freeLib = this.freeLib instanceof HulkResultLib lib ? lib : null;
            gameDataDTO.setFreeLib(freeLib);
        }
        return dto;
    }
}
