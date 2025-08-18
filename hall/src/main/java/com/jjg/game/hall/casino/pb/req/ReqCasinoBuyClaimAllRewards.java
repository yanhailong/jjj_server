package com.jjg.game.hall.casino.pb.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.hall.constant.HallConstant;

/**
 * @author lm
 * @date 2025/8/18 14:33
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.HALL_TYPE,cmd = HallConstant.MsgBean.REQ_CASINO_BUY_CLAIM_ALL_REWARDS)
@ProtoDesc("请求购买一键领取")
public class ReqCasinoBuyClaimAllRewards {
    @ProtoDesc("赌场id")
    public int casinoId;
}
