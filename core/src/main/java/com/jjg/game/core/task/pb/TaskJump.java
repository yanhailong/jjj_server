package com.jjg.game.core.task.pb;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * 任务跳转
 */
@ProtobufMessage
@ProtoDesc("任务跳转")
public class TaskJump {

    /**
     * 跳转类型
     */
    @ProtoDesc("跳转类型")
    private int type;

    /**
     * 跳转值
     */
    @ProtoDesc("跳转值")
    private int value;

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }
}
