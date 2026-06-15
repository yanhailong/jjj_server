package com.jjg.game.alliance.pb.res;

import com.jjg.game.alliance.constant.AllianceConst;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.alliance.pb.struct.AllianceMemberInfo;

import java.util.List;

/**
 * 成员列表返回。
 *
 * @author 11
 * @date 2026/6/11
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ALLIANCE, cmd = AllianceConst.MsgBean.RES_MEMBER_LIST, resp = true)
@ProtoDesc("成员列表返回")
public class ResMemberList extends AbstractResponse {
    @ProtoDesc("成员列表")
    public List<AllianceMemberInfo> list;
    @ProtoDesc("人数上限")
    public int memberCap;

    public ResMemberList(int code) {
        super(code);
    }
}
