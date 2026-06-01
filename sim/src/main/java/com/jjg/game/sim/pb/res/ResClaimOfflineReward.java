package com.jjg.game.sim.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.pb.ItemInfo;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.sim.constant.SimConstant;

import java.util.List;

/**
 * 领取离线收益返回
 *
 * @author 11
 * @date 2026/5/29
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME, cmd = SimConstant.MsgBean.RES_CLAIM_OFFLINE_REWARD, resp = true)
@ProtoDesc("领取离线收益返回")
public class ResClaimOfflineReward extends AbstractResponse {
    @ProtoDesc("实发收益")
    public List<ItemInfo> rewards;
    @ProtoDesc("是否看广告领取")
    public boolean watchAd;

    public ResClaimOfflineReward(int code) {
        super(code);
    }
}
