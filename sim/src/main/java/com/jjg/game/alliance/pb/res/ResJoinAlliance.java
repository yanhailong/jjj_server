package com.jjg.game.alliance.pb.res;

import com.jjg.game.alliance.constant.AllianceConst;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.alliance.pb.struct.AllianceBrief;

/**
 * 加入联盟返回。
 *
 * @author 11
 * @date 2026/6/11
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ALLIANCE, cmd = AllianceConst.MsgBean.RES_JOIN_ALLIANCE, resp = true)
@ProtoDesc("加入联盟返回")
public class ResJoinAlliance extends AbstractResponse {
    @ProtoDesc("1直接加入成功 2已提交申请")
    public int result;
    @ProtoDesc("联盟概要(一键申请成功加入时为所入盟)")
    public AllianceBrief alliance;

    public ResJoinAlliance(int code) {
        super(code);
    }
}
