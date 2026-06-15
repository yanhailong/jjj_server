package com.jjg.game.sim.pb.struct;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * 经营信息单条统计数据
 *
 * @author 11
 * @date 2026/6/15
 */
@ProtobufMessage
@ProtoDesc("经营信息统计数据")
public class StatInfo {
    @ProtoDesc("数据KEY")
    public int key;
    @ProtoDesc("数值")
    public long value;
    @ProtoDesc("最大值/总量 (用于 888/999 形式; 单值时为0)")
    public long max;

    public StatInfo() {
    }

    public StatInfo(int key, long value) {
        this.key = key;
        this.value = value;
    }

    public StatInfo(int key, long value, long max) {
        this.key = key;
        this.value = value;
        this.max = max;
    }
}
