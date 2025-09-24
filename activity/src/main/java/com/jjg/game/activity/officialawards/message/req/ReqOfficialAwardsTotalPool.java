package com.jjg.game.activity.officialawards.message.req;

import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author lm
 * @date 2025/9/10 09:39
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ACTIVITY, cmd = ActivityConstant.MsgBean.REQ_OFFICIAL_AWARDS_TOTAL_POOL)
@ProtoDesc("官方派奖请求总奖池")
public class ReqOfficialAwardsTotalPool {
}
