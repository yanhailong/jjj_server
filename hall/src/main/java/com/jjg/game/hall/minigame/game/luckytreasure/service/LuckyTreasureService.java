package com.jjg.game.hall.minigame.game.luckytreasure.service;

import cn.hutool.core.collection.ConcurrentHashSet;
import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.curator.NodeType;
import com.jjg.game.common.protostuff.MessageUtil;
import com.jjg.game.common.protostuff.PFMessage;
import com.jjg.game.common.redis.RedisLock;
import com.jjg.game.common.timer.TimerCenter;
import com.jjg.game.common.timer.TimerEvent;
import com.jjg.game.common.timer.TimerListener;
import com.jjg.game.core.config.bean.LuckyTreasureConfig;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.constant.LuckyTreasureConstant;
import com.jjg.game.core.constant.SubscriptionTopic;
import com.jjg.game.core.dao.luckytreasure.LuckyTreasureDao;
import com.jjg.game.core.dao.luckytreasure.LuckyTreasureRedisDao;
import com.jjg.game.core.data.*;
import com.jjg.game.core.manager.SubscriptionManager;
import com.jjg.game.core.pb.LuckyTreasureUpdateBroadcast;
import com.jjg.game.core.service.PlayerPackService;
import com.jjg.game.core.utils.TipUtils;
import com.jjg.game.hall.minigame.game.luckytreasure.bean.LuckyTreasureConsumeInfo;
import com.jjg.game.hall.minigame.game.luckytreasure.message.bean.LuckyTreasureHistory;
import com.jjg.game.hall.minigame.game.luckytreasure.message.bean.LuckyTreasureInfo;
import com.jjg.game.hall.minigame.game.luckytreasure.message.bean.LuckyTreasureUpdateInfo;
import com.jjg.game.hall.minigame.game.luckytreasure.message.res.*;
import com.jjg.game.hall.minigame.game.luckytreasure.util.LuckyTreasureStatusUtil;
import org.redisson.api.RLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 夺宝奇兵服务类
 */
@Service
public class LuckyTreasureService implements TimerListener<LuckyTreasureService> {
    private static final Logger log = LoggerFactory.getLogger(LuckyTreasureService.class);

    private final LuckyTreasureDao luckyTreasureDao;
    private final LuckyTreasureRedisDao luckyTreasureRedisDao;
    private final RedisLock redisLock;
    private final PlayerPackService playerPackService;
    private final TimerCenter timerCenter;
    private final SubscriptionManager subscriptionManager;
    private final ClusterSystem clusterSystem;

    /**
     * 等待通知更新的期号列表
     */
    private final ConcurrentHashSet<Long> issueNumberSet = new ConcurrentHashSet<>();

    /**
     * 延迟更新定时器
     */
    private TimerEvent<LuckyTreasureService> updateTimer;

    private TimerEvent<LuckyTreasureService> tickTimer;

    public LuckyTreasureService(LuckyTreasureDao luckyTreasureDao,
                                LuckyTreasureRedisDao luckyTreasureRedisDao,
                                RedisLock redisLock,
                                TimerCenter timerCenter,
                                SubscriptionManager subscriptionManager,
                                ClusterSystem clusterSystem,
                                PlayerPackService playerPackService) {
        this.luckyTreasureDao = luckyTreasureDao;
        this.luckyTreasureRedisDao = luckyTreasureRedisDao;
        this.redisLock = redisLock;
        this.playerPackService = playerPackService;
        this.timerCenter = timerCenter;
        this.subscriptionManager = subscriptionManager;
        this.clusterSystem = clusterSystem;
    }

    /**
     * 初始化
     */
    public void init() {
        tickTimer = new TimerEvent<>(this, null, 1000);
    }

