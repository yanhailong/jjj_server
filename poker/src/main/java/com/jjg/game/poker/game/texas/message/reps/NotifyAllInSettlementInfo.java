package com.jjg.game.poker.game.texas.message.reps;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.AbstractNotice;
import com.jjg.game.poker.game.texas.constant.TexasConstant;
import com.jjg.game.poker.game.texas.message.bean.TexasRoundInfo;

import java.util.List;

/**
 * @author lm
 * @date 2025/7/31 14:22
 */

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.TEXAS_TYPE,cmd = TexasConstant.MsgBean.NOTIFY_ALL_IN_SETTLEMENT_INFO,resp = true)
@ProtoDesc("通知德州扑克allIn结算信息")
public class NotifyAllInSettlementInfo extends AbstractNotice {
    @ProtoDesc("轮次信息")
    public List<TexasRoundInfo> roundInfos;
    @ProtoDesc("结算信息")
    public NotifySettlementInfo settlementInfo;
}
