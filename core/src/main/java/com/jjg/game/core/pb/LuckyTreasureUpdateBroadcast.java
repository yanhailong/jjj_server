package com.jjg.game.core.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * 夺宝奇兵库存变化广播
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.HALL_TYPE, cmd = MessageConst.ToServer.NOTIFY_LUCKY_TREASURE_UPDATE_STOCK, resp = true)
public class LuckyTreasureUpdateBroadcast extends AbstractMessage {

    /**
     * 库存变化的期号
     */
    private long issueNumber;

    public long getIssueNumber() {
        return issueNumber;
    }

    public void setIssueNumber(long issueNumber) {
        this.issueNumber = issueNumber;
    }
}
