package com.jjg.game.activity.officialawards.message.req;

import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author lm
 * @date 2025/9/10 09:39
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ACTIVITY, cmd = ActivityConstant.MsgBean.REQ_OFFICIAL_AWARDS_RECORD)
@ProtoDesc("官方派奖请求记录")
public class ReqOfficialAwardsRecord {
    @ProtoDesc("类型 1个人记录 2全部记录")
    public int type;
    @ProtoDesc("起始索引")
    public int startIndex;
    @ProtoDesc("每页数目")
    public int size;
}
