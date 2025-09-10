package com.jjg.game.activity.cashcow.message.req;

import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author lm
 * @date 2025/9/10 09:39
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ACTIVITY, cmd = ActivityConstant.MsgBean.REQ_CASH_COW_RECORD)
@ProtoDesc("摇钱树请求记录")
public class ReqCashCowRecord {
    @ProtoDesc("活动id")
    public long activityId;
    @ProtoDesc("类型 1个人记录 2全部记录")
    public int type;
    @ProtoDesc("起始索引")
    public int startIndex;
    @ProtoDesc("每页数目")
    public int size;
}
