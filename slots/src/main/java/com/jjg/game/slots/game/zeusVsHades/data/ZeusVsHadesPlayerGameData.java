package com.jjg.game.slots.game.zeusVsHades.data;

import com.jjg.game.slots.data.SlotsPlayerGameData;
import com.jjg.game.slots.data.SlotsPlayerGameDataDTO;

/**
 * @author lihaocao
 * @date 2025/12/2 17:27
 */
public class ZeusVsHadesPlayerGameData extends SlotsPlayerGameData {

    //是否剩余免费次数
    private boolean isCount;


    public boolean getIsCount() {
        return isCount;
    }

    public void setIsCount(boolean isCount) {
       this.isCount = isCount;
    }

    @Override
    public <T extends SlotsPlayerGameDataDTO> T converToDto(Class<T> cla) throws Exception {
        T dto = super.converToDto(cla);
        if (dto instanceof ZeusVsHadesPlayerGameDataDTO gameDataDTO) {
            gameDataDTO.setFreeIndex(this.freeIndex == null ? 0 : this.freeIndex.get());
            gameDataDTO.setRemainFreeCount(this.remainFreeCount == null ? 0 : this.remainFreeCount.get());
            ZeusVsHadesResultLib freeLib = this.freeLib instanceof ZeusVsHadesResultLib lib ? lib : null;
            gameDataDTO.setFreeLib(freeLib);
            gameDataDTO.setIsCount(isCount);
        }
        return dto;
    }
}

