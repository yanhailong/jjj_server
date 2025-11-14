package com.jjg.game.common.concurrent;

/**
 * @author lm
 * @date 2025/11/14 11:38
 */

public final class PlayerEvent {
    private int msgId;
    private BaseHandler<?> task;
    private long publishTimeNanos;

    public void set(int msgId, BaseHandler<?> task, long publishTimeNanos) {
        this.msgId = msgId;
        this.task = task;
        this.publishTimeNanos = publishTimeNanos;
    }

    public BaseHandler<?> getTask() {
        return task;
    }

    public long getPublishTimeNanos() {
        return publishTimeNanos;
    }

    public int getMsgId() {
        return msgId;
    }

    public void clear() {
        this.msgId = 0;
        this.task = null;
        this.publishTimeNanos = 0L;
    }
}
