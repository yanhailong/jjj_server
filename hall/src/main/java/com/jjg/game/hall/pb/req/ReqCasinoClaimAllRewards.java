package com.jjg.game.hall.pb.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.hall.constant.HallConstant;

/**
 * @author lm
 * @date 2025/8/18 14:33
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.HALL_TYPE, cmd = HallConstant.MsgBean.REQ_CASINO_CLAIM_ALL_REWARDS)
@ProtoDesc("请求一键领取赌场收益")
public class ReqCasinoClaimAllRewards {
    @ProtoDesc("赌场id")
    public int casinoId;
}
