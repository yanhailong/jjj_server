package com.jjg.game.hall.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.pb.ItemInfo;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.hall.constant.HallConstant;

import java.util.List;

/**
 * @author lm
 * @date 2026/3/12 16:34
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.HALL_TYPE, cmd = HallConstant.MsgBean.RES_REDEEM_CODE, resp = true)
@ProtoDesc("请求兑换礼包码返回")
public class ResRedeemCode extends AbstractResponse {
    @ProtoDesc("奖励内容")
    public List<ItemInfo> rewardItemInfos;
    @ProtoDesc("冷却结束时间戳(毫秒)")
    public long cooldownEndTime;

    public ResRedeemCode(int code) {
        super(code);
    }
}
