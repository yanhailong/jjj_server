package com.jjg.game.alliance.pb.res;

import com.jjg.game.alliance.constant.AllianceConst;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.alliance.pb.struct.AllianceApplicationInfo;

import java.util.List;

/**
 * 入盟申请列表返回。
 *
 * @author 11
 * @date 2026/6/11
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ALLIANCE, cmd = AllianceConst.MsgBean.RES_APPLICATION_LIST, resp = true)
@ProtoDesc("入盟申请列表返回")
public class ResApplicationList extends AbstractResponse {
    @ProtoDesc("申请列表")
    public List<AllianceApplicationInfo> list;

    public ResApplicationList(int code) {
        super(code);
    }
}
