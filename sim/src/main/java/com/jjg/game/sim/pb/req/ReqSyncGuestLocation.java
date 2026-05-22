package com.jjg.game.sim.pb.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.sim.constant.SimConstant;

/**
 * @author 11
 * @date 2026/5/21
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME, cmd = SimConstant.MsgBean.REQ_SYNC_GUEST_DEST)
@ProtoDesc("请求同步游客位置")
public class ReqSyncGuestLocation extends AbstractMessage {
    @ProtoDesc("游客id")
    public int guestId;
    @ProtoDesc("建筑id 为0表示离开场景")
    public int buildingId;
    @ProtoDesc("false=离开该建筑  true=到达该建筑")
    public boolean enter;
}