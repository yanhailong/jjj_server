package com.jjg.game.sim.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.jjg.game.sim.data.AbstractData;
import com.jjg.game.sim.data.SimBaseData;
import com.jjg.game.sim.data.SimCasinoData;
import com.jjg.game.sim.data.SimPlayerContext;
import com.jjg.game.sim.listener.SimPlayerTickListener;
import com.jjg.game.season.data.SeasonPlayerData;
import com.mongodb.client.model.ReplaceOptions;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 玩家数据自动落库。
 *
 * @author 11
 * @date 2026/5/26
 */
@Service
public class SimAutoSaveService implements SimPlayerTickListener {
    private static final Logger log = LoggerFactory.getLogger(SimAutoSaveService.class);

    /**
     * 同一玩家最小落库间隔: 5 分钟
     */
    private static final long SAVE_INTERVAL_MS = 5 * 60 * 1000L;
    private static final int SAVE_QUEUE_CAPACITY = 4096;

    @Autowired
    private MongoTemplate mongoTemplate;

    //落库 IO 线程 (与玩家逻辑线程隔离; 单线程保证同一文档的写入严格 FIFO, 避免旧快照覆盖新值)
    private ExecutorService ioExecutor;

    public void init() {
        this.ioExecutor = new ThreadPoolExecutor(
                1, 1, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(SAVE_QUEUE_CAPACITY),
                r -> {
                    Thread t = new Thread(r, "sim-save-io");
                    t.setDaemon(true);
                    return t;
                },
                SimAutoSaveService::blockUntilQueued);
    }

    /**
     * 按玩家稳定散列首次保存检查时间，避免集中登录或重启后在同一个 tick
     * 对所有玩家同时执行全量快照比较。
     */
    public void initializeAutoSaveSchedule(SimPlayerContext ctx, long now) {
        if (ctx.getLastSaveTime() != 0) {
            return;
        }
        long delay = initialSaveDelayMs(ctx.playerId());
        ctx.setLastSaveTime(now - SAVE_INTERVAL_MS + delay);
    }

    private static long initialSaveDelayMs(long playerId) {
        long value = playerId + 0x9E3779B97F4A7C15L;
        value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
        value ^= value >>> 31;
        return Math.floorMod(value, SAVE_INTERVAL_MS);
    }

    /** 队列满时阻塞生产者并按原顺序入队，避免无界堆积或旧快照覆盖新值。 */
    private static void blockUntilQueued(Runnable task, ThreadPoolExecutor executor) {
        if (executor.isShutdown()) {
            throw new RejectedExecutionException("sim save executor is shutdown");
        }
        try {
            executor.getQueue().put(task);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RejectedExecutionException("interrupted while enqueueing sim save", e);
        }
    }

