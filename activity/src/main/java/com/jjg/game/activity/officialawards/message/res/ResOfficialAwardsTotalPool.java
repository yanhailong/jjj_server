package com.jjg.game.activity.officialawards.message.res;

import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author lm
 * @date 2025/9/10 09:39
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ACTIVITY, cmd = ActivityConstant.MsgBean.RES_OFFICIAL_AWARDS_TOTAL_POOL, resp = true)
@ProtoDesc("官方派奖总奖池数")
public class ResOfficialAwardsTotalPool extends AbstractResponse {
    @ProtoDesc("总奖池数")
    public long totalPool;
    public ResOfficialAwardsTotalPool(int code) {
        super(code);
    }
}
