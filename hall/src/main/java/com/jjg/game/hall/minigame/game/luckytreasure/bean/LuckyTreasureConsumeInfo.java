package com.jjg.game.hall.minigame.game.luckytreasure.bean;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * 消耗详情
 */
@ProtobufMessage
@ProtoDesc("消耗详情")
public class LuckyTreasureConsumeInfo {

    /**
     * 道具id
     */
    @ProtoDesc("道具id")
    private int itemId;
    /**
     * 道具数量
     */
    @ProtoDesc("道具数量")
    private int count;

    public int getItemId() {
        return itemId;
    }

    public void setItemId(int itemId) {
        this.itemId = itemId;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public void setItemNum(int itemNum) {
        this.count = itemNum;
    }

}
