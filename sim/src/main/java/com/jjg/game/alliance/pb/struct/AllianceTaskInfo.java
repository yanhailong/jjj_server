package com.jjg.game.alliance.pb.struct;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

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
    @ProtoDesc("截止时间(ms): 池中为可接取截止, 已接取为完成截止")
    public long expireTime;
}
