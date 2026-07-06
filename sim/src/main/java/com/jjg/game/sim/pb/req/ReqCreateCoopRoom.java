package com.jjg.game.sim.pb.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.sim.constant.SimConstant;

/**
 * 请求创建协作房间 (成功后服务端把会话切到 slots 节点)。
 *
 * @author 11
 * @date 2026/7/6
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME, cmd = SimConstant.MsgBean.REQ_CREATE_COOP_ROOM)
@ProtoDesc("请求创建协作房间")
public class ReqCreateCoopRoom extends AbstractMessage {
    @ProtoDesc("任务配置id")
    public int taskId;
    @ProtoDesc("选择的游戏")
    public int gameType;
    @ProtoDesc("进入游戏所用房间配置id (warehouse.xlsx)")
    public int roomCfgId;
}
