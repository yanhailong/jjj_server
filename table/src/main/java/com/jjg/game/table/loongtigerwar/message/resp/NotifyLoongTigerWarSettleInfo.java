package com.jjg.game.table.loongtigerwar.message.resp;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.pb.AbstractResponse;
import com.jjg.game.table.loongtigerwar.message.LoongTigerWarMessageConstant;
import com.jjg.game.table.loongtigerwar.message.bean.LoongTigerWarPlayerSettleInfo;

import java.util.List;

/**
 * @author lm
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.LOONG_TIGER_WAR_TYPE,
        cmd = LoongTigerWarMessageConstant.RespMsgBean.NOTIFY_LOONG_TIGER_WAR_SETTLE_INFO,
        resp = true)
@ProtoDesc("龙虎斗结算信息")
public class NotifyLoongTigerWarSettleInfo extends AbstractResponse {

    @ProtoDesc("龙方牌")
    public int loongCard;

    @ProtoDesc("虎方牌")
    public int tigerCard;

    @ProtoDesc("获胜状态(1龙胜 2虎胜 3和)")
    public int winState;

    @ProtoDesc("玩家获得的金币数")
    public long getGold;

    @ProtoDesc("玩家结算信息")
    public List<LoongTigerWarPlayerSettleInfo> playerSettleInfos;

    public NotifyLoongTigerWarSettleInfo() {
        super(Code.SUCCESS);
    }

    //  Builder 类
    public static class Builder {
        private int loongCard;
        private int tigerCard;
        private int winState;
        private long getGold;
        private List<LoongTigerWarPlayerSettleInfo> playerSettleInfos;

        public Builder loongCard(int loongCard) {
            this.loongCard = loongCard;
            return this;
        }

        public Builder tigerCard(int tigerCard) {
            this.tigerCard = tigerCard;
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

        public Builder playerSettleInfos(List<LoongTigerWarPlayerSettleInfo> playerSettleInfos) {
            this.playerSettleInfos = playerSettleInfos;
            return this;
        }

        public NotifyLoongTigerWarSettleInfo build() {
            NotifyLoongTigerWarSettleInfo info = new NotifyLoongTigerWarSettleInfo();
            info.loongCard = this.loongCard;
            info.tigerCard = this.tigerCard;
            info.winState = this.winState;
            info.getGold = this.getGold;
            info.playerSettleInfos = this.playerSettleInfos;
            return info;
        }
    }
}
