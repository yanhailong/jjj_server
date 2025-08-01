package com.jjg.game.common.concurrent;

import com.alibaba.fastjson.JSON;
import com.jjg.game.common.concurrent.priority.BasePriorityHandler;
import com.jjg.game.common.concurrent.priority.BasePriorityHandlerRunnable;
import com.jjg.game.common.concurrent.priority.PlayerPriority;
import com.jjg.game.common.utils.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 线程基类
 *
 * @author 2CL
 */
public abstract class BaseProcessor implements IPrintTimeHandler {

    /**
     * 所有线程的执行数量
     */
    protected static final AtomicLong TOTAL_EXEC_NUM = new AtomicLong();
    /**
     * 当前Processor对应的线程
     */
    protected final ThreadPoolExecutor executor;

    protected Logger logger = LoggerFactory.getLogger(BaseProcessor.class);

    /**
     * 线程名字 *
     */
    protected final String name;

    /**
     * 线程队列
     */
    protected BlockingQueue<Runnable> blockingQueue;

    /**
     * 执行数量
     */
    public AtomicLong execAloneNum = new AtomicLong();

    /**
     * 创建数量
     */
    public AtomicLong createAloneNum = new AtomicLong();

    protected BaseHandler<?> currentHandler;

    /**
     * 单个handler的逻辑操作时间上限，超过时间将打印handler的信息
     */
    protected long doOverTimeThreshold = 200L;

    protected int logQueueSize = 200;

    protected long logWaitQueueTime = 2000L;

    protected long printQueueSizeTime = System.currentTimeMillis();

    protected boolean timeErrorPrint = false;

    private long printWaitTime = System.currentTimeMillis();

    public static final String LOG_DO_OVER_TIME = "[Do Overtime]--->";
    public static final String LOG_WAIT_OVER_TIME = "[Wait Overtime]--->";

    /**
     * 单线程
     *
     * @param name 线程池名
     */
    public BaseProcessor(String name) {
        this(name, 1, 1, 0L, false);
    }

    public BaseProcessor(String name, BlockingQueue<Runnable> blockingQueue) {
        this(name, 1, 1, 0L, false, blockingQueue);
    }

    /**
     * 固定大小线程池
     *
     * @param name        线程池名
     * @param threadCount 线程属相
     */
    public BaseProcessor(String name, int threadCount) {
        this(name, threadCount, threadCount, 0L, false);
    }

    /**
     * 线程池
     *
     * @param name         线程池名
     * @param corePoolSize 核心数量
     * @param maxPoolSize  最大数量
     */
    public BaseProcessor(String name, int corePoolSize, int maxPoolSize) {
        this(name, corePoolSize, maxPoolSize, 0L, false);
    }

    /**
     * 线程池
     *
     * @param name                   线程池名
     * @param corePoolSize           核心数量
     * @param maxPoolSize            最大数量
     * @param keepTime               保持时间
     * @param allowCoreThreadTimeOut 是否允许核心线程超时
     */
    public BaseProcessor(
        String name,
        int corePoolSize,
        int maxPoolSize,
        long keepTime,
        boolean allowCoreThreadTimeOut,
        BlockingQueue<Runnable> blockingQueue) {
        // 线程名字
        this.name = name;

        this.blockingQueue = Objects.requireNonNullElseGet(blockingQueue, LinkedBlockingQueue::new);

        // 线程实例
        this.executor =
            new ThreadPoolExecutor(
                corePoolSize,
                maxPoolSize,
                keepTime,
                TimeUnit.MILLISECONDS,
                this.blockingQueue,
                new DefaultThreadFactory(name));

        // 是否允许核心线程超时
        this.executor.allowCoreThreadTimeOut(allowCoreThreadTimeOut);

        // 启动所有核心线程，使其处于等待工作的空闲状态。仅当执行新任务时，此操作才重写默认的启动核心线程策略。
        this.executor.prestartAllCoreThreads();
    }

    public BaseProcessor(
        String name,
        int corePoolSize,
        int maxPoolSize,
        long keepTime,
        boolean allowCoreThreadTimeOut) {
        this(name, corePoolSize, maxPoolSize, keepTime, allowCoreThreadTimeOut, null);
    }

    public int getQueueSize() {
        return this.blockingQueue.size();
    }

