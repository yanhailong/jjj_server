package com.jjg.game.poker.game.tosouth.cardlib;

import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.PoolResultsCfg;
import com.jjg.game.sampledata.bean.SouthernMoneyCfg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 南方前进牌库管理器
 * 负责牌库的生成、获取等管理操作
 */
@Component
public class ToSouthCardLibManager {
    private static final Logger log = LoggerFactory.getLogger(ToSouthCardLibManager.class);

    /** 南方前进游戏ID */
    private static final int GAME_TYPE_TO_SOUTH = 300400;


    @Autowired
    private ToSouthCardLibDao cardLibDao;


    /**
     * 批量生成牌库
     *
     * @param count 生成局数（每局产生2条记录）
     */
    public void generateCardLib(int count) {
        // 1. 加锁
        if (!cardLibDao.addGenerateLock()) {
            log.warn("牌库正在生成中");
            return;
        }

        try {
            log.info("开始生成南方前进牌库 count={}", count);
            long startTime = System.currentTimeMillis();

            // 获取配置
            SouthernMoneyCfg moneyCfg = getFirstSouthernMoneyCfg();
            if (moneyCfg == null) {
                log.error("缺少SouthernMoneyCfg配置");
                return;
            }
            int poolId = moneyCfg.getPoolId();

            // 获取分区key列表(从PoolResultsCfg的typeProp提取)
            List<Integer> sortedSectionKeys = getSortedSectionKeys();
            if (sortedSectionKeys.isEmpty()) {
                log.error("缺少PoolResultsCfg配置或typeProp为空, gameType={}", GAME_TYPE_TO_SOUTH);
                return;
            }

            // 2. 获取新库名
            String newLibName = cardLibDao.getNewLibName();

            // 3. 批量模拟生成
            List<ToSouthCardLib> allLibs = new ArrayList<>();
            int successCount = 0;
            int failCount = 0;

            for (int i = 0; i < count; i++) {
                try {
                    List<ToSouthCardLib> libs = ToSouthCardLibGenerator.simulateOneGame(poolId, moneyCfg);
                    if (libs != null && !libs.isEmpty()) {
                        allLibs.addAll(libs);
                        successCount++;
                    } else {
                        failCount++;
                    }
                } catch (Exception e) {
                    failCount++;
                    log.error("模拟第{}局失败", i + 1, e);
                }

                // 每500条批量写入一次Redis，避免内存占用过大
                if (allLibs.size() >= 500) {
                    cardLibDao.batchSaveToRedis(newLibName, allLibs, sortedSectionKeys);
                    allLibs.clear();
                }
            }

            // 4. 剩余的写入Redis
            if (!allLibs.isEmpty()) {
                cardLibDao.batchSaveToRedis(newLibName, allLibs, sortedSectionKeys);
            }

            // 5. 切换到新库
            cardLibDao.afterSave(newLibName);

            // 6. 清理旧库
            cardLibDao.clearOldLib();

            // 7. 记录生成时间
            cardLibDao.addGenerateTime();

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("南方前进牌库生成完成 成功={}局, 失败={}局, 总条目={}条, 分区数={}, 耗时={}ms",
                    successCount, failCount, successCount * 2, sortedSectionKeys.size(), elapsed);

        } catch (Exception e) {
            log.error("生成南方前进牌库异常", e);
        } finally {
            // 解锁
            cardLibDao.removeGenerateLock();
        }
    }

    /**
     * 从牌库中根据sectionKey获取一条记录
     *
     * @param sectionKey 分区key(来自typeProp配置)
     * @return 牌库条目，可能为null
     */
    public ToSouthCardLib getCardLib(int sectionKey) {
        return cardLibDao.getCardLib(sectionKey);
    }

    /**
     * 检查牌库是否存在
     */
    public boolean hasCardLib() {
        return cardLibDao.hasCardLib();
    }

    /**
     * 检查是否正在生成
     */
    public boolean isGenerating() {
        return cardLibDao.hasGenerateLock();
    }

    /**
     * 获取第一条SouthernMoneyCfg配置（用于牌库生成）
     */
    private SouthernMoneyCfg getFirstSouthernMoneyCfg() {
        List<SouthernMoneyCfg> list = GameDataManager.getSouthernMoneyCfgList();
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list.getFirst();
    }

    /**
     * 获取排序后的分区Key列表
     * 从任意一条PoolResultsCfg的typeProp中提取key并排序
     */
    public List<Integer> getSortedSectionKeys() {
        List<PoolResultsCfg> cfgList = getPoolResultsCfgListByGameType();
        if (cfgList.isEmpty()) {
            return Collections.emptyList();
        }
        // 所有model的typeProp key相同，取第一条即可
        Map<Integer, Integer> typeProp = cfgList.getFirst().getTypeProp();
        if (typeProp == null || typeProp.isEmpty()) {
            return Collections.emptyList();
        }
        List<Integer> keys = new ArrayList<>(typeProp.keySet());
        Collections.sort(keys);
        return keys;
    }

    /**
     * 根据gameType获取所有PoolResultsCfg
     */
    public List<PoolResultsCfg> getPoolResultsCfgListByGameType() {
        List<PoolResultsCfg> allCfgList = GameDataManager.getPoolResultsCfgList();
        if (allCfgList == null || allCfgList.isEmpty()) {
            return Collections.emptyList();
        }
        return allCfgList.stream()
                .filter(cfg -> cfg.getGameType() == GAME_TYPE_TO_SOUTH)
                .collect(Collectors.toList());
    }

    /**
     * 根据玩家总盈亏选择对应的PoolResultsCfg (modelId)
     * 玩家总盈亏落在 [enterLimitMin, enterLimitMax) 区间内的配置
     *
     * @param playerTotalProfit 玩家在南方前进的总盈亏
     * @return 匹配的配置，未找到则返回默认(modelId=4,标准池)
     */
    public PoolResultsCfg selectPoolResultsCfg(long playerTotalProfit) {
        List<PoolResultsCfg> cfgList = getPoolResultsCfgListByGameType();
        if (cfgList.isEmpty()) {
            return null;
        }

        for (PoolResultsCfg cfg : cfgList) {
            if (playerTotalProfit >= cfg.getEnterLimitMin() && playerTotalProfit < cfg.getEnterLimitMax()) {
                return cfg;
            }
        }

        // 未匹配到，返回标准池(modelId=4)
        for (PoolResultsCfg cfg : cfgList) {
            if (cfg.getModelId() == 4) {
                return cfg;
            }
        }
        // 兜底：返回第一条
        return cfgList.getFirst();
    }

    // ==================== 玩家统计数据（持久化到Redis） ====================

    /**
     * 获取玩家连赢/连输值
     */
    public int getPlayerWinStreak(long playerId) {
        return cardLibDao.getPlayerWinStreak(playerId);
    }

    /**
     * 设置玩家连赢/连输值
     */
    public void setPlayerWinStreak(long playerId, int streak) {
        cardLibDao.setPlayerWinStreak(playerId, streak);
    }

    /**
     * 获取玩家总盈亏
     */
    public long getPlayerTotalProfit(long playerId) {
        return cardLibDao.getPlayerTotalProfit(playerId);
    }

    /**
     * 累加玩家总盈亏
     */
    public void addPlayerTotalProfit(long playerId, long delta) {
        cardLibDao.addPlayerTotalProfit(playerId, delta);
    }
}