    /**
     * 定时事件的监听方法
     *
     * @param e
     */
    @Override
    public void onTimer(TimerEvent<LuckyTreasureService> e) {
        if (e == updateTimer) {
            updateTimer = null;
            Set<Long> updateSet = new HashSet<>(issueNumberSet);
            issueNumberSet.clear();
            //只有先将活动从redis中取出来 避免循环中从redis拉取增大开销
            List<LuckyTreasure> luckyTreasureList = updateSet.stream().map(luckyTreasureRedisDao::getTreasureByIssueNumber).toList();
            subscriptionManager.publish(SubscriptionTopic.TOPIC_LUCKY_TREASURE_UPDATE, (playerId) -> {
                NotifyLuckyTreasureUpdate notifyLuckyTreasureUpdate = new NotifyLuckyTreasureUpdate();
                luckyTreasureList.forEach(treasure -> {
                    LuckyTreasureUpdateInfo afterInfo = new LuckyTreasureUpdateInfo();
                    afterInfo.setIssueNumber(treasure.getIssueNumber());
                    afterInfo.setAlreadyBuyCount(treasure.getBuyMap().getOrDefault(playerId, 0));
                    afterInfo.setIssueNumber(treasure.getIssueNumber());
                    afterInfo.setSoldCount(treasure.getSoldCount());
                    afterInfo.setCountDown(LuckyTreasureStatusUtil.calculateCountDown(treasure));
                    afterInfo.setConfigId(treasure.getConfig().getId());
                    afterInfo.setBuyCount(treasure.getBuyMap().size());
                    afterInfo.setTotalCount(treasure.getConfig().getTotal());
                    afterInfo.setStatus(LuckyTreasureStatusUtil.calculateStatus(treasure, playerId));
                    notifyLuckyTreasureUpdate.getUpdateList().add(afterInfo);
                });
                return notifyLuckyTreasureUpdate;
            });
            log.info("延迟同步夺宝奇兵库存完毕!set={}", updateSet);
        }
    }

    /**
     * 收到其他节点广播的同步数据消息
     */
    public void handleUpdateMessage(long issueNumber) {
        //添加到待更新列表
        if (issueNumber > 0) {
            issueNumberSet.add(issueNumber);
            if (updateTimer == null) {
                updateTimer = new TimerEvent<>(this, null, 0, 1, 200, false);
                // 添加到定时器中心
                timerCenter.add(updateTimer);
            }
            log.info("收到更新通知,需要同步更新期号[{}]数据", issueNumber);
        }
    }

    /**
     * 广播其他节点库存更新
     *
     * @param issueNumber 期号
     */
    public void broadcastUpdate(long issueNumber) {
        LuckyTreasureUpdateBroadcast message = new LuckyTreasureUpdateBroadcast();
        message.setIssueNumber(issueNumber);
        PFMessage pfMessage = MessageUtil.getPFMessage(message);
        clusterSystem.notifyNode(pfMessage, Set.of(NodeType.HALL.toString())::contains);
        //顺便通知自己
        handleUpdateMessage(issueNumber);
    }

    /**
     * 获取夺宝奇兵详情列表
     */
    public ResLuckyTreasureInfo getLuckyTreasureInfo(PlayerController playerController, int currPage, int pageSize) {
        try {
            // 限制每页最大条数
            if (pageSize > 20) {
                pageSize = 20;
            }
            if (pageSize <= 0) {
                pageSize = 10;
            }
            if (currPage <= 0) {
                currPage = 1;
            }

            // 从Redis获取活跃的夺宝奇兵活动
            List<LuckyTreasure> activeTreasures = luckyTreasureRedisDao.getActiveTreasures();

            // 分页处理
            int totalCount = activeTreasures.size();
            int totalPage = (totalCount + pageSize - 1) / pageSize;
            int startIndex = (currPage - 1) * pageSize;
            int endIndex = Math.min(startIndex + pageSize, totalCount);

            List<LuckyTreasureInfo> infoList = new ArrayList<>();
            if (startIndex < totalCount) {
                List<LuckyTreasure> pageTreasures = activeTreasures.subList(startIndex, endIndex);
                for (LuckyTreasure treasure : pageTreasures) {
                    LuckyTreasureInfo info = convertToInfo(treasure, playerController.getPlayer());
                    infoList.add(info);
                }
            }

            ResLuckyTreasureInfo response = new ResLuckyTreasureInfo(Code.SUCCESS);
            response.setInfoList(infoList);
            response.setCurrPage(currPage);
            response.setPageSize(pageSize);
            response.setTotalCount(totalCount);
            response.setTotalPage(totalPage);

            return response;
        } catch (Exception e) {
            log.error("获取夺宝奇兵详情失败", e);
            return new ResLuckyTreasureInfo(-1);
        }
    }

