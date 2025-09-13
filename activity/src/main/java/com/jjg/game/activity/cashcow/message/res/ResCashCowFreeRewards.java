package com.jjg.game.activity.cashcow.message.res;

import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.pb.ItemInfo;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author lm
 * @date 2025/9/13 13:34
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ACTIVITY, cmd = ActivityConstant.MsgBean.RES_CASH_COW_FREE_REWARDS, resp = true)
@ProtoDesc("响应领取摇钱树免费奖励")
public class ResCashCowFreeRewards extends AbstractResponse {
    @ProtoDesc("活动id")
    public long activityId;
    @ProtoDesc("奖励列表")
    public ItemInfo itemInfos;

    public ResCashCowFreeRewards(int code) {
        super(code);
    }
}
