package com.jjg.game.alliance.pb.struct;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * 联盟概要 (列表/搜索/主界面/排行通用)。
 *
 * @author 11
 * @date 2026/6/11
 */
@ProtobufMessage
@ProtoDesc("联盟概要信息")
public class AllianceBrief {
    @ProtoDesc("联盟id")
    public long allianceId;
    @ProtoDesc("名称")
    public String name;
    @ProtoDesc("图标id")
    public int icon;
    @ProtoDesc("公告")
    public String notice;
    @ProtoDesc("等级")
    public int level;
    @ProtoDesc("累计声誉值")
    public long reputation;
    @ProtoDesc("升到下一级所需累计声誉值(-1已满级)")
    public long nextLevelReputation;
    @ProtoDesc("当前人数")
    public int memberCount;
    @ProtoDesc("人数上限")
    public int memberCap;
    @ProtoDesc("入盟最低场景等级")
    public int joinMinCasinoLevel;
    @ProtoDesc("入盟是否需要审核")
    public boolean joinNeedAudit;
    @ProtoDesc("盟主id")
    public long leaderId;
}
