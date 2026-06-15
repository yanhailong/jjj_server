package com.jjg.game.alliance.pb.req;

import com.jjg.game.alliance.constant.AllianceConst;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * 解散联盟(盟主)。
 *
 * @author 11
 * @date 2026/6/11
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ALLIANCE, cmd = AllianceConst.MsgBean.REQ_DISSOLVE_ALLIANCE)
@ProtoDesc("解散联盟(盟主)")
public class ReqDissolveAlliance extends AbstractMessage {
}
