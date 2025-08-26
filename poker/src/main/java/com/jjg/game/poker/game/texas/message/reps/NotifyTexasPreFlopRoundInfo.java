package com.jjg.game.poker.game.texas.message.reps;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.common.pb.AbstractNotice;
import com.jjg.game.poker.game.texas.constant.TexasConstant;

import java.util.List;

/**
 * @author lm
 * @date 2025/7/29 18:25
 */

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.TEXAS_TYPE
        , cmd = TexasConstant.MsgBean.NOTIFY_PRE_FLOP_ROUND_INFO, resp = true)
@ProtoDesc("通知第一轮的信息")
public class NotifyTexasPreFlopRoundInfo extends AbstractNotice {
    @ProtoDesc("手牌")
    public List<Integer> cards;
    @ProtoDesc("当前操作的玩家id")
    public long playerId;
    @ProtoDesc("超时时间")
    public long overTime;
    @ProtoDesc("大盲注")
    public long bbBet;
    @ProtoDesc("小盲注")
    public long sbBet;
    @ProtoDesc("底注")
    public long totalBet;
    @ProtoDesc("玩家状态(true在游戏中 false不在游戏中)")
    public boolean playerStatus;
    @ProtoDesc("庄家位置")
    public int seatId;

    // Builder 内部类
    public static class Builder {
        private List<Integer> cards;
        private long playerId;
        private long overTime;
        private long bbBet;
        private long sbBet;
        private long totalBet;
        private boolean playerStatus;
        private int seatId;

        public Builder cards(List<Integer> cards) {
            this.cards = cards;
            return this;
        }

        public Builder playerId(long playerId) {
            this.playerId = playerId;
            return this;
        }

        public Builder overTime(long overTime) {
            this.overTime = overTime;
            return this;
        }

        public Builder bbBet(long bbBet) {
            this.bbBet = bbBet;
            return this;
        }

        public Builder sbBet(long sbBet) {
            this.sbBet = sbBet;
            return this;
        }

        public Builder totalBet(long totalBet) {
            this.totalBet = totalBet;
            return this;
        }

        public Builder playerStatus(boolean playerStatus) {
            this.playerStatus = playerStatus;
            return this;
        }

        public Builder seatId(int seatId) {
            this.seatId = seatId;
            return this;
        }
        public NotifyTexasPreFlopRoundInfo build() {
            NotifyTexasPreFlopRoundInfo info = new NotifyTexasPreFlopRoundInfo();
            info.cards = this.cards;
            info.playerId = this.playerId;
            info.overTime = this.overTime;
            info.bbBet = this.bbBet;
            info.sbBet = this.sbBet;
            info.totalBet = this.totalBet;
            info.playerStatus = this.playerStatus;
            info.seatId = this.seatId;
            return info;
        }
    }

    // 可选：提供一个静态方法方便调用
    public static Builder builder() {
        return new Builder();
    }
}
