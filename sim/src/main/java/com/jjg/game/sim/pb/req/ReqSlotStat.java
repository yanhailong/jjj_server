package com.jjg.game.sim.pb.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.sim.constant.SimConstant;

/**
 * @author 11
 * @date 2026/6/15
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME, cmd = SimConstant.MsgBean.REQ_SLOT_STAT)
@ProtoDesc("经营信息-SPINE游戏数据 (指定游戏)")
public class ReqSlotStat extends AbstractMessage {
    @ProtoDesc("指定游戏类型 (0=不指定,仅返回解锁游戏数)")
    public int gameType;
}
