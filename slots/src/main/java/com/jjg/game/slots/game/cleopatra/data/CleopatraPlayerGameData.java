package com.jjg.game.slots.game.cleopatra.data;

import com.jjg.game.slots.data.SlotsPlayerGameData;
import org.springframework.beans.BeanUtils;

/**
 * @author 11
 * @date 2025/8/1 17:27
 */
public class CleopatraPlayerGameData extends SlotsPlayerGameData {

    public CleopatraPlayerGameDataDTO converToDto(){
        CleopatraPlayerGameDataDTO dto = new CleopatraPlayerGameDataDTO();
        BeanUtils.copyProperties(this,dto);
        dto.setPlayerId(this.playerId());
        return dto;
    }
}
