package com.jjg.game.slots.game.angrybirds.data;

import com.jjg.game.slots.data.SlotsPlayerGameData;
import com.jjg.game.slots.data.SlotsPlayerGameDataDTO;
import com.jjg.game.slots.game.angrybirds.dao.AngryBirdsPlayerGameDataDTO;
import com.jjg.game.slots.game.angrybirds.dao.AngryBirdsPlayerGameDataRoomDTO;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * @author lm
 * @date 2025/12/2 17:27
 */
@Document
public class AngryBirdsPlayerGameData extends SlotsPlayerGameData {

    @Override
    public <T extends SlotsPlayerGameDataDTO> T converToDto(Class<T> cla) throws Exception {
        T dto = super.converToDto(cla);
        if (dto instanceof AngryBirdsPlayerGameDataDTO gameDataDTO) {
            gameDataDTO.setFreeIndex(this.freeIndex == null ? 0 : this.freeIndex.get());
            gameDataDTO.setRemainFreeCount(this.remainFreeCount == null ? 0 : this.remainFreeCount.get());
            AngryBirdsResultLib freeLib = this.freeLib instanceof AngryBirdsResultLib lib ? lib : null;
            gameDataDTO.setFreeLib(freeLib);
        }
        if (dto instanceof AngryBirdsPlayerGameDataRoomDTO gameDataDTO) {
            gameDataDTO.setFreeIndex(this.freeIndex == null ? 0 : this.freeIndex.get());
            gameDataDTO.setRemainFreeCount(this.remainFreeCount == null ? 0 : this.remainFreeCount.get());
            AngryBirdsResultLib freeLib = this.freeLib instanceof AngryBirdsResultLib lib ? lib : null;
            gameDataDTO.setFreeLib(freeLib);
        }
        return dto;
    }
}
