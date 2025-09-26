package com.jjg.game.hall.minigame.game.luckytreasure.message.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.constant.LuckyTreasureConstant;

/**
 * 请求领取夺宝奇兵道具
 */
@ProtobufMessage(
        messageType = MessageConst.MessageTypeDef.HALL_TYPE,
        cmd = LuckyTreasureConstant.Message.REQ_RECEIVE_LUCKY_TREASURE
)
@ProtoDesc("请求领取夺宝奇兵道具")
public class ReqReceiveLuckyTreasure extends AbstractMessage {

    /**
     * 期号
     */
    @ProtoDesc("期号")
    private long issueNumber;

    public long getIssueNumber() {
        return issueNumber;
    }

    public void setIssueNumber(long issueNumber) {
        this.issueNumber = issueNumber;
    }
}