    public void destroy() {
        if (ioExecutor != null) {
            ioExecutor.shutdown();
            try {
                while (!ioExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                    log.warn("等待 sim 落库 IO 线程退出");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public void onTick(SimPlayerContext ctx, long now) {
        SimBaseData base = ctx.getSimBaseData();
        if (base == null) {
            return;
        }
        //节流: 距上次落库检查不够间隔则跳过 (业务置 0 可强制下个 tick 立即检查)
        if (ctx.getLastSaveTime() > 0 && now - ctx.getLastSaveTime() < SAVE_INTERVAL_MS) {
            return;
        }
        //无论是否有变更都推进检查时间: 否则数据不变的玩家每个 tick 都全量序列化比对, 空转 CPU;
        //写库失败的数据 savedHash 保持脏, 下个检查周期自然重试
        ctx.setLastSaveTime(now);

        enqueueIfChanged(base);
        SimCasinoData casino = ctx.getCurrentCasino();
        if (casino != null) {
            enqueueIfChanged(casino);
        }
        if (ctx.getSimTaskData() != null) {
            enqueueIfChanged(ctx.getSimTaskData());
        }
        if (ctx.getSimCoopTaskData() != null) {
            enqueueIfChanged(ctx.getSimCoopTaskData());
        }
        SeasonPlayerData seasonData = ctx.getSeasonPlayerData();
        if (seasonData != null) {
            enqueueIfChanged(seasonData);
        }
        for (AbstractData employee : ctx.getEmployeeMap().values()) {
            enqueueIfChanged(employee);
        }
        for (AbstractData skills : ctx.getSkillsDataMap().values()) {
            enqueueIfChanged(skills);
        }
    }

    /**
     * 业务在关键节点主动落库的入口: 与周期性自动落库走同一 IO 线程,
     * 保证同一文档的写入严格 FIFO, 避免业务同步写与自动落库的异步旧快照互相覆盖。
     *
     * @return 是否有变更被提交落库
     */
    public boolean enqueueSave(AbstractData data) {
        return enqueueIfChanged(data);
    }

    /**
     * 在落库 IO 线程上执行任务, 与之前提交的落库写严格 FIFO;
     * 用于"数据落库完成后再删除源记录"这类顺序依赖 (如赛季待结算队列的消费删除)。
     */
    public void enqueueTask(Runnable task) {
        ExecutorService ex = this.ioExecutor;
        if (ex == null || ex.isShutdown()) {
            task.run();
            return;
        }
        ex.execute(() -> {
            try {
                task.run();
            } catch (Exception e) {
                log.error("落库 IO 线程任务执行失败", e);
            }
        });
    }

    /**
     * 玩家线程内: 生成"将落库内容"快照并算哈希; 内容变化则把不可变快照交给 IO 线程异步写库。
     *
     * @return 是否有变更被提交落库
     */
    private boolean enqueueIfChanged(AbstractData data) {
        //保证联合主键就绪, 快照里才有正确的 _id
        data.buildKey();
        Document snapshot = new Document();
        mongoTemplate.getConverter().write(data, snapshot);
        long hash = snapshotHash(snapshot);
        if (hash == data.getSavedHash()) {
            return false;
        }

        String collection = mongoTemplate.getCollectionName(data.getClass());
        Object id = snapshot.get("_id");
        ExecutorService ex = this.ioExecutor;
        if (ex == null || ex.isShutdown()) {
            //init 前或关闭后退化为同步写, 保证不丢
            writeSnapshot(collection, id, snapshot, data, hash);
            return true;
        }
        ex.execute(() -> writeSnapshot(collection, id, snapshot, data, hash));
        return true;
    }

    private void writeSnapshot(String collection, Object id, Document snapshot, AbstractData data, long hash) {
        try {
            mongoTemplate.getCollection(collection)
                    .replaceOne(new Document("_id", id), snapshot, new ReplaceOptions().upsert(true));
            //写库成功后再记录哈希 (volatile 跨线程可见); 失败则保持脏, 下个周期重试
            data.markSaved(hash);
        } catch (Exception e) {
            log.error("异步落库失败 collection={},id={}", collection, id, e);
        }
    }

    /**
     * 阻塞等待已提交的异步写库全部完成 (单线程 FIFO, 提交一个空屏障并等它执行完即可)。
     * 供退出/关服在最终落库前调用, 确保不会有旧快照在其后覆盖最新值。
     */
    public void awaitPending() {
        ExecutorService ex = this.ioExecutor;
        if (ex == null || ex.isShutdown()) {
            return;
        }
        try {
            ex.submit(() -> {
            }).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("等待异步落库完成被中断", e);
        } catch (Exception e) {
            log.warn("等待异步落库完成异常", e);
        }
    }

    /**
     * FNV-1a 64 位哈希, 内容相同则哈希相同
     */
    private static long fnv1a64(String s) {
        long hash = 0xcbf29ce484222325L;
        for (int i = 0, len = s.length(); i < len; i++) {
            hash ^= s.charAt(i);
            hash *= 0x100000001b3L;
        }
        return hash;
    }

    static long snapshotHash(Document snapshot) {
        return fnv1a64(JSON.toJSONString(snapshot, SerializerFeature.MapSortField));
    }

    /**
     * 最后执行: 让所有业务 tick 都跑完, 再统一刷盘
     */
    @Override
    public int order() {
        return Integer.MAX_VALUE;
    }
}
