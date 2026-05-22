package com.jjg.game.sim.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.sim.constant.SimConstant;
import com.jjg.game.sim.pb.strcut.GuestInfo;

import java.util.List;

/**
 * @author 11
 * @date 2026/5/15
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME, cmd = SimConstant.MsgBean.NOTIFY_GENERATE_GUEST, resp = true)
@ProtoDesc("通知生成游客")
public class NotifyGenerateGuest extends AbstractResponse {
    @ProtoDesc("游客信息")
    public List<GuestInfo> guests;

    public NotifyGenerateGuest(int code) {
        super(code);
    }
}
