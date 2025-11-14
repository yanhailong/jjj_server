package com.jjg.game.common.data;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @author 11
 * @date 2025/11/13 19:30
 */
public class MessageStat {
    private final AtomicLong totalTime = new AtomicLong(0);
    private final AtomicLong count = new AtomicLong(0);
    private final AtomicLong lastLogTime = new AtomicLong(0);

    public void addExecution(long duration) {
        totalTime.addAndGet(duration);
        count.incrementAndGet();
    }

    public long getAverageTime() {
        long countVal = count.get();
        return countVal == 0 ? 0 : totalTime.get() / countVal;
    }

    public long getCount() {
        return count.get();
    }

    public long getTotalTime() {
        return totalTime.get();
    }

    public AtomicLong getLastLogTime() {
        return lastLogTime;
    }
}
