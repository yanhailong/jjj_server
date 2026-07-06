package com.jjg.game.sim.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.sim.constant.SimConstant;

/**
 * 创建协作房间返回 (成功即开始切换 slots 节点, 客户端随后走进入游戏流程)。
 *
 * @author 11
 * @date 2026/7/6
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME, cmd = SimConstant.MsgBean.RES_CREATE_COOP_ROOM, resp = true)
@ProtoDesc("创建协作房间返回")
public class ResCreateCoopRoom extends AbstractResponse {
    @ProtoDesc("房间id")
    public long roomId;
    @ProtoDesc("任务配置id")
    public int taskId;
    @ProtoDesc("游戏类型")
    public int gameType;

    public ResCreateCoopRoom(int code) {
        super(code);
    }
}