    /**
     * 获取队列信息（handler名：数量）
     */
    public String getQueueInfo() {
        String info = "[]";
        HashMap<String, Integer> hashMap = hashHandlerCount();
        if (!hashMap.isEmpty()) {
            info = JSON.toJSONString(hashMap);
        }
        return info;
    }

    private HashMap<String, Integer> hashHandlerCount() {
        HashMap<String, Integer> hashMap = new HashMap<>();
        for (Runnable runnable : this.blockingQueue) {
            BaseHandlerRunnable baseHandlerRunnable = (BaseHandlerRunnable) runnable;
            String name = baseHandlerRunnable.getHandler().getClass().getSimpleName();
            if (hashMap.containsKey(name)) {
                hashMap.put(name, hashMap.get(name) + 1);
            } else {
                hashMap.put(name, 1);
            }
        }
        return hashMap;
    }

    /**
     * 获取队列信息（handler名: 数量）
     */
    public HashMap<String, Integer> getQueueHandlerMap() {
        return hashHandlerCount();
    }

    /**
     * 获取队列信息（handler名：数量）
     */
    public String getHandlers() {
        String info = "[]";
        ArrayList<String> list = new ArrayList<>();
        int i = 0;
        for (Runnable runnable : this.blockingQueue) {
            i++;
            if (i > 1000) {
                break;
            }
            BaseHandlerRunnable baseHandlerRunnable = (BaseHandlerRunnable) runnable;
            String name = baseHandlerRunnable.getHandler().getClass().getSimpleName();
            list.add(name);
        }
        if (!list.isEmpty()) {
            info = JSON.toJSONString(list);
        }

        Runnable runnable = blockingQueue.peek();
        if (runnable != null) {
            info += ("first runnable:" + runnable.getClass().getSimpleName());
        }

        return info;
    }

    /**
     * 当前正在运行的handler
     */
    public String getCurrentHandler() {
        return currentHandler != null ? currentHandler.getClass().getSimpleName() : null;
    }

    /**
     * 当前正在运行的handler
     */
    public BaseHandler<?> getCurrentHandlerObj() {
        return currentHandler;
    }

    /**
     * 向线程池投递带有返回值的handler
     */
    protected Future<String> submitHandler(BaseHandler<?> handler) {
        try {
            if (executor.isShutdown()) {
                logger.error("线程:{} 已经停止,Handler被丢弃: {}", this.name, handler.getClass().getName());
                return null;
            }

            handler.setTime(System.currentTimeMillis());
            handler.setCreateAloneNum(createAloneNum.incrementAndGet());
            handler.setAloneNum(execAloneNum.get());

            return executor.submit(
                new Callable<String>() {
                    private BaseHandler<?> handler;

                    public Callable<String> setHandler(BaseHandler<?> handler) {
                        this.handler = handler;
                        return this;
                    }

                    @Override
                    public String call() {
                        try {
                            long callBegin = System.currentTimeMillis();
                            long callWaitTime = callBegin - handler.getTime();
                            currentHandler = handler;
                            handler.action();

                            long callDoTime = System.currentTimeMillis() - callBegin;
                            printDoTime(handler, callDoTime, "");
                            printWaitTime(handler, callDoTime, callWaitTime, "submit call way");

                            TOTAL_EXEC_NUM.incrementAndGet();
                            execAloneNum.incrementAndGet();
                        } catch (Exception ex) {
                            String exStr = ExceptionUtils.e2s(ex);
                            logger.error(exStr);
                            handler.setResult(exStr);
                        }

                        return handler.getResult();
                    }
                }.setHandler(handler));
        } catch (Exception e) {
            logger.error(ExceptionUtils.e2s(e));
            return null;
        }
    }

