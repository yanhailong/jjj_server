package com.jjg.game.sim.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.sim.constant.SimConstant;

/**
 * @author 11
 * @date 2026/5/15
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME, cmd = SimConstant.MsgBean.RES_SYNC_GUEST_DEST, resp = true)
@ProtoDesc("同步游客到达目的地返回")
public class ResSyncGuestLocation extends AbstractResponse {
    public ResSyncGuestLocation(int code) {
        super(code);
    }
}
