package com.jjg.game.alliance.pb.res;

import com.jjg.game.alliance.constant.AllianceConst;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.alliance.pb.struct.AllianceBrief;

import java.util.List;

/**
 * 可加入联盟列表返回。
 *
 * @author 11
 * @date 2026/6/11
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ALLIANCE, cmd = AllianceConst.MsgBean.RES_ALLIANCE_LIST, resp = true)
@ProtoDesc("可加入联盟列表返回")
public class ResAllianceList extends AbstractResponse {
    @ProtoDesc("联盟列表(按声誉降序)")
    public List<AllianceBrief> list;

    public ResAllianceList(int code) {
        super(code);
    }
}
