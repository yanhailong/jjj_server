package com.jjg.game.hall.minigame.game.luckytreasure.service;

import cn.hutool.core.collection.ConcurrentHashSet;
import com.alibaba.fastjson.JSONObject;
import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.curator.NodeType;
import com.jjg.game.common.pb.ItemInfo;
import com.jjg.game.common.protostuff.MessageUtil;
import com.jjg.game.common.protostuff.PFMessage;
import com.jjg.game.common.redis.RedisLock;
import com.jjg.game.common.timer.TimerCenter;
import com.jjg.game.common.timer.TimerEvent;
import com.jjg.game.common.timer.TimerListener;
import com.jjg.game.core.config.bean.LuckyTreasureConfig;
import com.jjg.game.core.constant.*;
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
import com.jjg.game.hall.service.HallPlayerService;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.GlobalConfigCfg;
import com.jjg.game.sampledata.bean.ItemCfg;
import com.jjg.game.sampledata.bean.MailCfg;
import org.redisson.api.RLock;
import org.redisson.api.RMapCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

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
    private final HallPlayerService playerService;

    //global表中设置幸运夺宝相关的id
    private final int ID_GLOBAL_LUCKY_ICON = 101;
    private final int ID_GLOBAL_LUCKY_ITEM_ID = 102;
    /**
     * 等待通知更新的期号列表
     */
    private final ConcurrentHashSet<Long> issueNumberSet = new ConcurrentHashSet<>();

    /**
     * 延迟更新定时器
     */
    private TimerEvent<LuckyTreasureService> updateTimer;

    public LuckyTreasureService(LuckyTreasureDao luckyTreasureDao,
                                LuckyTreasureRedisDao luckyTreasureRedisDao,
                                RedisLock redisLock,
                                TimerCenter timerCenter,
                                SubscriptionManager subscriptionManager,
                                ClusterSystem clusterSystem,
                                PlayerPackService playerPackService, HallPlayerService playerService) {
        this.luckyTreasureDao = luckyTreasureDao;
        this.luckyTreasureRedisDao = luckyTreasureRedisDao;
        this.redisLock = redisLock;
        this.playerPackService = playerPackService;
        this.timerCenter = timerCenter;
        this.subscriptionManager = subscriptionManager;
        this.clusterSystem = clusterSystem;
        this.playerService = playerService;
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

            List<LuckyTreasure> luckyTreasureList = new ArrayList<>();

            //只有先将活动从redis中取出来 避免循环中从redis拉取增大开销
            List<LuckyTreasure> redisLuckyTreasureList = updateSet.stream()
                    .map(luckyTreasureRedisDao::getTreasureByIssueNumber)
                    .filter(Objects::nonNull) // 过滤掉null值
                    .toList();

            //如果在redis中没有找到该 issueNumber，则要从mongodb中查询
            List<Long> needFindFromMongList = new ArrayList<>();

            if (!redisLuckyTreasureList.isEmpty()) {
                luckyTreasureList.addAll(redisLuckyTreasureList);
            }

            updateSet.forEach(id -> {
                LuckyTreasure tmp = redisLuckyTreasureList.stream().filter(luckyTreasure -> Objects.equals(luckyTreasure.getIssueNumber(), id)).findFirst().orElse(null);
                if (tmp == null) {
                    needFindFromMongList.add(id);
                }
            });

            List<LuckyTreasure> mongoLuckyTreasureList = luckyTreasureDao.find(needFindFromMongList);
            if (!mongoLuckyTreasureList.isEmpty()) {
                luckyTreasureList.addAll(mongoLuckyTreasureList);
            }

            subscriptionManager.publish(SubscriptionTopic.TOPIC_LUCKY_TREASURE_UPDATE, (playerId) -> {
                NotifyLuckyTreasureUpdate notifyLuckyTreasureUpdate = new NotifyLuckyTreasureUpdate();
                luckyTreasureList.forEach(treasure -> {
                    LuckyTreasureUpdateInfo afterInfo = new LuckyTreasureUpdateInfo();
                    afterInfo.setIssueNumber(treasure.getIssueNumber());
                    afterInfo.setAlreadyBuyCount(treasure.getBuyMap().getOrDefault(playerId, 0));
                    afterInfo.setIssueNumber(treasure.getIssueNumber());
                    afterInfo.setSoldCount(treasure.getSoldCount());
                    afterInfo.setCountDown(LuckyTreasureStatusUtil.calculateRewardTimeSecond(treasure));
                    afterInfo.setEndBuyCountDown(LuckyTreasureStatusUtil.calculateCountDown(treasure));
                    afterInfo.setConfigId(treasure.getConfig().getId());
                    afterInfo.setBuyCount(treasure.getBuyMap().size());
                    afterInfo.setTotalCount(treasure.getConfig().getTotal());
                    afterInfo.setStatus(calculateStatus(treasure, playerId));
                    if (treasure.getStatus() == LuckyTreasureStatusUtil.STATUS_CAN_BUY || treasure.getStatus() == LuckyTreasureStatusUtil.STATUS_WAIT_DRAW) {
                        notifyLuckyTreasureUpdate.getUpdateList().add(afterInfo);
                    }

                });
                if (notifyLuckyTreasureUpdate.getUpdateList().isEmpty()) {
                    return null;
                }
                log.debug("推送订阅 topic = {},playerId = {}, LuckyTreasureUpdateInfo = {}", SubscriptionTopic.TOPIC_LUCKY_TREASURE_UPDATE, playerId, JSONObject.toJSONString(notifyLuckyTreasureUpdate));
                return notifyLuckyTreasureUpdate;
            });

            subscriptionManager.publish(SubscriptionTopic.TOPIC_LUCKY_TREASURE_UPDATE, (playerId) -> {
                NotifyLuckyTreasureRecordUpdate notifyLuckyTreasureRecordUpdate = new NotifyLuckyTreasureRecordUpdate();
                luckyTreasureList.forEach(treasure -> {
                    LuckyTreasureUpdateInfo afterInfo = new LuckyTreasureUpdateInfo();
                    afterInfo.setIssueNumber(treasure.getIssueNumber());
                    afterInfo.setAlreadyBuyCount(treasure.getBuyMap().getOrDefault(playerId, 0));
                    afterInfo.setIssueNumber(treasure.getIssueNumber());
                    afterInfo.setSoldCount(treasure.getSoldCount());
                    afterInfo.setCountDown(LuckyTreasureStatusUtil.calculateRewardTimeSecond(treasure));
                    afterInfo.setEndBuyCountDown(LuckyTreasureStatusUtil.calculateCountDown(treasure));
                    afterInfo.setConfigId(treasure.getConfig().getId());
                    afterInfo.setBuyCount(treasure.getBuyMap().size());
                    afterInfo.setTotalCount(treasure.getConfig().getTotal());
                    afterInfo.setStatus(calculateStatus(treasure, playerId));
                    if (treasure.getStatus() == LuckyTreasureStatusUtil.STATUS_CAN_BUY || treasure.getStatus() == LuckyTreasureStatusUtil.STATUS_WAIT_DRAW) {
                        notifyLuckyTreasureRecordUpdate.getUpdateList().add(afterInfo);
                    }
                    notifyLuckyTreasureRecordUpdate.getUpdateList().add(afterInfo);
                });
                log.debug("推送订阅 topic = {},playerId = {}, LuckyTreasureUpdateRecordInfo = {}", SubscriptionTopic.TOPIC_LUCKY_TREASURE_UPDATE, playerId, JSONObject.toJSONString(notifyLuckyTreasureRecordUpdate));
                if (notifyLuckyTreasureRecordUpdate.getUpdateList().isEmpty()) {
                    return null;
                }
                return notifyLuckyTreasureRecordUpdate;
            });
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

//            log.info("收到更新通知,需要同步更新期号[{}]数据", issueNumber);
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
            RMapCache<Long, LuckyTreasure> activeTreasures = luckyTreasureRedisDao.getActiveTreasures();

            // 分页处理
            int totalCount = activeTreasures.size();
            int totalPage = (totalCount + pageSize - 1) / pageSize;
            int startIndex = (currPage - 1) * pageSize;

            List<LuckyTreasureInfo> infoList = new ArrayList<>();
            if (startIndex < totalCount) {
                Iterator<Map.Entry<Long, LuckyTreasure>> iterator = activeTreasures.entrySet().iterator();

                // 跳过前面的数据
                int skipCount = (currPage - 1) * pageSize;
                while (iterator.hasNext() && skipCount > 0) {
                    iterator.next();
                    skipCount--;
                }

                // 获取当前页数据
                int count = 0;
                while (iterator.hasNext() && count < pageSize) {
                    Map.Entry<Long, LuckyTreasure> entry = iterator.next();
                    LuckyTreasureInfo info = convertToInfo(entry.getValue(), playerController.getPlayer());
                    infoList.add(info);
                    count++;
                }
            }

            ResLuckyTreasureInfo response = new ResLuckyTreasureInfo(Code.SUCCESS);
            response.setInfoList(infoList);
            response.setCurrPage(currPage);
            response.setPageSize(pageSize);
            response.setTotalCount(totalCount);
            response.setTotalPage(totalPage);

            response.setIcon(GameDataManager.getGlobalConfigCfg(ID_GLOBAL_LUCKY_ICON).getValue());
            response.setItemId(Integer.parseInt(GameDataManager.getGlobalConfigCfg(ID_GLOBAL_LUCKY_ICON).getValue()));

            return response;
        } catch (Exception e) {
            log.error("获取夺宝奇兵详情失败", e);
            return new ResLuckyTreasureInfo(Code.EXCEPTION);
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
            player = playerService.get(player.getId());
            playerController.setPlayer(player);
            // 购买数量无效
            if (count <= 0) {
                TipUtils.sendTip(playerController, TipUtils.TipType.TOAST, 50031);
                result.code = Code.PARAM_ERROR;
                log.debug("购买数量无效,购买夺宝奇兵道具失败 playerId = {},count = {}", player.getId(), count);
                return result;
            }

            // 获取夺宝奇兵活动数据
            LuckyTreasure treasure = luckyTreasureRedisDao.getTreasureByIssueNumber(issueNumber);
            if (treasure == null) {
                TipUtils.sendTip(playerController, TipUtils.TipType.TOAST, 50031);
                result.code = Code.NOT_FOUND;
                log.debug("获取夺宝奇兵的数据失败,购买夺宝奇兵道具失败 playerId = {},issueNumber = {}", player.getId(), issueNumber);
                return result;
            }

            // 检查活动状态
            int status = calculateStatus(treasure, player.getId());
            if (status != LuckyTreasureStatusUtil.STATUS_CAN_BUY) {
                TipUtils.sendTip(playerController, TipUtils.TipType.TOAST, 50031);
                result.code = Code.FAIL;
                log.debug("该活动状态错误,购买夺宝奇兵道具失败 playerId = {},issueNumber = {},status = {}", player.getId(), issueNumber, status);
                return result;
            }

            // 检查剩余数量
            int remainingCount = treasure.getConfig().getTotal() - treasure.getSoldCount();
            if (remainingCount < count) {
                TipUtils.sendTip(playerController, TipUtils.TipType.TOAST, 50032);
                result.code = Code.FAIL;
                log.debug("剩余数量不足,购买夺宝奇兵道具失败 playerId = {},issueNumber = {},remainingCount = {}", player.getId(), issueNumber, remainingCount);
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

            RLock writeLock = null;
            try {
                // 获取写锁进行购买操作
                writeLock = redisLock.getWriteLock(lockKey, 100);
                if (writeLock == null) {
                    result.code = Code.FAIL;
                    TipUtils.sendTip(playerController, TipUtils.TipType.TOAST, 50028);
                    log.debug("加锁失败,购买夺宝奇兵道具失败 playerId = {},issueNumber = {}", player.getId(), issueNumber);
                    return result;
                }
                // 先扣除玩家道具
                CommonResult<ItemOperationResult> deductResult = playerPackService.removeItems(player, consumeMap, AddType.LUCKY_TREASURE_BUY);
                if (!deductResult.success()) {
                    TipUtils.sendTip(playerController, TipUtils.TipType.TOAST, 50028);
                    result.code = Code.FAIL;
                    log.debug("扣除道具失败,购买夺宝奇兵道具失败 playerId = {},issueNumber = {},consumeMap = {}", player.getId(), issueNumber, consumeMap);
                    return result;
                }

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
                    response.setStatus(calculateStatus(latestTreasure, player.getId()));

                    List<ItemInfo> itemInfos = new ArrayList<>();
                    for (Map.Entry<Integer, Long> en : consumeMap.entrySet()) {
                        int itemId = en.getKey();
                        ItemCfg itemCfg = GameDataManager.getItemCfg(itemId);
                        if (itemCfg == null) {
                            continue;
                        }

                        ItemInfo itemInfo = new ItemInfo();
                        itemInfo.itemId = itemId;
                        if (itemCfg.getType() == GameConstant.Item.TYPE_DIAMOND) {
                            itemInfo.count = deductResult.data.getDiamond();
                        } else if (itemCfg.getType() == GameConstant.Item.TYPE_GOLD) {
                            itemInfo.count = deductResult.data.getGoldNum();
                        } else {
                            Long num = deductResult.data.getChangeEndItemNum().get(en.getKey());
                            if (num != null) {
                                itemInfo.count = num;
                            }
                        }
                        itemInfos.add(itemInfo);
                    }
                    response.setItems(itemInfos);
                    result.data = response;
                    //购买成功通知更新 广播到所有节点
                    broadcastUpdate(latestTreasure.getIssueNumber());
                }
                return result;
            } catch (Exception e) {
                // 发生异常，退还道具
                playerPackService.addItems(player.getId(), consumeMap, AddType.LUCKY_TREASURE_BUY_EXCEPTION_ROLL_BACK);
                throw e;
            } finally {
                if (writeLock != null) {
                    writeLock.unlock();
                }
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
            if (latestTreasure == null) {
                // 活动状态已变更，退还道具
                playerPackService.addItems(player.getId(), consumeMap, AddType.LUCKY_TREASURE_BUY_STATUSCHANGED_ROLL_BACK);
                TipUtils.sendTip(player.getId(), TipUtils.TipType.TOAST, 50031);
                log.debug("latestTreasure为空，购买夺宝奇兵道具失败11 playerId = {}", player.getId());
                return Code.FAIL;
            }

            int status = calculateStatus(latestTreasure, player.getId());
            if (status != LuckyTreasureStatusUtil.STATUS_CAN_BUY) {
                // 活动状态已变更，退还道具
                playerPackService.addItems(player.getId(), consumeMap, AddType.LUCKY_TREASURE_BUY_STATUSCHANGED_ROLL_BACK);
                TipUtils.sendTip(player.getId(), TipUtils.TipType.TOAST, 50031);
                log.debug("状态错误，购买夺宝奇兵道具失败11 playerId = {},status = {}", player.getId(), status);
                return Code.FAIL;
            }

            // 检查剩余数量
            int remainingCount = latestTreasure.getConfig().getTotal() - latestTreasure.getSoldCount();
            if (remainingCount < count) {
                // 剩余数量不足，退还道具
                playerPackService.addItems(player.getId(), consumeMap, AddType.LUCKY_TREASURE_BUY_NOT_ENOUGH_ROLLBACK);
                TipUtils.sendTip(player.getId(), TipUtils.TipType.TOAST, 50032);
                log.debug("剩余数量不足，购买夺宝奇兵道具失败11 playerId = {},remainingCount = {},count = {}", player.getId(), remainingCount, count);
                return Code.FAIL;
            }

            // 执行购买
            latestTreasure = luckyTreasureRedisDao.buyTreasure(latestTreasure.getIssueNumber(), player.getId(), count);

            if (latestTreasure == null) {
                // 购买失败，退还道具
                playerPackService.addItems(player.getId(), consumeMap, AddType.LUCKY_TREASURE_BUY_FAILED_ROLLBACK);
                TipUtils.sendTip(player.getId(), TipUtils.TipType.TOAST, 50030);
                log.debug("购买失败，购买夺宝奇兵道具失败11 playerId = {}", player.getId());
                return Code.FAIL;
            }

            // 购买成功，更新数据库
            luckyTreasureDao.save(latestTreasure);

            log.info("夺宝奇兵购买成功, 玩家ID:{}, 期号:{}, 购买数量:{}", player.getId(), latestTreasure.getIssueNumber(), count);

            return Code.SUCCESS;
        } catch (Exception e) {
            log.error("执行购买逻辑失败", e);
            // 发生异常，退还道具
            playerPackService.addItems(player.getId(), consumeMap, AddType.LUCKY_TREASURE_BUY_EXCEPTION_ROLL_BACK);
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
        info.setStatus(calculateStatus(treasure, player.getId()));
        info.setEndBuyCountDown(LuckyTreasureStatusUtil.calculateCountDown(treasure));
        info.setCountDown(LuckyTreasureStatusUtil.calculateRewardTimeSecond(treasure));
        info.setReceiveCountdown(LuckyTreasureStatusUtil.calculateReceiveCountdown(treasure));

        if (treasure.getAwardPlayerId() > 0) {
            Player winPlayer = playerService.get(treasure.getAwardPlayerId());
            if (winPlayer != null) {
                info.setWinPlayerName(winPlayer.getNickName());
            }
        }

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
                if (record.getStatus() == LuckyTreasureStatusUtil.STATUS_CAN_BUY || record.getStatus() == LuckyTreasureStatusUtil.STATUS_WAIT_DRAW) {
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
        history.setAwardPlayerLevel(treasure.getAwardPlayerLevel());
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

            MailCfg mailCfg = GameDataManager.getMailCfg(LuckyTreasureConstant.MailId.REWARD_MAIL_ID);
            // 发放奖励道具
            LuckyTreasureConfig config = latestTreasure.getConfig();
            if (mailCfg == null) {
                Map<Integer, Long> rewardMap = new HashMap<>();
                rewardMap.put(config.getItemId(), (long) config.getItemNum());

                long playerId = player.getId();
                CommonResult<ItemOperationResult> addResult = playerPackService.addItems(playerId, rewardMap, AddType.LUCKY_TREASURE_REWARDS);

                if (!addResult.success()) {
                    return false;
                }
                latestTreasure.setStatus(LuckyTreasureStatusUtil.STATUS_RECEIVED);
                // 更新领取状态
                latestTreasure.setReceived(true);
                //记录领奖的时间戳
                latestTreasure.setReceiveTime(System.currentTimeMillis());
                luckyTreasureDao.save(latestTreasure);

                log.info("夺宝奇兵奖励领取成功, 玩家ID:{}, 期号:{}, 领奖码:{}, 道具ID:{}, 数量:{}", playerId, latestTreasure.getIssueNumber(),
                        latestTreasure.getRewardCode(), config.getItemId(), config.getItemNum());
            }
            broadcastUpdate(issueNumber);
            return true;

        } catch (Exception e) {
            log.error("执行领取奖励逻辑失败", e);
            return false;
        }
    }

    /**
     * 计算该期活动的状态
     *
     * @param treasure
     * @param playerId
     * @return
     */
    public int calculateStatus(LuckyTreasure treasure, long playerId) {
        if (treasure == null || treasure.getConfig() == null) {
            return LuckyTreasureStatusUtil.STATUS_NOT_WINNER;
        }

        long currentTime = System.currentTimeMillis();
        long endTime = treasure.getEndTime();
        int total = treasure.getConfig().getTotal();
        int soldCount = treasure.getSoldCount();

        // 活动未结束的情况
        if (treasure.getStatus() == LuckyTreasureStatusUtil.STATUS_CAN_BUY) {
            // 已售完但未到结束时间，等待开奖
            if (soldCount >= total) {
                return LuckyTreasureStatusUtil.STATUS_CAN_BUY;
            }
            if (endTime < currentTime) {
                return LuckyTreasureStatusUtil.STATUS_WAIT_DRAW;
            }
            // 可继续购买
            return LuckyTreasureStatusUtil.STATUS_CAN_BUY;
        }

        //等待开奖
        if (treasure.getStatus() == LuckyTreasureStatusUtil.STATUS_WAIT_DRAW) {
            return LuckyTreasureStatusUtil.STATUS_WAIT_DRAW;
        }

        // 已开奖,未过期的情况
        // 非中奖玩家直接返回未中奖
        if (treasure.getAwardPlayerId() != playerId) {
            return LuckyTreasureStatusUtil.STATUS_NOT_WINNER;
        }
        // 中奖玩家的状态判断
        if (treasure.isReceived()) {
            return LuckyTreasureStatusUtil.STATUS_RECEIVED;
        }

        if (treasure.getStatus() == LuckyTreasureStatusUtil.STATUS_EXPIRED_WINNER) {
            return treasure.getStatus();
        }

        // 已开奖的情况
        // 检查领奖是否过期
        long rewardTime = 0L;
        GlobalConfigCfg globalConfigCfg = GameDataManager.getGlobalConfigCfg(LuckyTreasureConstant.Common.LUCKY_TREASURE_GLOBAL_REWARED_CONFIG_ID);
        if (globalConfigCfg == null || globalConfigCfg.getIntValue() < 1) {
            rewardTime = TimeUnit.SECONDS.toMillis(globalConfigCfg.getIntValue());
        }

        long receiveDeadline = endTime + rewardTime + TimeUnit.MINUTES.toMillis(treasure.getConfig().getCollectTime());

        if (currentTime > receiveDeadline) {
            treasure.setStatus(LuckyTreasureStatusUtil.STATUS_EXPIRED_WINNER);
//            luckyTreasureDao.save(treasure);
//            broadcastUpdate(treasure.getIssueNumber());
            return treasure.getStatus();
        }
        return LuckyTreasureStatusUtil.STATUS_WAIT_RECEIVE;
    }

}