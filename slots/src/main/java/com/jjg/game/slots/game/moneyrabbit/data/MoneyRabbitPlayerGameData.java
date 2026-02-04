package com.jjg.game.slots.game.moneyrabbit.data;

import com.jjg.game.slots.data.SlotsPlayerGameData;
import com.jjg.game.slots.data.SlotsPlayerGameDataDTO;

public class MoneyRabbitPlayerGameData extends SlotsPlayerGameData {
    @Override
    public <T extends SlotsPlayerGameDataDTO> T converToDto(Class<T> cla) throws Exception {
        T dto = super.converToDto(cla);
        if (dto instanceof MoneyRabbitPlayerGameDataDTO gameDataDTO) {
            gameDataDTO.setFreeIndex(this.freeIndex == null ? 0 : this.freeIndex.get());
            gameDataDTO.setRemainFreeCount(this.remainFreeCount == null ? 0 : this.remainFreeCount.get());
            MoneyRabbitResultLib freeLib = this.freeLib instanceof MoneyRabbitResultLib lib ? lib : null;
            gameDataDTO.setFreeLib(freeLib);
        }
        return dto;
    }

}