    /**
     * 购买夺宝奇兵道具
     */
    public CommonResult<ResBuyLuckyTreasure> buyLuckyTreasure(PlayerController playerController, long issueNumber, int count) {
        CommonResult<ResBuyLuckyTreasure> result = new CommonResult<>();
        result.code = Code.SUCCESS;
        try {
            Player player = playerController.getPlayer();
            // 购买数量无效
            if (count <= 0) {
                TipUtils.sendTip(playerController, TipUtils.TipType.TOAST, 50031);
                result.code = Code.FAIL;
                return result;
            }

            // 获取夺宝奇兵活动数据
            LuckyTreasure treasure = luckyTreasureRedisDao.getTreasureByIssueNumber(issueNumber);
            if (treasure == null) {
                TipUtils.sendTip(playerController, TipUtils.TipType.TOAST, 50031);
                result.code = Code.FAIL;
                return result;
            }

            // 检查活动状态
            if (LuckyTreasureStatusUtil.calculateStatus(treasure, player.getId()) != LuckyTreasureStatusUtil.STATUS_CAN_BUY) {
                TipUtils.sendTip(playerController, TipUtils.TipType.TOAST, 50031);
                result.code = Code.FAIL;
                return result;
            }

            // 检查剩余数量
            int remainingCount = treasure.getConfig().getTotal() - treasure.getSoldCount();
            if (remainingCount < count) {
                TipUtils.sendTip(playerController, TipUtils.TipType.TOAST, 50032);
                result.code = Code.FAIL;
                return result;
            }

            // 先计算需要扣除的道具
            LuckyTreasureConfig config = treasure.getConfig();
            List<Integer> consumption = config.getConsumption();
            Map<Integer, Long> consumeMap = new HashMap<>();
            for (int i = 0; i < consumption.size(); i += 2) {
                int itemId = consumption.get(i);
                long itemNum = (long) consumption.get(i + 1) * count;
                consumeMap.put(itemId, itemNum);
            }

            // 使用读写锁确保购买的一致性
            String lockKey = LuckyTreasureConstant.RedisLock.LUCKY_TREASURE_BUY + issueNumber;

            try {
                // 获取写锁进行购买操作
                RLock writeLock = redisLock.getWriteLock(lockKey, 100);
                if (writeLock == null) {
                    result.code = Code.FAIL;
                    TipUtils.sendTip(playerController, TipUtils.TipType.TOAST, 50028);
                    return result;
                }
                if (!writeLock.tryLock()) {
                    TipUtils.sendTip(playerController, TipUtils.TipType.TOAST, 50028);
                    result.code = Code.FAIL;
                    return result;
                }
                // 先扣除玩家道具
                CommonResult<ItemOperationResult> deductResult = playerPackService.removeItems(player, consumeMap, "luckyTreasureBuy");
                if (!deductResult.success()) {
                    TipUtils.sendTip(playerController, TipUtils.TipType.TOAST, 50028);
                    result.code = Code.FAIL;
                    return result;
                }

                try {
                    //在写锁中重新获取最新数据
                    LuckyTreasure latestTreasure = luckyTreasureRedisDao.getTreasureByIssueNumber(issueNumber);
                    int resultCode = executeBuyWithLock(player, latestTreasure, count, consumeMap);
                    if (resultCode != Code.SUCCESS) {
                        result.code = resultCode;
                    } else {
                        // 返回成功结果
                        ResBuyLuckyTreasure response = new ResBuyLuckyTreasure(Code.SUCCESS);
                        response.setIssueNumber(latestTreasure.getIssueNumber());
                        response.setBuyCount(count);
                        response.setRemainingCount(remainingCount - count);
                        response.setStatus(LuckyTreasureStatusUtil.calculateStatus(latestTreasure, player.getId()));
                        result.data = response;
                        //购买成功通知更新 广播到所有节点
                        broadcastUpdate(latestTreasure.getIssueNumber());
                    }
                    return result;
                } finally {
                    writeLock.unlock();
                }
            } catch (Exception e) {
                // 发生异常，退还道具
                playerPackService.addItems(player.getId(), consumeMap, "luckyTreasureBuyExceptionRollback");
                throw e;
            }
        } catch (Exception e) {
            log.error("购买夺宝奇兵失败, 玩家ID:{}, 期号:{}, 数量:{}",
                    playerController.getPlayer().getId(), issueNumber, count, e);
            TipUtils.sendTip(playerController, TipUtils.TipType.TOAST, 50031);
            result.code = Code.FAIL;
            return result;
        }
    }

