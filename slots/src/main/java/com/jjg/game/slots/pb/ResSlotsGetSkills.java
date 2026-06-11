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
 * @date 2026/6/11
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SLOTS_COMMON, cmd = SlotsConst.SlotsCommon.RES_SLOTS_GET_SKILLS, resp = true)
@ProtoDesc("获取slots技能")
public class ResSlotsGetSkills extends AbstractResponse {
    @ProtoDesc("技能信息  propId->level")
    public List<KVInfo> skillInfos;

    public ResSlotsGetSkills(int code) {
        super(code);
    }
}
