package com.jjg.game.slots.game.wealthgod.data;

import com.jjg.game.slots.data.SlotsPlayerGameData;
import org.springframework.beans.BeanUtils;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * 财神
 */
@Document
public class WealthGodPlayerGameData extends SlotsPlayerGameData {

    public WealthGodPlayerGameDataDTO convertToDto(){
        WealthGodPlayerGameDataDTO dto = new WealthGodPlayerGameDataDTO();
        BeanUtils.copyProperties(this,dto);
        dto.setPlayerId(this.getPlayerId());
        return dto;
    }

}
