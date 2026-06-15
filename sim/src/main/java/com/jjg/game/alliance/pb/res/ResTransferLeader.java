package com.jjg.game.alliance.pb.res;

import com.jjg.game.alliance.constant.AllianceConst;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * 转让盟主返回。
 *
 * @author 11
 * @date 2026/6/11
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ALLIANCE, cmd = AllianceConst.MsgBean.RES_TRANSFER_LEADER, resp = true)
@ProtoDesc("转让盟主返回")
public class ResTransferLeader extends AbstractResponse {
    @ProtoDesc("新盟主id")
    public long newLeaderId;

    public ResTransferLeader(int code) {
        super(code);
    }
}
