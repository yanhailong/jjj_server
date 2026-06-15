package com.jjg.game.alliance.pb.req;

import com.jjg.game.alliance.constant.AllianceConst;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * 创建联盟。
 *
 * @author 11
 * @date 2026/6/11
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ALLIANCE, cmd = AllianceConst.MsgBean.REQ_CREATE_ALLIANCE)
@ProtoDesc("创建联盟")
public class ReqCreateAlliance extends AbstractMessage {
    @ProtoDesc("名称")
    public String name;
    @ProtoDesc("图标id")
    public int icon;
    @ProtoDesc("联盟描述")
    public String notice;
    @ProtoDesc("入盟最低赌场等级")
    public int joinMinCasinoLevel;
    @ProtoDesc("入盟是否需要审核")
    public boolean joinNeedAudit;
}
