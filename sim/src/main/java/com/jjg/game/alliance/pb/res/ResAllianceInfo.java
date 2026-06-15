package com.jjg.game.alliance.pb.res;

import com.jjg.game.alliance.constant.AllianceConst;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.alliance.pb.struct.AllianceBrief;

/**
 * 联盟主界面信息返回。
 *
 * @author 11
 * @date 2026/6/11
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ALLIANCE, cmd = AllianceConst.MsgBean.RES_ALLIANCE_INFO, resp = true)
@ProtoDesc("联盟主界面信息返回")
public class ResAllianceInfo extends AbstractResponse {
    @ProtoDesc("我所在联盟id(0=无盟)")
    public long myAllianceId;
    @ProtoDesc("联盟概要(无盟为null)")
    public AllianceBrief alliance;
    @ProtoDesc("我的贡献值")
    public long myContribution;
    @ProtoDesc("我的职位 1盟主 3成员")
    public int myPosition;

    public ResAllianceInfo(int code) {
        super(code);
    }
}
