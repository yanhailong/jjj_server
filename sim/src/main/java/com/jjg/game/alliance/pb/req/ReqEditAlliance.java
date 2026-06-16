package com.jjg.game.alliance.pb.req;

import com.jjg.game.alliance.constant.AllianceConst;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * 编辑联盟(盟主)。
 *
 * @author 11
 * @date 2026/6/11
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ALLIANCE, cmd = AllianceConst.MsgBean.REQ_EDIT_ALLIANCE)
@ProtoDesc("编辑联盟(盟主)")
public class ReqEditAlliance extends AbstractMessage {
    @ProtoDesc("名称")
    public String name;
    @ProtoDesc("图标id")
    public int icon;
    @ProtoDesc("公告")
    public String notice;
    @ProtoDesc("入盟最低场景等级")
    public int joinMinCasinoLevel;
    @ProtoDesc("入盟是否需要审核")
    public boolean joinNeedAudit;
}
