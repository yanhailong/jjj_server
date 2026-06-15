package com.jjg.game.sim.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.sim.constant.SimConstant;
import com.jjg.game.sim.pb.struct.StatInfo;

import java.util.List;

/**
 * @author 11
 * @date 2026/6/15
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME, cmd = SimConstant.MsgBean.RES_SLOT_STAT, resp = true)
@ProtoDesc("经营信息-SPINE游戏数据返回")
public class ResSlotStat extends AbstractResponse {
    @ProtoDesc("指定的游戏类型")
    public int gameType;
    @ProtoDesc("SPINE游戏数据列表 (KEY->数值)")
    public List<StatInfo> stats;

    public ResSlotStat(int code) {
        super(code);
    }
}
