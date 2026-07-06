package com.jjg.game.sim.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.sim.constant.SimConstant;

/**
 * 加入协作房间返回 (成功即开始切换到房间所在 slots 节点)。
 *
 * @author 11
 * @date 2026/7/6
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME, cmd = SimConstant.MsgBean.RES_JOIN_COOP_ROOM, resp = true)
@ProtoDesc("加入协作房间返回")
public class ResJoinCoopRoom extends AbstractResponse {
    @ProtoDesc("房间id")
    public long roomId;
    @ProtoDesc("游戏类型")
    public int gameType;

    public ResJoinCoopRoom(int code) {
        super(code);
    }
}
