package com.jjg.game.sim.pb.struct;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.KVInfo;

import java.util.List;

/**
 * @author 11
 * @date 2026/5/27
 */
@ProtobufMessage
@ProtoDesc("建筑信息")
public class BuildingInfo {
    @ProtoDesc("建筑id")
    public int id;
    @ProtoDesc("等级")
    public int level;
    @ProtoDesc("升级 CD 结束时间")
    public long cdEndTime;
    @ProtoDesc("进度条")
    public int progress;
    @ProtoDesc("已经观看的广告次数")
    public int watchAdCount;
    @ProtoDesc("cd是否清零")
    public boolean cdZero;
    @ProtoDesc("技能等级限制条件是否通过")
    public boolean skillConditionPass;
    @ProtoDesc("交互次数  如果是运营部则为游客生成，其他建筑为交互次数")
    public int interactCount;
    @ProtoDesc("游客品质出现概率")
    public List<KVInfo> guestQualityList;
}
