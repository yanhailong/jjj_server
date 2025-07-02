package com.jjg.game.common.timer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.*;

/**
 * 抽象timer管理器
 *
 * @author 2CL
 */
public class BaseTimerCenter<T extends TimerEvent<?>> extends Thread {

    private Logger log = LoggerFactory.getLogger(getClass());

    /**
     * 定时器事件数组
     */
    protected List<T> array = new CopyOnWriteArrayList<>();

    /**
     * 活动标志
     */
    protected volatile boolean active;

    /**
     * 运行时间
     */
    private int runTime = 10;

    private int collateInterval = 2 * 60 * 1000;

    private long lastCollateTime = 0L;

    /**
     * 定时事件执行线程池
     */
    private ThreadPoolExecutor timerService;

    private int corePoolSize;
    private int maximumPoolSize;
    private int queueSize;

    public BaseTimerCenter(String timerName) {
        super(timerName);
    }

    public BaseTimerCenter(String timerName, int corePoolSize, int maximumPoolSize, int queueSize) {
        super(timerName);
        this.corePoolSize = corePoolSize;
        this.maximumPoolSize = maximumPoolSize;
        this.queueSize = queueSize;
    }

    protected void begin() {
        active = true;
        //创建线程池
        if (corePoolSize > 0 && maximumPoolSize > 0 && queueSize > 0) {
            timerService = new ThreadPoolExecutor(corePoolSize, maximumPoolSize, 10L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(queueSize), new ThreadPoolExecutor.CallerRunsPolicy());
        } else {
            timerService = new ThreadPoolExecutor(1, Runtime.getRuntime().availableProcessors() + 1, 10L,
                TimeUnit.SECONDS, new ArrayBlockingQueue<>(8), new ThreadPoolExecutor.CallerRunsPolicy());
        }

    }


    /**
     * 判断是否包含指定的对象
     */
    public boolean contain(T e) {
        return array.contains(e);
    }

    public List<? extends T> getArray() {
        return array;
    }

    public ExecutorService getTimerService() {
        return timerService;
    }

    public void setArray(List<T> array) {
        this.array = array;
    }

    public void setTimerService(ThreadPoolExecutor timerService) {
        this.timerService = timerService;
    }

    /**
     * 获得指定监听器和动作参数的定时器事件（如果多个则返回第一个）
     */
    public T get(TimerListener<?> listener, Object parameter) {
        for (T ev : array) {
            if (listener != ev.getTimerListener()) {
                continue;
            }
            if (parameter == null || parameter.equals(ev.getParameter())) {
                return ev;
            }
        }
        return null;
    }

    /**
     * 加上一个定时器事件
     */
    public void add(T event) {
        array.remove(event);
        event.init();
        array.add(event);
    }

    /**
     * 移除指定的定时器事件
     */
    public void remove(T e) {
        if (e != null) {
            array.remove(e);
        }
    }

    /**
     * 移除指定定时事件监听器的定时器事件，包括所有的事件动作参数
     */
    public void remove(TimerListener<?> listener) {
        remove(listener, null);
    }

    /**
     * 移除带有指定的定时事件监听器和事件动作参数的定时器事件
     */
    public void remove(TimerListener<?> listener, Object parameter) {
        for (T ev : array) {
            if (listener != ev.getTimerListener()) {
                continue;
            }
            if (parameter == null || parameter.equals(ev.getParameter())) {
                array.remove(ev);
            }
        }
    }

    public void run() {
        begin();
        while (active) {
            try {
                long currentTime = System.currentTimeMillis();
                fire(currentTime);
                if (currentTime - lastCollateTime > collateInterval) {
                    collate();
                    lastCollateTime = currentTime;
                }
                sleep(runTime);
            } catch (Exception e) {
                log.warn("", e);
            }
        }
    }

    /**
     * 通知所有定时器事件，检查是否要引发定时事件
     */
    public void fire(long time) {
        for (T ev : array) {
            if (ev == null || ev.count <= 0 || !ev.getEnabled()) {
                array.remove(ev);
            } else if (ev.inFire || timerService.getQueue().contains(ev)) {
                continue;
            } else if (time >= ev.nextTime) {
                ev.inFire = true;
                timerService.execute(ev);
            }
        }
    }

    public void collate() {
        // 获得当前线程池中执行的任务队列长度
        int queueSize = timerService.getQueue().size();
        // 获得当前线程池中的线程数
        int poolSize = timerService.getPoolSize();
        // 获取曾经同时位于池中的最大线程数
        int largestPoolSize = timerService.getLargestPoolSize();
        // 可使用的最大线程数
        int maxPoolSize = timerService.getMaximumPoolSize();
        log.debug("collate millis thread pool,queueSize={},poolSize={},largestPoolSize={},maxPoolSize={}", queueSize,
            poolSize, largestPoolSize, maxPoolSize);
    }

    public void close() {
        active = false;
        clear();
        if (timerService != null) {
            timerService.shutdown();
        }
    }

    /**
     * 清理方法
     */
    public void clear() {
        array.clear();
    }

    public int getRunTime() {
        return runTime;
    }
}
