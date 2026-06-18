package com.jjg.game.sim.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.pb.ItemInfo;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.sim.constant.SimConstant;

import java.util.List;

/**
 * @author 11
 * @date 2026/6/15
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME, cmd = SimConstant.MsgBean.RES_PURCHASED_GUEST_REWARD, resp = true)
@ProtoDesc("领取购买游客奖励返回 (单个目的地, 奖励可能为空)")
public class ResPurchasedGuestReward extends AbstractResponse {
    @ProtoDesc("购买游客唯一id")
    public String uid;
    @ProtoDesc("目的地序号")
    public int index;
    @ProtoDesc("本目的地结算的奖励 (可能为空)")
    public List<ItemInfo> rewards;

    public ResPurchasedGuestReward(int code) {
        super(code);
    }
}
