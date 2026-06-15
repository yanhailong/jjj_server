package com.jjg.game.alliance.pb.struct;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * 成员列表项。
 *
 * @author 11
 * @date 2026/6/11
 */
@ProtobufMessage
@ProtoDesc("联盟成员信息")
public class AllianceMemberInfo {
    @ProtoDesc("玩家id")
    public long playerId;
    @ProtoDesc("昵称")
    public String nick;
    @ProtoDesc("头像id")
    public int headImg;
    @ProtoDesc("头像框id")
    public int headFrame;
    @ProtoDesc("玩家等级")
    public int level;
    @ProtoDesc("职位 1盟主 3成员")
    public int position;
    @ProtoDesc("状态 0离线 1在线 2游戏中")
    public int status;
    @ProtoDesc("历史累计贡献度")
    public long contributionTotal;
    @ProtoDesc("入盟时间(ms)")
    public long joinTime;
}
