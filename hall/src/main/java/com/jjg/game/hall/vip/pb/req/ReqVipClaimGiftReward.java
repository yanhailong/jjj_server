package com.jjg.game.hall.vip.pb.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.hall.constant.HallConstant;

/**
 * 请求领取奖励
 *
 * @author lm
 * @date 2025/8/28 09:32
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.HALL_TYPE,cmd = HallConstant.MsgBean.REQ_VIP_CLAIM_GIFT_REWARD)
@ProtoDesc("请求领取礼包奖励")
public class ReqVipClaimGiftReward extends AbstractMessage {
    @ProtoDesc("类型 1周工资 2生日彩金 3晋级彩金 4年终奖")
    public int type;
    @ProtoDesc("领取的vip等级")
    public int vipLevel;
}