    /**
     * 执行购买逻辑（在写锁内执行，道具已扣除）
     */
    private int executeBuyWithLock(Player player, LuckyTreasure latestTreasure, int count, Map<Integer, Long> consumeMap) {
        try {
            if (latestTreasure == null || LuckyTreasureStatusUtil.calculateStatus(latestTreasure, player.getId()) != LuckyTreasureStatusUtil.STATUS_CAN_BUY) {
                // 活动状态已变更，退还道具
                playerPackService.addItems(player.getId(), consumeMap, "luckyTreasureBuyStatusChangedRollback");
                TipUtils.sendTip(player.getId(), TipUtils.TipType.TOAST, 50031);
                return Code.FAIL;
            }

            // 检查剩余数量
            int remainingCount = latestTreasure.getConfig().getTotal() - latestTreasure.getSoldCount();
            if (remainingCount < count) {
                // 剩余数量不足，退还道具
                playerPackService.addItems(player.getId(), consumeMap, "luckyTreasureBuyNotEnoughRollback");
                TipUtils.sendTip(player.getId(), TipUtils.TipType.TOAST, 50032);
                return Code.FAIL;
            }

            // 执行购买
            latestTreasure = luckyTreasureRedisDao.buyTreasure(latestTreasure.getIssueNumber(), player.getId(), count);

            if (latestTreasure == null) {
                // 购买失败，退还道具
                playerPackService.addItems(player.getId(), consumeMap, "luckyTreasureBuyFailedRollback");
                TipUtils.sendTip(player.getId(), TipUtils.TipType.TOAST, 50030);
                return Code.FAIL;
            }

            // 购买成功，更新数据库
            luckyTreasureDao.save(latestTreasure);

            log.info("夺宝奇兵购买成功, 玩家ID:{}, 期号:{}, 购买数量:{}", player.getId(), latestTreasure.getIssueNumber(), count);

            return Code.SUCCESS;
        } catch (Exception e) {
            log.error("执行购买逻辑失败", e);
            // 发生异常，退还道具
            playerPackService.addItems(player.getId(), consumeMap, "luckyTreasureBuyExceptionRollback");
            return Code.EXCEPTION;
        }
    }

    /**
     * 转换为信息对象
     */
    private LuckyTreasureInfo convertToInfo(LuckyTreasure treasure, Player player) {
        LuckyTreasureInfo info = new LuckyTreasureInfo();
        LuckyTreasureConfig config = treasure.getConfig();

        info.setIssueNumber(treasure.getIssueNumber());
        info.setConfigId(config.getId());
        info.setType(config.getType());
        info.setItemId(config.getItemId());
        info.setItemNum(config.getItemNum());
        info.setBestValue(config.getBestValue());
        info.setTotalCount(config.getTotal());
        info.setSoldCount(treasure.getSoldCount());
        info.setIcon(config.getDes());
        info.setName(config.getName());
        info.setBuyCount(treasure.getBuyMap().size());
        info.setAlreadyBuyCount(treasure.getBuyMap().getOrDefault(player.getId(), 0));

        info.setRewardCode(treasure.getRewardCode());

        // 使用工具类计算临时字段
        info.setStatus(LuckyTreasureStatusUtil.calculateStatus(treasure, player.getId()));
        info.setCountDown(LuckyTreasureStatusUtil.calculateCountDown(treasure));
        info.setReceiveCountdown(LuckyTreasureStatusUtil.calculateReceiveCountdown(treasure));

        // 设置消耗信息
        List<LuckyTreasureConsumeInfo> consumeInfoList = convertConsumeInfo(config);
        info.setConsumeInfoList(consumeInfoList);

        return info;
    }

