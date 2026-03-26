package com.jjg.game.slots.game.wolfmoon.data;

import com.jjg.game.slots.data.SlotsPlayerGameData;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * @author 11
 * @date 2025/2/27 15:33
 */
@Document
public class WolfMoonPlayerGameData extends SlotsPlayerGameData {

    /**
     * 免费游戏类型
     * 1-高赔付符号 2-固定堆叠百搭符号 3-递增奖励倍数
     */
    private int freeGameType;


    public int getFreeGameType() {
        return freeGameType;
    }

    public void setFreeGameType(int freeGameType) {
        this.freeGameType = freeGameType;
    }


}
