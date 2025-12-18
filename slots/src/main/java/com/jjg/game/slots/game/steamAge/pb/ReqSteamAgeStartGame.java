package com.jjg.game.slots.game.steamAge.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.steamAge.SteamAgeConstant;

/**
 * @author lihaocao
 * @date 2025/12/2 17:50
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.CHRISTMAS_NIGHT_TYPE, cmd = SteamAgeConstant.MsgBean.REQ_START_GAME)
@ProtoDesc("请求开始游戏")
public class ReqSteamAgeStartGame extends AbstractMessage {
    @ProtoDesc("下注金额")
    public long stakeVlue;
}
