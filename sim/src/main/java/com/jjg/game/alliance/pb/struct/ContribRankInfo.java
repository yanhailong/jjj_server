package com.jjg.game.alliance.pb.struct;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * 玩家维度排行项 (贡献度周榜 / 对决贡献榜单共用)。
 *
 * @author 11
 * @date 2026/6/11
 */
@ProtobufMessage
@ProtoDesc("玩家排行项")
public class ContribRankInfo {
    @ProtoDesc("排名(1起)")
    public int rank;
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
    @ProtoDesc("榜单分值(贡献度/比赛值)")
    public long score;
}
