package com.jjg.game.slots.game.candyparty.data;

import com.jjg.game.slots.data.SlotsPlayerGameData;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * @author lm
 * @date 2025/12/2 17:27
 */
@Document
public class CandyPartyPlayerGameData extends SlotsPlayerGameData {
    //收集图标数量
    private int collectedIconNum;
    //当前层数
    private int layerNumber = 1;

    public int getCollectedIconNum() {
        return collectedIconNum;
    }

    public void setCollectedIconNum(int collectedIconNum) {
        this.collectedIconNum = collectedIconNum;
    }

    public int getLayerNumber() {
        return layerNumber;
    }

    public void setLayerNumber(int layerNumber) {
        this.layerNumber = layerNumber;
    }
}
