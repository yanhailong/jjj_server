package com.jjg.game.slots.game.mahjiongwin.pb;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.KVInfo;

import java.util.List;

/**
 * @author 11
 * @date 2025/9/23 15:31
 */
@ProtobufMessage
@ProtoDesc("连续中奖触发倍数信息")
public class MahjiongwinAddTimesInfo {
    @ProtoDesc("状态  0.正常  1.免费")
    public int status;
    @ProtoDesc("倍数列表")
    public List<KVInfo> times;
}
