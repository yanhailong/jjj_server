package com.jjg.game.slots.handler;

import com.alibaba.fastjson.JSON;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.core.handler.CoreToServerMessageHandler;
import com.jjg.game.core.pb.KVInfo;
import com.jjg.game.core.pb.gm.NotifyGenrateLib;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.SpecialResultLibCfg;
import com.jjg.game.slots.data.GenerateLibTask;
import com.jjg.game.slots.manager.AbstractSlotsGameManager;
import com.jjg.game.slots.manager.SlotsFactoryManager;
import com.jjg.game.slots.pb.NoticeSlotsLibChange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author 11
 * @date 2025/7/23 17:31
 */
@Component
@MessageType(MessageConst.MessageTypeDef.TO_SERVER_CONST_TYPE)
public class SlotsToServerMessageHandler extends CoreToServerMessageHandler {
    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private SlotsFactoryManager slotsFactoryManager;

    // 生成任务队列（包含gameType和count信息）
    private final Queue<GenerateLibTask> generateTaskQueue = new LinkedList<>();
    // 每个 gameType 是否正在执行
    private final Set<Integer> runningGameTypes = Collections.newSetFromMap(new ConcurrentHashMap<>());
    // 正在执行的任务计数
    private final AtomicInteger runningTasks = new AtomicInteger(0);
    private final int MAX_CONCURRENT_TASKS = 2;

    // 锁
    private final Object queueLock = new Object();

    /**
     * 生成结果库
     *
     * @param req
     */
    @Command(MessageConst.ToServer.NOTICE_GENERATE_LIB)
    public void genrateLib(NotifyGenrateLib req) {
        try {
            log.info("收到生成结果库的请求 list={}", JSON.toJSONString(req.list));

            for (KVInfo info : req.list) {
                AbstractSlotsGameManager gameManager = slotsFactoryManager.getGameManager(info.key);
                if (gameManager == null) {
                    log.debug("获取 gameManager 为空，生成结果库失败 gameType = {},count = {}", info.key, info.value);
                    return;
                }

                // 任务入队
                Map<Integer, Integer> countMap = countMap(info.key, info.value);
                synchronized (queueLock) {
                    generateTaskQueue.offer(new GenerateLibTask(info.key, countMap));
                    log.info("任务已入队，当前队列长度: {}, gameType = {}", generateTaskQueue.size(), info.key);
                }
            }
            // 尝试启动任务
            tryStartNextTask();
        } catch (Exception e) {
            log.error("", e);
        }
    }

    /**
     * 结果库变更
     *
     * @param req
     */
    @Command(MessageConst.ToServer.NOTICE_SLOTS_LIB_CHANGE)
    public void reqConfigInfo(NoticeSlotsLibChange req) {
        try {
            log.info("收到结果库变化的通知消息 gameType = {},changeType = {}", req.gameType, req.changeType);
            AbstractSlotsGameManager gameManager = slotsFactoryManager.getGameManager(req.gameType);
            if (gameManager == null) {
                log.debug("获取 gameManager 为空，处理specialLib变化失败 gameType = {},changeType = {}", req.gameType, req.changeType);
                return;
            }

            List<SpecialResultLibCfg> cfgList = new ArrayList<>();
            if (req.changeType == 1) {
                req.libCfgList.forEach(str -> cfgList.add(JSON.parseObject(str, SpecialResultLibCfg.class)));
            }

            gameManager.notifySpecialResultLibCacheData(cfgList);
        } catch (Exception e) {
            log.error("", e);
        }
    }

    private Map<Integer, Integer> countMap(int gameType, int count) {
        Map<Integer, Integer> countMap = new HashMap<>();
        GameDataManager.getSpecialModeCfgMap().forEach((k, v) -> {
            if (v.getGameType() == gameType) {
                countMap.put(v.getType(), count);
            }
        });
        return countMap;
    }

    private void tryStartNextTask() {
        log.info("尝试开启新的任务");
        List<GenerateLibTask> tasksToStart = new ArrayList<>();

        synchronized (queueLock) {
            // 只从队列头取一个任务
            while (!generateTaskQueue.isEmpty() && runningTasks.get() < MAX_CONCURRENT_TASKS) {
                GenerateLibTask task = generateTaskQueue.poll();
                int gameType = task.getGameType();

                // 检查该 gameType 是否已有任务在运行
                if (runningGameTypes.contains(gameType)) {
                    log.debug("gameType = {} 已有任务在运行，故忽略该任务", gameType);
                    continue;  // 继续检查新的队头
                }

                // 可以启动这个任务
                tasksToStart.add(task);
                runningTasks.incrementAndGet();
                runningGameTypes.add(gameType);
                log.info("添加可执行任务 gameTpye = {}", gameType);
            }
        }

        for (GenerateLibTask task : tasksToStart) {
            CompletableFuture.runAsync(() -> {
                try {
                    log.info("开始执行生成任务，gameType = {}", task.getGameType());

                    AbstractSlotsGameManager gameManager = slotsFactoryManager.getGameManager(task.getGameType());

                    if (gameManager != null) {
                        // 直接调用 generate 方法
                        gameManager.generate(task.getLibTypeCountMap(), true);
//                        Thread.sleep(60000);
                        log.info("生成任务执行成功，gameType = {}", task.getGameType());
                    }
                } catch (Exception e) {
                    log.error("执行生成任务异常，gameType = " + task.getGameType(), e);
                } finally {
                    log.info("任务执行结束 gameType = {}", task.getGameType());
                    runningTasks.decrementAndGet();
                    runningGameTypes.remove(task.getGameType());  // 移除标记
                    // 任务完成后，尝试启动下一个
                    tryStartNextTask();
                }
            });
        }
        log.info("tryStartNextTask执行结束 queueSize = {},runningGameTypes = {}", generateTaskQueue.size(), runningGameTypes);
    }
}
