package com.jjg.game.hall.minigame.game.luckytreasure.service;

import com.jjg.game.common.redis.RedisLock;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.ItemOperationResult;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.service.PlayerPackService;
import org.redisson.api.RLock;
import com.jjg.game.hall.minigame.game.luckytreasure.bean.LuckyTreasureConsumeInfo;
import com.jjg.game.hall.minigame.game.luckytreasure.constant.LuckyTreasureConstant;
import com.jjg.game.hall.minigame.game.luckytreasure.dao.LuckyTreasureDao;
import com.jjg.game.hall.minigame.game.luckytreasure.dao.LuckyTreasureRedisDao;
import com.jjg.game.hall.minigame.game.luckytreasure.data.LuckyTreasure;
import com.jjg.game.hall.minigame.game.luckytreasure.data.LuckyTreasureConfig;
import com.jjg.game.hall.minigame.game.luckytreasure.message.bean.LuckyTreasureInfo;
import com.jjg.game.hall.minigame.game.luckytreasure.message.res.ResBuyLuckyTreasure;
import com.jjg.game.hall.minigame.game.luckytreasure.message.res.ResLuckyTreasureInfo;
import com.jjg.game.hall.minigame.game.luckytreasure.util.LuckyTreasureStatusUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 夺宝奇兵服务类
 */
@Service
public class LuckyTreasureService {
    private static final Logger log = LoggerFactory.getLogger(LuckyTreasureService.class);

    private final LuckyTreasureDao luckyTreasureDao;
    private final LuckyTreasureRedisDao luckyTreasureRedisDao;
    private final RedisLock redisLock;
    private final PlayerPackService playerPackService;

    public LuckyTreasureService(LuckyTreasureDao luckyTreasureDao,
                                LuckyTreasureRedisDao luckyTreasureRedisDao,
                                RedisLock redisLock,
                                PlayerPackService playerPackService) {
        this.luckyTreasureDao = luckyTreasureDao;
        this.luckyTreasureRedisDao = luckyTreasureRedisDao;
        this.redisLock = redisLock;
        this.playerPackService = playerPackService;
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
    public ResBuyLuckyTreasure buyLuckyTreasure(PlayerController playerController, long issueNumber, int count) {
        try {
            Player player = playerController.getPlayer();

            // 购买数量无效
            if (count <= 0) {
                return new ResBuyLuckyTreasure(Code.PARAM_ERROR);
            }

            // 获取夺宝奇兵活动数据
            LuckyTreasure treasure = luckyTreasureRedisDao.getTreasureByIssueNumber(issueNumber);
            if (treasure == null) {
                return new ResBuyLuckyTreasure(Code.FAIL);
            }

            // 检查活动状态
            if (LuckyTreasureStatusUtil.calculateStatus(treasure, player.getId()) != LuckyTreasureStatusUtil.STATUS_CAN_BUY) {
                return new ResBuyLuckyTreasure(Code.FAIL);
            }

            // 检查剩余数量
            int remainingCount = treasure.getConfig().getTotal() - treasure.getSoldCount();
            if (remainingCount < count) {
                return new ResBuyLuckyTreasure(Code.FAIL);
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

            // 先扣除玩家道具
            CommonResult<ItemOperationResult> deductResult = playerPackService.removeItems(player, consumeMap, "luckyTreasureBuy");
            if (!deductResult.success()) {
                return new ResBuyLuckyTreasure(Code.NOT_ENOUGH_ITEM);
            }

            // 使用读写锁确保购买的一致性
            String lockKey = LuckyTreasureConstant.RedisLock.LUCKY_TREASURE_BUY + issueNumber;

            try {
                // 获取写锁进行购买操作
                RLock writeLock = redisLock.getWriteLock(lockKey, 100);
                if (writeLock == null) {
                    // 获取锁失败，退还道具
                    playerPackService.addItems(player.getId(), consumeMap, "luckyTreasureBuyRollback");
                    return new ResBuyLuckyTreasure(Code.FAIL);
                }

                try {
                    return executeBuyWithLock(player, treasure, count, consumeMap);
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
            return new ResBuyLuckyTreasure(Code.EXCEPTION);
        }
    }

    /**
     * 执行购买逻辑（在写锁内执行，道具已扣除）
     */
    private ResBuyLuckyTreasure executeBuyWithLock(Player player, LuckyTreasure treasure, int count, Map<Integer, Long> consumeMap) {
        try {
            // 重新获取最新数据
            LuckyTreasure latestTreasure = luckyTreasureRedisDao.getTreasureByIssueNumber(treasure.getIssueNumber());
            if (latestTreasure == null || LuckyTreasureStatusUtil.calculateStatus(latestTreasure, player.getId()) != LuckyTreasureStatusUtil.STATUS_CAN_BUY) {
                // 活动状态已变更，退还道具
                playerPackService.addItems(player.getId(), consumeMap, "luckyTreasureBuyStatusChangedRollback");
                return new ResBuyLuckyTreasure(Code.FAIL); // 活动状态已变更
            }

            // 检查剩余数量
            int remainingCount = latestTreasure.getConfig().getTotal() - latestTreasure.getSoldCount();
            if (remainingCount < count) {
                // 剩余数量不足，退还道具
                playerPackService.addItems(player.getId(), consumeMap, "luckyTreasureBuyNotEnoughRollback");
                return new ResBuyLuckyTreasure(Code.FAIL); // 剩余数量不足
            }

            // 执行购买
            boolean buySuccess = luckyTreasureRedisDao.buyTreasure(latestTreasure.getIssueNumber(), player.getId(), count);

            if (!buySuccess) {
                // 购买失败，退还道具
                playerPackService.addItems(player.getId(), consumeMap, "luckyTreasureBuyFailedRollback");
                return new ResBuyLuckyTreasure(Code.FAIL); // 购买失败
            }

            // 购买成功，更新数据库
            luckyTreasureDao.save(latestTreasure);

            // 返回成功结果
            ResBuyLuckyTreasure response = new ResBuyLuckyTreasure(Code.SUCCESS);
            response.setIssueNumber(latestTreasure.getIssueNumber());
            response.setBuyCount(count);
            response.setRemainingCount(latestTreasure.getConfig().getTotal() - latestTreasure.getSoldCount());
            response.setStatus(LuckyTreasureStatusUtil.calculateStatus(latestTreasure, player.getId()));

            log.info("夺宝奇兵购买成功, 玩家ID:{}, 期号:{}, 购买数量:{}", player.getId(), latestTreasure.getIssueNumber(), count);

            return response;
        } catch (Exception e) {
            log.error("执行购买逻辑失败", e);
            // 发生异常，退还道具
            playerPackService.addItems(player.getId(), consumeMap, "luckyTreasureBuyExceptionRollback");
            return new ResBuyLuckyTreasure(Code.EXCEPTION);
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
        List<LuckyTreasureConsumeInfo> consumeInfoList = getLuckyTreasureConsumeInfos(config);
        info.setConsumeInfoList(consumeInfoList);

        return info;
    }

    private List<LuckyTreasureConsumeInfo> getLuckyTreasureConsumeInfos(LuckyTreasureConfig config) {
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
