package com.jjg.game.alliance.pb.struct;

import com.jjg.game.common.pb.ItemInfo;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * 联盟任务运行时信息。静态配置字段由客户端读取 task.xlsx。
 *
 * @author 11
 * @date 2026/6/11
 */
@ProtobufMessage
@ProtoDesc("联盟任务信息")
public class AllianceTaskInfo {
    @ProtoDesc("任务配置id")
    public int cfgId;
    @ProtoDesc("当前进度(池中任务为0)")
    public long progress;
    @ProtoDesc("完成截止时间")
    public long expireTime;
    @ProtoDesc("任务目标")
    public long target;
    @ProtoDesc("奖励")
    public List<ItemInfo> rewards;
    @ProtoDesc("多语言id")
    public int langId;
    @ProtoDesc("品质")
    public int quality;
    @ProtoDesc("是否允许放弃")
    public boolean abandon;
    @ProtoDesc("条件id")
    public int conditionId;
    @ProtoDesc("持续时间(min)")
    public int duration;
    @ProtoDesc("开始时间")
    public long beginTime;
    @ProtoDesc("结束时间")
    public long endTime;
}
