package com.jjg.game.slots.game.christmasBashNight.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.christmasBashNight.ChristmasBashNightConstant;

/**
 * @author lihaocao
 * @date 2025/12/2 17:50
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.CHRISTMAS_NIGHT_TYPE, cmd = ChristmasBashNightConstant.MsgBean.REQ_POOL_INFO)
@ProtoDesc("请求奖池信息")
public class ReqChristmasBashNightPoolInfo extends AbstractMessage {
    @ProtoDesc("下注金额")
    public long stakeVlue;
}
