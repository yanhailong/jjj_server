package com.jjg.game.sim.pb.struct;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

@ProtobufMessage
@ProtoDesc("拜访赌场公开快照")
public class VisitCasinoInfo {
    @ProtoDesc("房主id")
    public long playerId;
    @ProtoDesc("房主名称")
    public String playerName;
    @ProtoDesc("头像id")
    public int headImgId;
    @ProtoDesc("头像框id")
    public int headFrameId;
    @ProtoDesc("经营总等级")
    public int roleLevel;
    @ProtoDesc("赌场id")
    public int casinoId;
    @ProtoDesc("赌场等级")
    public int casinoLevel;
    @ProtoDesc("游客容量")
    public int visitorCapacity;
    @ProtoDesc("总人气")
    public long popularity;
    @ProtoDesc("今日已获得人气")
    public int todayPopularity;
    @ProtoDesc("每日人气上限")
    public int dailyPopularityLimit;
    @ProtoDesc("展示勋章id")
    public List<Integer> medalIds;
    @ProtoDesc("建筑公开信息")
    public List<VisitBuildingInfo> buildings;
    @ProtoDesc("可试玩游戏")
    public List<VisitGameInfo> games;
    @ProtoDesc("礼物配置")
    public List<VisitGiftInfo> gifts;
}
