package com.jjg.game.poker.game.blackjack.message.resp;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.AbstractNotice;
import com.jjg.game.poker.game.blackjack.constant.BlackJackConstant;
import com.jjg.game.poker.game.blackjack.message.bean.BlackJackPlayerInfo;

import java.util.List;

/**
 * @author lm
 * @date 2025/7/29 13:36
 */

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.BLACK_JACK_TYPE, cmd = BlackJackConstant.MsgBean.REPS_BLACK_ROOM_BASE_INFO, resp = true)
@ProtoDesc("响应房间基本信息")
public class RepsBlackJackRoomBaseInfo extends AbstractNotice {
    @ProtoDesc("玩家信息 不包含庄家")
    public List<BlackJackPlayerInfo> playerInfos;
    @ProtoDesc("结算信息")
    public NotifyBlackJackSettlementInfo settlementInfo;
    @ProtoDesc("当前操作人")
    public long operationId;
    @ProtoDesc("操作结束时间")
    public long overTime;
    @ProtoDesc("庄家能展示的牌")
    public List<Integer> cardIds;
}
