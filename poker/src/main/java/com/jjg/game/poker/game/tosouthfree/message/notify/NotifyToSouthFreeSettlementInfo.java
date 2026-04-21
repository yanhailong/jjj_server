package com.jjg.game.poker.game.tosouthfree.message.notify;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractNotice;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.poker.game.tosouthfree.constant.ToSouthFreeConstant;
import com.jjg.game.poker.game.tosouthfree.message.bean.ToSouthFreePlayerSettlementInfo;

import java.util.List;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.TO_SOUTH, cmd = ToSouthFreeConstant.MsgBean.NOTIFY_SETTLEMENT_INFO, resp = true)
@ProtoDesc("通知南方前进-免费结算信息")
public class NotifyToSouthFreeSettlementInfo extends AbstractNotice {
    @ProtoDesc("玩家结算列表")
    public List<ToSouthFreePlayerSettlementInfo> settlementInfos;
    @ProtoDesc("结算时间")
    public long endTime;

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private List<ToSouthFreePlayerSettlementInfo> settlementInfos;
        private long endTime;

        public Builder settlementInfos(List<ToSouthFreePlayerSettlementInfo> settlementInfos) {
            this.settlementInfos = settlementInfos;
            return this;
        }

        public Builder endTime(long endTime) {
            this.endTime = endTime;
            return this;
        }

        public NotifyToSouthFreeSettlementInfo build() {
            NotifyToSouthFreeSettlementInfo info = new NotifyToSouthFreeSettlementInfo();
            info.settlementInfos = this.settlementInfos;
            info.endTime = this.endTime;
            return info;
        }
    }
}
