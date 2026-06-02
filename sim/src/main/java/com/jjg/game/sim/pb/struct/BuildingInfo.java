package com.jjg.game.sim.pb.struct;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

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
    @ProtoDesc("主管id")
    public int managerEmployId;
    @ProtoDesc("进度条")
    public int progress;
}
