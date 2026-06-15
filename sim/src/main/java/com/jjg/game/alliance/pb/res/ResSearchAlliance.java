package com.jjg.game.alliance.pb.res;

import com.jjg.game.alliance.constant.AllianceConst;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.alliance.pb.struct.AllianceBrief;

/**
 * 搜索联盟返回。
 *
 * @author 11
 * @date 2026/6/11
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ALLIANCE, cmd = AllianceConst.MsgBean.RES_SEARCH_ALLIANCE, resp = true)
@ProtoDesc("搜索联盟返回")
public class ResSearchAlliance extends AbstractResponse {
    @ProtoDesc("联盟概要(未找到为null)")
    public AllianceBrief alliance;

    public ResSearchAlliance(int code) {
        super(code);
    }
}