    private List<LuckyTreasureConsumeInfo> convertConsumeInfo(LuckyTreasureConfig config) {
        List<Integer> consumption = config.getConsumption();
        List<LuckyTreasureConsumeInfo> consumeInfoList = new ArrayList<>();
        for (int i = 0; i < consumption.size(); i += 2) {
            LuckyTreasureConsumeInfo consumeInfo = new LuckyTreasureConsumeInfo();
            consumeInfo.setItemId(consumption.get(i));
            consumeInfo.setItemNum(consumption.get(i + 1));
            consumeInfoList.add(consumeInfo);
        }
        return consumeInfoList;
    }

    /**
     * 获取玩家参与过的夺宝奇兵记录
     */
    public ResLuckyTreasureRecord getLuckyTreasureRecord(PlayerController playerController, int currPage, int pageSize) {
        try {
            Player player = playerController.getPlayer();

            // 限制每页最大条数
            if (pageSize > 20) {
                pageSize = 20;
            }
            if (pageSize <= 0) {
                pageSize = 10;
            }
            //Pageable 默认0为第一页
            currPage -= 1;
            if (currPage < 0) {
                currPage = 0;
            }

            Pageable pageable = PageRequest.of(currPage, pageSize);

            Page<LuckyTreasure> pagedLuckyTreasures = luckyTreasureDao.findPlayerRecord(player.getId(), pageable);

            // 处理数据：对于未结束的活动(endTime=0)，从Redis获取最新数据
            List<LuckyTreasure> processedRecords = new ArrayList<>();

            pagedLuckyTreasures.forEach(record -> {
                if (record.getEndTime() == 0) {
                    // 活动未结束，从Redis获取最新数据
                    LuckyTreasure latestRecord = luckyTreasureRedisDao.getTreasureByIssueNumber(record.getIssueNumber());
                    // Redis中没有数据，使用数据库数据
                    processedRecords.add(Objects.requireNonNullElse(latestRecord, record));
                } else {
                    // 活动已结束，使用数据库数据
                    processedRecords.add(record);
                }
            });

            List<LuckyTreasureInfo> infoList = processedRecords.stream()
                    .map(record -> convertToInfo(record, player))
                    .toList();

            long totalCount = pagedLuckyTreasures.getTotalElements();
            int totalPage = pagedLuckyTreasures.getTotalPages();
            ResLuckyTreasureRecord response = new ResLuckyTreasureRecord(Code.SUCCESS);
            response.setInfoList(infoList);
            //客户端页码从1开始
            response.setCurrPage(currPage + 1);
            response.setPageSize(pageSize);
            response.setTotalCount(totalCount);
            response.setTotalPage(totalPage);

            return response;
        } catch (Exception e) {
            log.error("获取夺宝奇兵记录失败", e);
            return new ResLuckyTreasureRecord(Code.EXCEPTION);
        }
    }

