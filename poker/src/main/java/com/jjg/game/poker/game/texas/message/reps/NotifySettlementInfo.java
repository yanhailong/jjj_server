package com.jjg.game.poker.game.texas.message.reps;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.AbstractNotice;
import com.jjg.game.poker.game.common.constant.PokerConstant;
import com.jjg.game.poker.game.common.message.bean.PlayerSettlementInfo;
import com.jjg.game.poker.game.texas.message.bean.TexasPotInfo;

import java.util.List;

/**
 * @author lm
 * @date 2025/7/25 14:34
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.POKER_GENERAL_TYPE,
        cmd = PokerConstant.MsgBean.NOTIFY_SETTLEMENT_INFO, resp = true)
@ProtoDesc("棋牌结算通知")
public class NotifySettlementInfo extends AbstractNotice {
    @ProtoDesc("结算时玩家信息")
    public List<PlayerSettlementInfo> playerSettlementInfos;
    @ProtoDesc("各个池玩家获得奖励信息")
    public List<TexasPotInfo> potInfos;
    @ProtoDesc("结算结束时间")
    public long endTime;
}
