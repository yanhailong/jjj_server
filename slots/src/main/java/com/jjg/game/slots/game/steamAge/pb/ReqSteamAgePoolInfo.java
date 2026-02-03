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
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.STEAM_AGE, cmd = SteamAgeConstant.MsgBean.REQ_POOL_INFO)
@ProtoDesc("请求奖池信息")
public class ReqSteamAgePoolInfo extends AbstractMessage {
    @ProtoDesc("下注金额")
    public long stakeVlue;
}
