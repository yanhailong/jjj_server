package com.jjg.game.poker.game.tosouth.message.notify;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractNotice;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.poker.game.tosouth.constant.ToSouthConstant;
import com.jjg.game.poker.game.tosouth.message.bean.ToSouthPlayerSettlementInfo;

import java.util.List;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.TO_SOUTH, cmd = ToSouthConstant.MsgBean.NOTIFY_SETTLEMENT_INFO)
@ProtoDesc("通知南方前进结算信息")
public class NotifyToSouthSettlementInfo extends AbstractNotice {
    @ProtoDesc("玩家结算列表")
    public List<ToSouthPlayerSettlementInfo> settlementInfos;
    @ProtoDesc("结算时间")
    public long endTime;
}
