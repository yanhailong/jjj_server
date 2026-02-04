package com.jjg.game.poker.game.tosouth.message.bean;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

@ProtobufMessage
@ProtoDesc("南方前进玩家结算信息")
public class ToSouthPlayerSettlementInfo {
    @ProtoDesc("玩家 ID")
    public long playerId;
    @ProtoDesc("赢分 (净输赢)")
    public long winScore;
    @ProtoDesc("当前分数 (结算后)")
    public long currentScore;
    @ProtoDesc("剩余手牌")
    public List<Integer> handCards;
    @ProtoDesc("是否赢家")
    public boolean isWinner;
    @ProtoDesc("是否通杀")
    public boolean isInstantWin;
    @ProtoDesc("通杀手牌 (仅在通杀时有效)")
    public List<Integer> instantWinCards;
    @ProtoDesc("通杀类型 (0:无, 1:4个2, 2:一条龙, 3:同色, 4:6个对, 5:5连对  6:6连对 7:3连三张  8: 4连三张 9:2个四张 10:3个四张 11:1个四张+3连对)")
    public int instantWinType;

    public ToSouthPlayerSettlementInfo() {
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private long playerId;
        private long winScore;
        private long currentScore;
        private List<Integer> handCards;
        private boolean isWinner;
        private boolean isInstantWin;
        private List<Integer> instantWinCards;
        private int instantWinType;

        public Builder playerId(long playerId) {
            this.playerId = playerId;
            return this;
        }

        public Builder winScore(long winScore) {
            this.winScore = winScore;
            return this;
        }

        public Builder currentScore(long currentScore) {
            this.currentScore = currentScore;
            return this;
        }

        public Builder handCards(List<Integer> handCards) {
            this.handCards = handCards;
            return this;
        }

        public Builder isWinner(boolean isWinner) {
            this.isWinner = isWinner;
            return this;
        }

        public Builder isInstantWin(boolean isInstantWin) {
            this.isInstantWin = isInstantWin;
            return this;
        }

        public Builder instantWinCards(List<Integer> instantWinCards) {
            this.instantWinCards = instantWinCards;
            return this;
        }

        public Builder instantWinType(int instantWinType) {
            this.instantWinType = instantWinType;
            return this;
        }

        public ToSouthPlayerSettlementInfo build() {
            ToSouthPlayerSettlementInfo info = new ToSouthPlayerSettlementInfo();
            info.playerId = this.playerId;
            info.winScore = this.winScore;
            info.currentScore = this.currentScore;
            info.handCards = this.handCards;
            info.isWinner = this.isWinner;
            info.isInstantWin = this.isInstantWin;
            info.instantWinCards = this.instantWinCards;
            info.instantWinType = this.instantWinType;
            return info;
        }
    }
}
