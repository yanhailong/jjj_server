package com.jjg.game.activepass.pb;

import com.jjg.game.sim.constant.SimConstant;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME, cmd = SimConstant.MsgBean.REQ_ACTIVE_PASS_REWARD_CLAIM)
@ProtoDesc("领取活跃通行证等级奖励")
public class ReqActivePassRewardClaim extends AbstractMessage {
    public int passId;
    /** 0为一键领取全部，否则为PassReward.id。 */
    public int rewardId;
    /** 0为全部已解锁轨道，否则1免费、2初级、4高级。 */
    public int track;
}
