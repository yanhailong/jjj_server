package com.jjg.game.hall.levelpack.message.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.hall.constant.HallConstant;

/**
 * @author lm
 * @date 2025/9/17 19:01
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.HALL_TYPE, cmd = HallConstant.MsgBean.REQ_PLAYER_LEVEL_CLAIM_REWARDS)
@ProtoDesc("等级礼包详细信息")
public class ReqPlayerLevelClaimRewards {
    @ProtoDesc("礼包id")
    public int id;
}
