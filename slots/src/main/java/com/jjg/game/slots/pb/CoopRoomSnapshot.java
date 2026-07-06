package com.jjg.game.slots.pb;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * 协作房间全量快照 (进房/低频变更广播用)。
 *
 * @author 11
 * @date 2026/7/6
 */
@ProtobufMessage
@ProtoDesc("协作房间快照")
public class CoopRoomSnapshot {
    @ProtoDesc("房间id")
    public long roomId;
    @ProtoDesc("任务配置id")
    public int taskId;
    @ProtoDesc("房主id")
    public long ownerId;
    @ProtoDesc("游戏类型")
    public int gameType;
    @ProtoDesc("状态 0等待 1进行中 2已结算")
    public int status;
    @ProtoDesc("成员列表 (座位序)")
    public List<CoopMemberInfo> members;
    @ProtoDesc("总人数上限")
    public int maxMembers;
    @ProtoDesc("共享特殊事件目标")
    public int sharedTarget;
    @ProtoDesc("共享特殊事件累计")
    public int sharedProgress;
    @ProtoDesc("任务截止时间 (开始后有效, 0=不限, ms)")
    public long deadline;
}