    /**
     * 向该线程投递Handler(Runnable)
     */
    protected void executeHandler(BaseHandler<?> handler) {
        try {
            if (executor.isShutdown()) {
                logger.warn("线程{} 已经停止, Handler被丢弃: {}", this.name, handler.getClass().getName());
                return;
            }
            handler.setTime(System.currentTimeMillis());
            handler.setCreateAloneNum(createAloneNum.incrementAndGet());
            handler.setAloneNum(execAloneNum.get());

            if (handler instanceof BasePriorityHandler) {
                executor.execute(
                    new BasePriorityHandlerRunnable(((BasePriorityHandler<?>) handler).getPriority()) {
                        @Override
                        public void run() {
                            execHandler(this.handler, "BasePriorityHandlerRunnable way");
                            TOTAL_EXEC_NUM.incrementAndGet();
                            execAloneNum.incrementAndGet();
                        }
                    }.setHandler(handler));
            } else {
                executor.execute(
                    new BaseHandlerRunnable() {
                        @Override
                        public void run() {
                            execHandler(this.handler, "BaseHandlerRunnable way");
                            TOTAL_EXEC_NUM.incrementAndGet();
                            execAloneNum.incrementAndGet();
                        }
                    }.setHandler(handler));
            }

        } catch (Exception e) {
            logger.error(ExceptionUtils.e2s(e));
        }
    }

    /**
     * 向该线程投递Handler(Runnable)
     */
    public void executeHandlerImmediately(IProcessorHandler handler) {
        try {
            if (executor.isShutdown()) {
                logger.error("线程{} 已经停止, Handler被丢弃: {}", this.name, handler.getClass().getName());
                return;
            }
            BaseHandler<?> baseHandler = (BaseHandler<?>) handler;
            baseHandler.setTime(System.currentTimeMillis());
            baseHandler.setCreateAloneNum(createAloneNum.incrementAndGet());
            baseHandler.setAloneNum(execAloneNum.get());
            execHandler(baseHandler, "executeHandlerImmediately way");
            TOTAL_EXEC_NUM.incrementAndGet();
            execAloneNum.incrementAndGet();
        } catch (Exception e) {
            logger.error(ExceptionUtils.e2s(e));
        }
    }

    /**
     * 执行投递到该线程中的handler
     */
    protected void execHandler(BaseHandler<?> handler, String callWay) {
        try {
            if (handler != null) {
                currentHandler = handler;

                long execBegin = System.currentTimeMillis();
                long execWaitTime = execBegin - handler.getTime();
                if (!beforeHandler(handler)) {
                    return;
                }
                handler.action();
                long execDoTime = System.currentTimeMillis() - execBegin;
                printDoTime(handler, execDoTime, "");
                printWaitTime(handler, execDoTime, execWaitTime, callWay);
            } else {
                throw new RuntimeException("向线程投递了NULL的Handler:" + this.name);
            }
        } catch (Exception e) {
            exceptionHandler(handler, e);
        } finally {
            // 因为在finally中,需要对后置逻辑处理进行容错
            try {
                afterHandler(handler);
            } catch (Exception e) {
                logger.error("处理handler后置逻辑时异常\n" + ExceptionUtils.e2s(e));
            }
            if (handler instanceof IAfterExecHandler) {
                ((IAfterExecHandler) handler).afterAction();
            }
        }

        int size = this.blockingQueue.size();
        this.printQueueSize(handler, size);
    }

    @Override
    public void printQueueSize(BaseHandler<?> handler, int size) {
        if (size < this.logQueueSize) {
            return;
        }
        // 持续超过阈值
        long now = System.currentTimeMillis();
        if ((now - logWaitQueueTime) < this.printQueueSizeTime) {
            return;
        }
        this.printQueueSizeTime = now;
        if (timeErrorPrint) {
            logger.error("线程队列长度超过警戒值:{} {}", size, this.name);
        } else {
            logger.warn("线程队列长度超过警戒值:{} {}", size, this.name);
        }
    }

    @Override
    public void printWaitTime(BaseHandler<?> handler, long logDoTime, long waitTime, String callWay) {
        if (waitTime < this.logWaitQueueTime) {
            return;
        }
        // 间隔打印
        long now = System.currentTimeMillis();
        if ((now - this.logWaitQueueTime) < this.printWaitTime) {
            return;
        }
        this.printWaitTime = now;
        int priority = -1;
        if (handler instanceof BasePriorityHandler<?> priorityHandler) {
            PlayerPriority playerPriority = priorityHandler.getPriority();
            if (playerPriority != null) {
                priority = playerPriority.getPriority();
            }
        }

        String printContent =
            LOG_WAIT_OVER_TIME
                + " threadName:"
                + this.name
                + ",handler:"
                + handler.getClass().getName()
                + ",doTime:"
                + logDoTime
                + ",waitTime:"
                + waitTime
                + ",taskCount:"
                + this.executor.getTaskCount()
                + ",queueSize:"
                + this.blockingQueue.size()
                + ","
                + blockingQueue.getClass().getSimpleName()
                + ",callWay:"
                + callWay
                + ",priority:"
                + priority
                + ",createExecNum:"
                + handler.getAloneNum()
                + ",currentExecNum:"
                + execAloneNum.get()
                + ",createNum:"
                + handler.getCreateAloneNum()
                + ",createCurrentNum:"
                + createAloneNum.get();

        if (this.timeErrorPrint) {
            logger.error(printContent);
        } else {
            logger.warn(printContent);
        }
    }

