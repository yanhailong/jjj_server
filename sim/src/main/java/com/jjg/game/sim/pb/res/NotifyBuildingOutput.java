package com.jjg.game.sim.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractNotice;
import com.jjg.game.common.pb.ItemInfo;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.sim.constant.SimConstant;

import java.util.List;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME, cmd = SimConstant.MsgBean.NOTIFY_BUILDING_OUTPUT, resp = true)
@ProtoDesc("通知建筑产出")
public class NotifyBuildingOutput extends AbstractNotice {
    @ProtoDesc("itemId = 1.金币  2.能量  3.服务能力  4.游戏上限  5.知名度  6.曝光度  12.经验")
    public List<ItemInfo> rewards;
}
