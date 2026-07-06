package com.jjg.game.sim.pb.struct;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * 多人任务单条信息 (静态内容客户端依配置自取)。
 *
 * @author 11
 * @date 2026/7/6
 */
@ProtobufMessage
@ProtoDesc("多人任务信息")
public class CoopTaskInfo {
    @ProtoDesc("任务配置id")
    public int taskId;
    @ProtoDesc("状态 0未领取 1已领取待建房 2房间中 3待领奖 4失败")
    public int status;
    @ProtoDesc("已创建的房间id (未创建为0)")
    public long roomId;
    @ProtoDesc("创建房间时选择的游戏 (未创建为0)")
    public int gameType;
}
