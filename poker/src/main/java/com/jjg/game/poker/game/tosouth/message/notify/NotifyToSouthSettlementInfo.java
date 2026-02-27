package com.jjg.game.poker.game.tosouth.message.notify;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractNotice;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.poker.game.tosouth.constant.ToSouthConstant;
import com.jjg.game.poker.game.tosouth.message.bean.ToSouthPlayerSettlementInfo;

import java.util.List;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.TO_SOUTH, cmd = ToSouthConstant.MsgBean.NOTIFY_SETTLEMENT_INFO, resp = true)
@ProtoDesc("通知南方前进结算信息")
public class NotifyToSouthSettlementInfo extends AbstractNotice {
    @ProtoDesc("玩家结算列表")
    public List<ToSouthPlayerSettlementInfo> settlementInfos;
    @ProtoDesc("结算时间")
    public long endTime;

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private List<ToSouthPlayerSettlementInfo> settlementInfos;
        private long endTime;

        public Builder settlementInfos(List<ToSouthPlayerSettlementInfo> settlementInfos) {
            this.settlementInfos = settlementInfos;
            return this;
        }

        public Builder endTime(long endTime) {
            this.endTime = endTime;
            return this;
        }

        public NotifyToSouthSettlementInfo build() {
            NotifyToSouthSettlementInfo info = new NotifyToSouthSettlementInfo();
            info.settlementInfos = this.settlementInfos;
            info.endTime = this.endTime;
            return info;
        }
    }
}
