package com.jjg.game.sim.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.pb.ItemInfo;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.KVInfo;
import com.jjg.game.sim.constant.SimConstant;

import java.util.List;

/**
 * @author 11
 * @date 2026/5/28
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME, cmd = SimConstant.MsgBean.RES_RECRUIT_GUEST, resp = true)
@ProtoDesc("招募游客返回")
public class ResRecruitGuest extends AbstractResponse {
    @ProtoDesc("获得的游客")
    public List<KVInfo> guests;
    @ProtoDesc("获得的碎片")
    public List<ItemInfo> items;
    @ProtoDesc("次数")
    public int count;

    public ResRecruitGuest(int code) {
        super(code);
    }
}
