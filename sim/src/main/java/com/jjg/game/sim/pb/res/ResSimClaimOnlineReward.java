package com.jjg.game.sim.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.pb.ItemInfo;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.sim.constant.SimConstant;
import com.jjg.game.sim.pb.struct.OnlineRewardInfo;

import java.util.List;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME, cmd = SimConstant.MsgBean.RES_SIM_CLAIM_ONLINE_REWARD, resp = true)
@ProtoDesc("领取在线收益返回")
public class ResSimClaimOnlineReward extends AbstractResponse {
    @ProtoDesc("本次实际收益，与OfflineReward.rewards一致，itemId = 1.金币 2.能量 3.服务能力 4.游戏上限 5.知名度 6.曝光度 12.场景经验")
    public List<ItemInfo> rewards;
    @ProtoDesc("最新在线收益信息，成功及次数、冷却校验失败时返回")
    public OnlineRewardInfo info;

    public ResSimClaimOnlineReward(int code) {
        super(code);
    }
}
