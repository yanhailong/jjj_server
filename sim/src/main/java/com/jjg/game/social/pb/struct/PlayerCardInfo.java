package com.jjg.game.social.pb.struct;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * 玩家信息卡。
 *
 * @author 11
 * @date 2026/6/9
 */
@ProtobufMessage
@ProtoDesc("玩家信息卡")
public class PlayerCardInfo {
    @ProtoDesc("玩家id")
    public long playerId;
    @ProtoDesc("昵称")
    public String nick;
    @ProtoDesc("头像id")
    public int headImg;
    @ProtoDesc("头像框id")
    public int headFrame;
    @ProtoDesc("等级")
    public int level;
    @ProtoDesc("金币")
    public long gold;
    @ProtoDesc("钻石")
    public long diamond;
    @ProtoDesc("联盟名称, 无则空")
    public String allianceName;
    @ProtoDesc("场景图标列表")
    public List<CasinoIconInfo> casinos;
    @ProtoDesc("与我的关系 0.陌生 1.好友 2.申请中")
    public int relation;
    @ProtoDesc("是否被我拉黑")
    public boolean inBlacklist;
    @ProtoDesc("是否在线")
    public boolean online;
    @ProtoDesc("该玩家已选展示徽章的当前最高档位ID列表(MedalBuff.id)，按展示顺序，未达到首档的不返回")
    public List<Integer> displayedMedalIds;
}
