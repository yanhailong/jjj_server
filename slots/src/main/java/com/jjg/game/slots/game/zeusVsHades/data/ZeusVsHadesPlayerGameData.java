package com.jjg.game.slots.game.zeusVsHades.data;

import com.jjg.game.slots.data.SlotsPlayerGameData;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * @author lihaocao
 * @date 2025/12/2 17:27
 */
@Document
public class ZeusVsHadesPlayerGameData extends SlotsPlayerGameData {

    //是否剩余免费次数
    private boolean isCount;


    public boolean getIsCount() {
        return isCount;
    }

    public void setIsCount(boolean isCount) {
       this.isCount = isCount;
    }
}

