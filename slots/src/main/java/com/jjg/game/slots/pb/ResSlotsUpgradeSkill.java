package com.jjg.game.slots.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.KVInfo;
import com.jjg.game.slots.constant.SlotsConst;

import java.util.List;

/**
 * @author 11
 * @date 2026/5/18
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SLOTS_COMMON, cmd = SlotsConst.SlotsCommon.RES_SLOTS_UPGRADE_SKILL, resp = true)
@ProtoDesc("获取slots技能返回")
public class ResSlotsUpgradeSkill extends AbstractResponse {

    public ResSlotsUpgradeSkill(int code) {
        super(code);
    }
}
