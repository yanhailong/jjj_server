package com.jjg.game.alliance.pb.res;

import com.jjg.game.alliance.constant.AllianceConst;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * 联盟通用变更通知。
 *
 * @author 11
 * @date 2026/6/11
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ALLIANCE, cmd = AllianceConst.MsgBean.NOTIFY_ALLIANCE, resp = true)
@ProtoDesc("联盟通用变更通知")
public class NotifyAlliance extends AbstractResponse {
    @ProtoDesc("类型(AllianceConst.NotifyType)")
    public int type;
    @ProtoDesc("联盟id")
    public long allianceId;
    @ProtoDesc("附加参数(升级=新等级/转让=新盟主id等)")
    public String param;

    public NotifyAlliance(int code) {
        super(code);
    }
}
