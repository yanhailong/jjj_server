package com.jjg.game.sim.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author 11
 * @date 2026/6/8
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.TO_SERVER_CONST_TYPE, cmd = MessageConst.ToServer.NOTICE_SLOTS_SPIN, resp = true, toPbFile = false)
@ProtoDesc("通知其他节点slots旋转")
public class NotifyServerPlayerSpin {
    public long playerId;
    public int gameType;
    public int winTimes;
    public String sessionId;
    public String sessionPath;
}
