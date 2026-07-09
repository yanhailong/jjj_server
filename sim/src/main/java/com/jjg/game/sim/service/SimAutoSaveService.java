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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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

    @Autowired
    private MongoTemplate mongoTemplate;

    //落库 IO 线程 (与玩家逻辑线程隔离; 单线程保证同一文档的写入严格 FIFO, 避免旧快照覆盖新值)
    private ExecutorService ioExecutor;

    public void init() {
        this.ioExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "sim-save-io");
            t.setDaemon(true);
            return t;
        });
    }

    public void destroy() {
        if (ioExecutor != null) {
            ioExecutor.shutdown();
            try {
                ioExecutor.awaitTermination(10, TimeUnit.SECONDS);
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
        //节流: 距上次落库不够间隔则跳过
        if (ctx.getLastSaveTime() > 0 && now - ctx.getLastSaveTime() < SAVE_INTERVAL_MS) {
            return;
        }

        boolean enqueued = enqueueIfChanged(base);
        SimCasinoData casino = ctx.getCurrentCasino();
        if (casino != null) {
            enqueued |= enqueueIfChanged(casino);
        }
        if (ctx.getSimTaskData() != null) {
            enqueued |= enqueueIfChanged(ctx.getSimTaskData());
        }
        if (ctx.getSimCoopTaskData() != null) {
            enqueued |= enqueueIfChanged(ctx.getSimCoopTaskData());
        }
        SeasonPlayerData seasonData = ctx.getSeasonPlayerData();
        if (seasonData != null) {
            enqueued |= enqueueIfChanged(seasonData);
        }
        for (AbstractData employee : ctx.getEmployeeMap().values()) {
            enqueued |= enqueueIfChanged(employee);
        }
        for (AbstractData skills : ctx.getSkillsDataMap().values()) {
            enqueued |= enqueueIfChanged(skills);
        }

        if (enqueued) {
            ctx.setLastSaveTime(now);
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
        long hash = fnv1a64(JSON.toJSONString(snapshot, SerializerFeature.MapSortField));
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
     * 供退出/关服在同步全量落库前调用, 确保不会有旧快照在其后覆盖最新值。
     */
    public void awaitPending() {
        ExecutorService ex = this.ioExecutor;
        if (ex == null || ex.isShutdown()) {
            return;
        }
        try {
            ex.submit(() -> {
            }).get(5, TimeUnit.SECONDS);
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

    /**
     * 最后执行: 让所有业务 tick 都跑完, 再统一刷盘
     */
    @Override
    public int order() {
        return Integer.MAX_VALUE;
    }
}
