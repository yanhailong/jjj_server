package com.jjg.game.hall.minigame.game.luckytreasure.message.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.hall.minigame.game.luckytreasure.constant.LuckyTreasureConstant;

/**
 * 购买夺宝奇兵道具回复
 */
@ProtobufMessage(
        messageType = MessageConst.MessageTypeDef.HALL_TYPE,
        cmd = LuckyTreasureConstant.Message.RES_BUY_LUCKY_TREASURE,
        resp = true
)
@ProtoDesc("购买夺宝奇兵道具回复")
public class ResBuyLuckyTreasure extends AbstractResponse {

    /**
     * 期号
     */
    @ProtoDesc("期号")
    private long issueNumber;

    /**
     * 购买数量
     */
    @ProtoDesc("购买数量")
    private int buyCount;

    /**
     * 剩余可购买数量
     */
    @ProtoDesc("剩余可购买数量")
    private int remainingCount;

    /**
     * 当前状态 1=可购买,2=等待开奖,3=待领取,4=已领取,5=领奖结束(中奖未领取),6=未中奖
     */
    @ProtoDesc("当前状态 1=可购买,2=等待开奖,3=待领取,4=已领取,5=领奖结束(中奖未领取),6=未中奖")
    private int status;

    public ResBuyLuckyTreasure(int code) {
        super(code);
    }

    public long getIssueNumber() {
        return issueNumber;
    }

    public void setIssueNumber(long issueNumber) {
        this.issueNumber = issueNumber;
    }

    public int getBuyCount() {
        return buyCount;
    }

    public void setBuyCount(int buyCount) {
        this.buyCount = buyCount;
    }

    public int getRemainingCount() {
        return remainingCount;
    }

    public void setRemainingCount(int remainingCount) {
        this.remainingCount = remainingCount;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }
}
