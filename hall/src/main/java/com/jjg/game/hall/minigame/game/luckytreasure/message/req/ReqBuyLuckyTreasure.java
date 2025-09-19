package com.jjg.game.hall.minigame.game.luckytreasure.message.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.hall.minigame.game.luckytreasure.constant.LuckyTreasureConstant;

/**
 * 请求购买夺宝奇兵道具
 */
@ProtobufMessage(
        messageType = MessageConst.MessageTypeDef.HALL_TYPE,
        cmd = LuckyTreasureConstant.Message.REQ_BUY_LUCKY_TREASURE
)
@ProtoDesc("请求购买夺宝奇兵道具")
public class ReqBuyLuckyTreasure extends AbstractMessage {

    /**
     * 期号
     */
    @ProtoDesc("期号")
    private long issueNumber;

    /**
     * 购买数量
     */
    @ProtoDesc("购买数量")
    private int count;

    public long getIssueNumber() {
        return issueNumber;
    }

    public void setIssueNumber(long issueNumber) {
        this.issueNumber = issueNumber;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
