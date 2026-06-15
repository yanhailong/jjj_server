package com.jjg.game.alliance.pb.res;

import com.jjg.game.alliance.constant.AllianceConst;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * 踢出成员返回。
 *
 * @author 11
 * @date 2026/6/11
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ALLIANCE, cmd = AllianceConst.MsgBean.RES_KICK_MEMBER, resp = true)
@ProtoDesc("踢出成员返回")
public class ResKickMember extends AbstractResponse {
    @ProtoDesc("被踢玩家id")
    public long playerId;

    public ResKickMember(int code) {
        super(code);
    }
}
