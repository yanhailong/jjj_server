package com.jjg.game.hall.vip.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.hall.constant.HallConstant;
import com.jjg.game.common.pb.ItemInfo;
import com.jjg.game.hall.vip.pb.bean.VipGiftInfo;

import java.util.List;

/**
 * 请求领取奖励
 *
 * @author lm
 * @date 2025/8/28 09:32
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.HALL_TYPE,cmd = HallConstant.MsgBean.RES_VIP_CLAIM_GIFT_REWARD,resp = true)
@ProtoDesc("响应领取礼包奖励")
public class ResVipClaimGiftReward extends AbstractResponse {
    @ProtoDesc("道具信息")
    public List<ItemInfo> items;
    @ProtoDesc("礼包信息")
    public VipGiftInfo vipGiftInfo;
    @ProtoDesc("当前已领取的最大vip等级")
    public int claimMaxLv;
    public ResVipClaimGiftReward(int code) {
        super(code);
    }
}
