package com.jjg.game.alliance.pb.req;

import com.jjg.game.alliance.constant.AllianceConst;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * 处理入盟申请(支持一键)。
 *
 * @author 11
 * @date 2026/6/11
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ALLIANCE, cmd = AllianceConst.MsgBean.REQ_HANDLE_APPLICATION)
@ProtoDesc("处理入盟申请(支持一键)")
public class ReqHandleApplication extends AbstractMessage {
    @ProtoDesc("申请者id列表")
    public List<Long> playerIds;
    @ProtoDesc("true同意 false拒绝")
    public boolean agree;
}
