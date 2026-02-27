package com.jjg.game.slots.game.wolfmoon.data;

import com.jjg.game.slots.data.SlotsPlayerGameData;
import com.jjg.game.slots.data.SlotsPlayerGameDataDTO;

public class WolfMoonPlayerGameData extends SlotsPlayerGameData {
    private int freeMultiplyValue;

    public int getFreeMultiplyValue() {
        return freeMultiplyValue;
    }

    public void setFreeMultiplyValue(int freeMultiplyValue) {
        this.freeMultiplyValue = freeMultiplyValue;
    }

    @Override
    public <T extends SlotsPlayerGameDataDTO> T converToDto(Class<T> cla) throws Exception {
        T dto = super.converToDto(cla);
        if (dto instanceof WolfMoonPlayerGameDataDTO dataDTO) {
            dataDTO.setFreeIndex(this.freeIndex == null ? 0 : this.freeIndex.get());
            dataDTO.setRemainFreeCount(this.remainFreeCount == null ? 0 : this.remainFreeCount.get());
            dataDTO.setFreeMultiplyValue(this.freeMultiplyValue);
            WolfMoonResultLib freeLib = this.freeLib instanceof WolfMoonResultLib lib ? lib : null;
            dataDTO.setFreeLib(freeLib);
        }
        if (dto instanceof WolfMoonPlayerGameDataRoomDTO dataDTO) {
            dataDTO.setFreeIndex(this.freeIndex == null ? 0 : this.freeIndex.get());
            dataDTO.setRemainFreeCount(this.remainFreeCount == null ? 0 : this.remainFreeCount.get());
            dataDTO.setFreeMultiplyValue(this.freeMultiplyValue);
            WolfMoonResultLib freeLib = this.freeLib instanceof WolfMoonResultLib lib ? lib : null;
            dataDTO.setFreeLib(freeLib);
        }
        return dto;
    }
}
