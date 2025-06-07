package com.vegasnight.game.common.timer;

import com.vegasnight.game.common.executor.MarsWorkExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.*;

/**
 * 系统公共 定时器 or 线程池
 * @since 1.0
 */
public class TimerCenter extends Thread {

    private Logger log = LoggerFactory.getLogger(getClass());

    /**
     * 定时器事件数组
     */
    private List<TimerEvent> array = new CopyOnWriteArrayList<TimerEvent>();

    /**
     * 活动标志
     */
    volatile boolean active;

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
    /* 线性业务执行器*/
    @Autowired(required = false)
    private MarsWorkExecutor workExecutor;

    private int corePoolSize;
    private int maximumPoolSize;
    private int queueSize;

    public TimerCenter(String timerName) {
        super(timerName);
    }

    public TimerCenter(String timerName,int corePoolSize,int maximumPoolSize,int queueSize) {
        super(timerName);
        this.corePoolSize = corePoolSize;
        this.maximumPoolSize = maximumPoolSize;
        this.queueSize = queueSize;
    }

    private void begin() {
        active = true;
        //创建线程池
        if(corePoolSize > 0 && maximumPoolSize > 0 && queueSize > 0){
            timerService = new ThreadPoolExecutor(corePoolSize,maximumPoolSize, 10L,TimeUnit.SECONDS, new ArrayBlockingQueue<>(queueSize),new ThreadPoolExecutor.CallerRunsPolicy());
        }else {
            timerService = new ThreadPoolExecutor(1,Runtime.getRuntime().availableProcessors() + 1, 10L,TimeUnit.SECONDS, new ArrayBlockingQueue<>(8),new ThreadPoolExecutor.CallerRunsPolicy());
        }
    }


    /**
     * 判断是否包含指定的对象
     */
    public boolean contain(TimerEvent e) {
        return array.contains(e);
    }

    public List<TimerEvent> getArray() {
        return array;
    }

    public ExecutorService getTimerService() {
        return timerService;
    }

    public void setArray(List<TimerEvent> array) {
        this.array = array;
    }

    public void setTimerService(ThreadPoolExecutor timerService) {
        this.timerService = timerService;
    }

    /**
     * 获得指定监听器和动作参数的定时器事件（如果多个则返回第一个）
     */
    public TimerEvent get(TimerListener listener, Object parameter) {
        for (TimerEvent ev : array) {
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
    public void add(TimerEvent e) {
        if (array.contains(e)) {
            array.remove(e);
        }
        e.init();
        array.add(e);
    }

    /**
     * 移除指定的定时器事件
     */
    public void remove(TimerEvent e) {
        if (e != null) {
            array.remove(e);
        }
    }

    /**
     * 移除指定定时事件监听器的定时器事件，包括所有的事件动作参数
     */
    public void remove(TimerListener listener) {
        remove(listener, null);
    }

    /**
     * 移除带有指定的定时事件监听器和事件动作参数的定时器事件
     */
    public void remove(TimerListener listener, Object parameter) {
        for (TimerEvent ev : array) {
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
        for (Iterator<TimerEvent> iter = array.iterator(); iter.hasNext(); ) {
            TimerEvent ev = iter.next();
            if (ev == null || ev.count <= 0 || !ev.getEnabled()) {
                array.remove(ev);
            } else if (ev.inFire || timerService.getQueue().contains(ev)) {
                continue;
            } else if (time >= ev.nextTime) {
                ev.inFire = true;
                if (workExecutor != null && ev.workId > 0) {
                    workExecutor.submit(ev.workId, ev);
                } else {
                    timerService.execute(ev);
                }
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
        log.debug("collate millis thread pool,queueSize={},poolSize={},largestPoolSize={},maxPoolSize={}",queueSize,poolSize,largestPoolSize,maxPoolSize);
    }

    public void close() {
        active = false;
        clear();
        timerService.shutdown();
    }

    /**
     * 清理方法
     */
    public void clear() {
        array.clear();
    }
}
