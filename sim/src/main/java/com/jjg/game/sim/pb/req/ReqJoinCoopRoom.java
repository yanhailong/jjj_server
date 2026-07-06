package com.jjg.game.sim.pb.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.sim.constant.SimConstant;

/**
 * 请求加入协作房间 (由频道邀请消息携带的 roomId 发起, 成功后切到房间所在 slots 节点)。
 *
 * @author 11
 * @date 2026/7/6
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME, cmd = SimConstant.MsgBean.REQ_JOIN_COOP_ROOM)
@ProtoDesc("请求加入协作房间")
public class ReqJoinCoopRoom extends AbstractMessage {
    @ProtoDesc("房间id")
    public long roomId;
}
