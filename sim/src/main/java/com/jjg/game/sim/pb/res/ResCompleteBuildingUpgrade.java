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
 * @date 2026/5/28
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME, cmd = SimConstant.MsgBean.RES_COMPLETE_BUILDING_UPGRADE, resp = true)
@ProtoDesc("完成建筑升级返回")
public class ResCompleteBuildingUpgrade extends AbstractResponse {
    @ProtoDesc("建筑id")
    public int id;
    @ProtoDesc("新等级")
    public int level;
    @ProtoDesc("升级奖励")
    public List<ItemInfo> rewards;

    public ResCompleteBuildingUpgrade(int code) {
        super(code);
    }
}
