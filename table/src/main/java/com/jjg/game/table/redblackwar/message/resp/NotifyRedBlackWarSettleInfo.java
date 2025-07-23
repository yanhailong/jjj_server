package com.jjg.game.table.redblackwar.message.resp;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.pb.AbstractResponse;
import com.jjg.game.table.redblackwar.message.RedBlackWarMessageConstant;
import com.jjg.game.table.redblackwar.message.bean.RBWPlayerSettleInfo;

import java.util.List;

/**
 * @author lm
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.RED_BLACK_WAR_TYPE,
        cmd = RedBlackWarMessageConstant.RespMsgBean.NOTIFY_RED_BLACK_WAR_SETTLE_INFO,
        resp = true)
@ProtoDesc("红黑大战结算信息")
public class NotifyRedBlackWarSettleInfo extends AbstractResponse {
    @ProtoDesc("红方牌")
    public List<Integer> redCards;

    @ProtoDesc("红方牌型")
    public int redCardType;

    @ProtoDesc("黑方牌")
    public List<Integer> blackCards;

    @ProtoDesc("黑方牌型")
    public int blackCardType;

    @ProtoDesc("获胜状态(1红方胜 2黑方胜)")
    public int winState;

    @ProtoDesc("玩家获得的金币数")
    public long getGold;

    @ProtoDesc("玩家结算信息")
    public List<RBWPlayerSettleInfo> playerSettleInfos;

    public NotifyRedBlackWarSettleInfo() {
        super(Code.SUCCESS);
    }

    //  Builder 内部类
    public static class Builder {
        private List<Integer> redCards;
        private int redCardType;
        private List<Integer> blackCards;
        private int blackCardType;
        private int winState;
        private long getGold;
        private List<RBWPlayerSettleInfo> playerSettleInfos;

        public Builder redCards(List<Integer> redCards) {
            this.redCards = redCards;
            return this;
        }

        public Builder redCardType(int redCardType) {
            this.redCardType = redCardType;
            return this;
        }

        public Builder blackCards(List<Integer> blackCards) {
            this.blackCards = blackCards;
            return this;
        }

        public Builder blackCardType(int blackCardType) {
            this.blackCardType = blackCardType;
            return this;
        }

        public Builder winState(int winState) {
            this.winState = winState;
            return this;
        }

        public Builder getGold(long getGold) {
            this.getGold = getGold;
            return this;
        }

        public Builder playerSettleInfos(List<RBWPlayerSettleInfo> playerSettleInfos) {
            this.playerSettleInfos = playerSettleInfos;
            return this;
        }

        public NotifyRedBlackWarSettleInfo build() {
            NotifyRedBlackWarSettleInfo info = new NotifyRedBlackWarSettleInfo();
            info.redCards = this.redCards;
            info.redCardType = this.redCardType;
            info.blackCards = this.blackCards;
            info.blackCardType = this.blackCardType;
            info.winState = this.winState;
            info.getGold = this.getGold;
            info.playerSettleInfos = this.playerSettleInfos;
            return info;
        }
    }
}