    @Override
    public void printDoTime(BaseHandler<?> handler, long doTime, String info) {
        if (doTime < doOverTimeThreshold) {
            return;
        }
        String printContent =
            LOG_DO_OVER_TIME
                + " threadName:"
                + this.name
                + ",handler:"
                + handler.getClass().getName()
                + "handlerParameter"
                + handler.getHandlerParam()
                + ",doTime:"
                + doTime
                + ",queueSize:"
                + this.blockingQueue.size()
                + ",info:"
                + info;
        if (this.timeErrorPrint || (doTime > 2 * doOverTimeThreshold)) {
            logger.error(printContent);
        } else {
            logger.warn(printContent);
        }
    }

    /**
     * 具体游戏逻辑内部对前置条件的处理
     *
     * @param handler 执行handler
     */
    protected boolean beforeHandler(BaseHandler<?> handler) {
        return true;
    }

    /**
     * 具体游戏逻辑内部对后置条件的处理
     */
    protected void afterHandler(BaseHandler<?> handler) throws Exception {
        // do nothing
    }

    /**
     * 逻辑内部对异常的处理
     */
    protected void exceptionHandler(BaseHandler<?> handler, Exception e) {
        logger.error(
            "Handler异常: {} {}",
            handler != null ? handler.getClass().getName() : null,
            ExceptionUtils.e2s(e));
    }

    /**
     * 线程是否已关闭
     *
     * <p>如果isCompleteAllCommand为true, 则已经执行过stop, 并且已提交的命令全部执行完的情况下返回true
     *
     * @param isCompleteAllCommand 是否完成所有任务
     */
    public boolean isStop(boolean isCompleteAllCommand) {
        if (isCompleteAllCommand) {
            return executor.isTerminated();
        } else {
            return executor.isShutdown();
        }
    }

    /**
     * 关闭线程池
     */
    public void stop() {
        stop(true);
    }

    /**
     * 关闭线程池
     *
     * @param await 是否等待队列全部执行完成
     */
    private void stop(boolean await) {
        if (await) {
            executor.shutdown();
        } else {
            executor.shutdownNow();
        }

        int count = 0;
        try {
            while (!this.executor.isTerminated()) {
                logger.info(
                    "Thread Stopping: {} 执行未结束..... 剩余队列长度: {}", this.name, this.blockingQueue.size());

                if (count > 1000) {
                    logger.error(
                        "Thread Stopping: {} 执行未结束 当前队列任务:{},handlers:{},currentHandler:{}",
                        this.name,
                        getQueueInfo(),
                        getHandlers(),
                        getCurrentHandler());
                } else if (count > 100) {
                    logger.warn(
                        "Thread Stopping: {} 执行未结束 当前队列任务:{},handlers:{},currentHandler:{}",
                        this.name,
                        getQueueInfo(),
                        getHandlers(),
                        getCurrentHandler());
                }

                this.executor.awaitTermination(1L, TimeUnit.SECONDS);
                count++;
            }
            logger.info(
                "Thread Stopped: {} 执行结束. 当前队列长度: {}", this.name, this.blockingQueue.size());
        } catch (Exception e) {
            logger.error(ExceptionUtils.e2s(e));
        }
    }

    public String getName() {
        return name;
    }

    public long getAloneNum() {
        return execAloneNum.get();
    }

    public static long getTotalExecNum() {
        return TOTAL_EXEC_NUM.get();
    }

    /**
     * 是否设置允许核心线程超时（为true且keepTime>0时，会移除超时的核心线程）
     */
    protected void allowCoreThreadTimeOut(boolean value) {
        executor.allowCoreThreadTimeOut(value);
    }
}
