package com.jjg.game.activity.cashcow.message.res;

import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author lm
 * @date 2025/9/10 09:39
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ACTIVITY, cmd = ActivityConstant.MsgBean.RES_CASH_COW_JOIN, resp = true)
@ProtoDesc("摇钱树响应参加活动结果")
public class ResCashCowJoin extends AbstractResponse {
    @ProtoDesc("活动id")
    public long activityId;
    @ProtoDesc("详情id")
    public int detailId;
    @ProtoDesc("本次获得数量")
    public long num;
    @ProtoDesc("总奖池")
    public long totalPool;
    @ProtoDesc("奖池")
    public long poll;

    public ResCashCowJoin(int code) {
        super(code);
    }
}
