package com.jjg.game.hall.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.hall.constant.HallConstant;

/**
 * @author lm
 * @date 2025/8/18 14:33
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.HALL_TYPE, cmd = HallConstant.MsgBean.RES_CASINO_BUY_CLAIM_ALL_REWARDS,resp = true)
@ProtoDesc("请求购买一键领取")
public class ResCasinoBuyClaimAllRewards {
    @ProtoDesc("赌场id")
    public int casinoId;
    @ProtoDesc("到期时间")
    public long endTime;
}
