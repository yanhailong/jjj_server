package com.jjg.game.core.task.pb;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * 任务条件
 */
@ProtobufMessage
@ProtoDesc("任务条件")
public class TaskCondition {

    /**
     * 配置id
     */
    @ProtoDesc("配置id")
    private int configId;

    /**
     * 进度
     */
    @ProtoDesc("进度")
    private long progress;

    /**
     * 配置参数
     */
    @ProtoDesc("配置参数")
    private long configParam;

    /**
     * 是否已经完成
     */
    @ProtoDesc("是否已经完成")
    private boolean isFinish;

    public int getConfigId() {
        return configId;
    }

    public void setConfigId(int configId) {
        this.configId = configId;
    }

    public long getProgress() {
        return progress;
    }

    public void setProgress(long progress) {
        this.progress = progress;
    }

    public long getConfigParam() {
        return configParam;
    }

    public void setConfigParam(long configParam) {
        this.configParam = configParam;
    }

    public boolean isFinish() {
        return isFinish;
    }

    public void setFinish(boolean finish) {
        isFinish = finish;
    }
}
