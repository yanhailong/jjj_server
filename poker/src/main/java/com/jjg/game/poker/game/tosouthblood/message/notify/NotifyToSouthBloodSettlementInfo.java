package com.jjg.game.poker.game.tosouthblood.message.notify;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractNotice;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.poker.game.tosouthblood.constant.ToSouthBloodConstant;
import com.jjg.game.poker.game.tosouthblood.message.bean.ToSouthBloodPlayerSettlementInfo;

import java.util.List;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.TO_SOUTH_BLOOD, cmd = ToSouthBloodConstant.MsgBean.NOTIFY_SETTLEMENT_INFO, resp = true)
@ProtoDesc("通知南方前进-血战结算信息")
public class NotifyToSouthBloodSettlementInfo extends AbstractNotice {
    @ProtoDesc("玩家结算列表")
    public List<ToSouthBloodPlayerSettlementInfo> settlementInfos;
    @ProtoDesc("结算时间")
    public long endTime;

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private List<ToSouthBloodPlayerSettlementInfo> settlementInfos;
        private long endTime;

        public Builder settlementInfos(List<ToSouthBloodPlayerSettlementInfo> settlementInfos) {
            this.settlementInfos = settlementInfos;
            return this;
        }

        public Builder endTime(long endTime) {
            this.endTime = endTime;
            return this;
        }

        public NotifyToSouthBloodSettlementInfo build() {
            NotifyToSouthBloodSettlementInfo info = new NotifyToSouthBloodSettlementInfo();
            info.settlementInfos = this.settlementInfos;
            info.endTime = this.endTime;
            return info;
        }
    }
}
