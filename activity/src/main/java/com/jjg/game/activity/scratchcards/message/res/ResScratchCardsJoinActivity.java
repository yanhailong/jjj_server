package com.jjg.game.activity.scratchcards.message.res;

import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.activity.scratchcards.message.bean.ScratchCardsRewardsInfo;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * @author lm
 * @date 2025/9/4 13:43
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ACTIVITY, cmd = ActivityConstant.MsgBean.RES_SCRATCH_CARDS_JOIN_ACTIVITY, resp = true)
@ProtoDesc("响应刮刮乐参与活动")
public class ResScratchCardsJoinActivity extends AbstractResponse {
    @ProtoDesc("活动id")
    public long activityId;
    @ProtoDesc("详情id")
    public long detailId;
    @ProtoDesc("中奖信息")
    public List<ScratchCardsRewardsInfo> rewardsInfo;

    public ResScratchCardsJoinActivity(int code) {
        super(code);
    }
}