    /**
     * 获取夺宝奇兵开奖历史记录
     */
    public ResLuckyTreasureHistory getLuckyTreasureHistory(int currPage, int pageSize) {
        try {
            // 限制每页最大条数
            if (pageSize > 20) {
                pageSize = 20;
            }
            if (pageSize <= 0) {
                pageSize = 10;
            }
            //Pageable 默认0为第一页
            currPage -= 1;
            if (currPage < 0) {
                currPage = 0;
            }

            Pageable pageable = PageRequest.of(currPage, pageSize);

            // 从数据库查询所有已结束的夺宝奇兵活动
            Page<LuckyTreasure> finishedRecords = luckyTreasureDao.findAllRewardHistory(pageable, 100);

            List<LuckyTreasureHistory> historyList = finishedRecords.stream().map(this::convertToHistory).toList();

            ResLuckyTreasureHistory response = new ResLuckyTreasureHistory(Code.SUCCESS);
            response.setInfoList(historyList);
            //客户端页码从1开始
            response.setCurrPage(currPage + 1);
            response.setPageSize(pageSize);
            response.setTotalCount(finishedRecords.getTotalElements());
            response.setTotalPage(finishedRecords.getTotalPages());

            return response;
        } catch (Exception e) {
            log.error("获取夺宝奇兵历史记录失败", e);
            return new ResLuckyTreasureHistory(Code.EXCEPTION);
        }
    }

    /**
     * 转换为历史记录对象
     */
    private LuckyTreasureHistory convertToHistory(LuckyTreasure treasure) {
        LuckyTreasureHistory history = new LuckyTreasureHistory();
        LuckyTreasureConfig config = treasure.getConfig();

        history.setIssueNumber(treasure.getIssueNumber());
        history.setConfigId(config.getId());
        history.setType(config.getType());
        history.setItemId(config.getItemId());
        history.setItemNum(config.getItemNum());
        history.setIcon(config.getDes());
        history.setName(config.getName());
        history.setAwardPlayerHeadImgId(treasure.getAwardPlayerHeadImgId());
        history.setAwardPlayerHeadFrameId(treasure.getAwardPlayerHeadFrameId());
        history.setAwardPlayerNickName(treasure.getAwardPlayerNickName());
        history.setAwardPlayerNationalId(treasure.getAwardPlayerNationalId());
        history.setEndTime(treasure.getEndTime());
        return history;
    }

    /**
     * 领取奖励
     */
    public boolean receive(PlayerController playerController, long issueNumber) {
        return redisLock.tryLockAndGet(LuckyTreasureConstant.RedisLock.LUCKY_TREASURE_RECEIVE + issueNumber,
                () -> this.receiveReward(playerController, issueNumber));
    }

    /**
     * 执行领取奖励逻辑
     */
    public boolean receiveReward(PlayerController playerController, long issueNumber) {
        try {
            Player player = playerController.getPlayer();
            if (player == null) {
                return false;
            }
            // 重新获取最新数据
            LuckyTreasure latestTreasure = luckyTreasureRedisDao.getTreasureByIssueNumber(issueNumber);
            if (latestTreasure == null) {
                //数据库查询
                latestTreasure = luckyTreasureDao.findById(issueNumber).orElse(null);
            }

            if (latestTreasure == null) {
                return false;
            }

            // 再次检查是否已领取
            if (latestTreasure.isReceived()) {
                //操作太频繁了
                TipUtils.sendTip(player.getId(), TipUtils.TipType.TOAST, 50030);
                return false;
            }

            //不是道具奖励不处理
            if (latestTreasure.getConfig().getType() != 2) {
                return false;
            }

            // 发放奖励道具
            LuckyTreasureConfig config = latestTreasure.getConfig();
            Map<Integer, Long> rewardMap = new HashMap<>();
            rewardMap.put(config.getItemId(), (long) config.getItemNum());

            long playerId = player.getId();
            CommonResult<ItemOperationResult> addResult = playerPackService.addItems(playerId, rewardMap, "luckyTreasureReceive");

            if (!addResult.success()) {
                return false;
            }
            // 更新领取状态
            latestTreasure.setReceived(true);
            //记录领奖的时间戳
            latestTreasure.setReceiveTime(System.currentTimeMillis());
            luckyTreasureDao.save(latestTreasure);

            log.info("夺宝奇兵奖励领取成功, 玩家ID:{}, 期号:{}, 领奖码:{}, 道具ID:{}, 数量:{}", playerId, latestTreasure.getIssueNumber(),
                    latestTreasure.getRewardCode(), config.getItemId(), config.getItemNum());

            return true;

        } catch (Exception e) {
            log.error("执行领取奖励逻辑失败", e);
            return false;
        }
    }

}
