package com.jjg.game.slots.game.candyparty.pb.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.candyparty.constant.CandyPartyConstant;

/**
 * @author 11
 * @date 2025/8/1 17:50
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.CANDY_PARTY, cmd = CandyPartyConstant.MsgBean.REQ_CANDY_PARTY_START_GAME)
@ProtoDesc("请求开始游戏")
public class ReqCandyPartyStartGame extends AbstractMessage {
    @ProtoDesc("下注金额")
    public long stakeValue;
}
