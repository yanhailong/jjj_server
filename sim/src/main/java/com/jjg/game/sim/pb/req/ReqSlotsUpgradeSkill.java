package com.jjg.game.sim.pb.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.constant.SlotsConst;

/**
 * @author 11
 * @date 2026/5/18
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SLOTS_COMMON, cmd = SlotsConst.SlotsCommon.REQ_SLOTS_UPGRADE_SKILL)
@ProtoDesc("升级技能")
public class ReqSlotsUpgradeSkill extends AbstractMessage {
    public int skillId;
}