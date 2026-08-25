package com.jjg.game.sim.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractNotice;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.sim.constant.SimConstant;
import com.jjg.game.sim.pb.struct.CasinoUpgradeCondition;

import java.util.List;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME,cmd = SimConstant.MsgBean.NOTIFY_CASINO_UPGRADE, resp = true)
@ProtoDesc("通知场景升级")
public class NotifyCasinoUpgrade extends AbstractNotice {
    @ProtoDesc("场景等级")
    public int level;
    @ProtoDesc("场景经验")
    public int exp;
    @ProtoDesc("升级消耗的经验")
    public int upgradeCost;
    @ProtoDesc("升级需要的条件")
    public List<CasinoUpgradeCondition> upgradeLevelConditions;
}
