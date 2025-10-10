package com.jjg.game.core.task.pb;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * 任务奖励
 */
@ProtobufMessage
@ProtoDesc("任务奖励")
public class TaskReward {

    /**
     * 道具id
     */
    @ProtoDesc("道具id")
    private int itemId;

    /**
     * 道具数量
     */
    @ProtoDesc("道具数量")
    private int itemNum;

    public int getItemId() {
        return itemId;
    }

    public void setItemId(int itemId) {
        this.itemId = itemId;
    }

    public int getItemNum() {
        return itemNum;
    }

    public void setItemNum(int itemNum) {
        this.itemNum = itemNum;
    }
}
