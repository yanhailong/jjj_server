package com.jjg.game.hall.casino.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.constant.Code;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.hall.constant.HallConstant;

/**
 * @author lm
 * @date 2025/8/18 14:33
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.HALL_TYPE, cmd = HallConstant.MsgBean.RES_CASINO_BUY_CLAIM_ALL_REWARDS,resp = true)
@ProtoDesc("响应购买一键领取")
public class ResCasinoBuyClaimAllRewards extends AbstractResponse {
    @ProtoDesc("赌场id")
    public int casinoId;
    @ProtoDesc("到期时间")
    public long endTime;
    public ResCasinoBuyClaimAllRewards() {
        super(Code.SUCCESS);
    }
}
