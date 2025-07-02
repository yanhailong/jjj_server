package com.jjg.game.common.concurrent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.concurrent.ThreadFactory;

/**
 * 线程工厂
 *
 * @author 2CL
 */
public class DefaultThreadFactory implements ThreadFactory {
    /**
     * logger
     */
    private final Logger logger = LoggerFactory.getLogger(DefaultThreadFactory.class);
    /**
     * 线程名字 *
     */
    private final String threadName;

    /**
     * 线程所属的组 *
     */
    private final ThreadGroup group;

    /**
     * 是否是守护线程 *
     */
    private boolean daemon;

    public DefaultThreadFactory(String name) {
        group = Thread.currentThread().getThreadGroup();
        this.threadName = name;
    }

    public DefaultThreadFactory(String name, boolean daemon) {
        this(name);
        this.daemon = daemon;
    }

    @Override
    public Thread newThread(@Nonnull Runnable r) {
        Thread t = new Thread(group, r, threadName, 0);
        t.setDaemon(daemon);
        if (t.isDaemon()) {
            logger.warn("create a daemon Thread, threadName:{}", threadName);
        }
        if (t.getPriority() != Thread.NORM_PRIORITY) {
            t.setPriority(Thread.NORM_PRIORITY);
        }
        return t;
    }
}
