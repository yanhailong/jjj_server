package com.jjg.game.slots.game.wealthgod.data;

import com.jjg.game.slots.data.SlotsPlayerGameData;
import org.springframework.beans.BeanUtils;

/**
 * 财神
 */
public class WealthGodPlayerGameData extends SlotsPlayerGameData {

    public WealthGodPlayerGameDataDTO convertToDto(){
        WealthGodPlayerGameDataDTO dto = new WealthGodPlayerGameDataDTO();
        BeanUtils.copyProperties(this,dto);
        dto.setPlayerId(this.playerId());
        return dto;
    }

}
