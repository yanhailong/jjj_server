package com.jjg.game.alliance.pb.res;

import com.jjg.game.alliance.constant.AllianceConst;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.alliance.pb.struct.AllianceBrief;

/**
 * 编辑联盟返回。
 *
 * @author 11
 * @date 2026/6/11
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ALLIANCE, cmd = AllianceConst.MsgBean.RES_EDIT_ALLIANCE, resp = true)
@ProtoDesc("编辑联盟返回")
public class ResEditAlliance extends AbstractResponse {
    @ProtoDesc("编辑后的联盟概要")
    public AllianceBrief alliance;

    public ResEditAlliance(int code) {
        super(code);
    }
}
