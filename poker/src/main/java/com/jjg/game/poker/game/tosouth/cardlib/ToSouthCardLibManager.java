package com.jjg.game.poker.game.tosouth.cardlib;

import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.PoolResultsCfg;
import com.jjg.game.sampledata.bean.Room_ChessCfg;
import com.jjg.game.sampledata.bean.SouthernMoneyCfg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 南方前进牌库管理器
 * 负责牌库的生成、获取等管理操作
 * 水池控制参考 slots 的 AbstractSlotsGameManager
 */
@Component
public class ToSouthCardLibManager {
    private static final Logger log = LoggerFactory.getLogger(ToSouthCardLibManager.class);

    /** 南方前进游戏ID */
    private static final int GAME_TYPE_TO_SOUTH = 300400;

    private static final BigDecimal TEN_THOUSAND = BigDecimal.valueOf(10000);

    @Autowired
    private ToSouthCardLibDao cardLibDao;

    /**
     * 应用启动时初始化水池余额
     */
    @PostConstruct
    public void init() {
        initPool();
    }

    /**
     * 初始化水池余额（参考 SlotsPoolDao.initPool）
     * 读取 Room_Chess.xlsx 中的 initBasePool，使用 putIfAbsent 写入 Redis
     */
    public void initPool() {
        try {
            Map<Integer, Room_ChessCfg> cfgMap = GameDataManager.getRoom_ChessCfgMap();
            if (cfgMap == null || cfgMap.isEmpty()) {
                log.warn("Room_ChessCfg 配置为空，跳过水池初始化");
                return;
            }
            for (Map.Entry<Integer, Room_ChessCfg> en : cfgMap.entrySet()) {
                Room_ChessCfg cfg = en.getValue();
                if (cfg.getGameID() != GAME_TYPE_TO_SOUTH) {
                    continue;
                }
                int roomCfgId = cfg.getId();
                long initBasePool = cfg.getInitBasePool();
                if (initBasePool <= 0) {
                    log.warn("Room_ChessCfg roomCfgId={} 的 initBasePool={} 无效，跳过", roomCfgId, initBasePool);
                    continue;
                }
                cardLibDao.initPoolBalance(GAME_TYPE_TO_SOUTH, roomCfgId, initBasePool);
            }
            log.info("南方前进水池初始化完成");
        } catch (Exception e) {
            log.error("南方前进水池初始化异常", e);
        }
    }

    // ==================== 水池余额操作 ====================

    /**
     * 获取当前水池余额
     */
    public long getPoolBalance(int roomCfgId) {
        return cardLibDao.getPoolBalance(GAME_TYPE_TO_SOUTH, roomCfgId);
    }

    /**
     * 水池余额增减
     * 正数=系统收钱（玩家输）, 负数=系统赔钱（玩家赢）
     *
     * @return 操作后的余额
     */
    public long addPoolBalance(int roomCfgId, long value) {
        return cardLibDao.addPoolBalance(GAME_TYPE_TO_SOUTH, roomCfgId, value);
    }

    /**
     * 获取水池初始值（来自 Room_Chess.xlsx 的 initBasePool）
     */
    public long getPoolInit(int roomCfgId) {
        Room_ChessCfg cfg = GameDataManager.getRoom_ChessCfg(roomCfgId);
        if (cfg == null) {
            log.warn("Room_ChessCfg 不存在 roomCfgId={}", roomCfgId);
            return 0;
        }
        return cfg.getInitBasePool();
    }

    /**
     * 计算水池偏差值（万分比）
     * 公式: diff = ((currentPool - initPool) / initPool) * 10000
     * 参考 AbstractSlotsGameManager.getLibCfg
     *
     * @param roomCfgId 房间配置ID
     * @return 水池偏差（万分比），initPool为0时返回0
     */
    public long getPoolDiff(int roomCfgId) {
        long poolInit = getPoolInit(roomCfgId);
        if (poolInit <= 0) {
            return 0;
        }
        long poolValue = getPoolBalance(roomCfgId);
        return BigDecimal.valueOf(poolValue - poolInit)
                .divide(BigDecimal.valueOf(poolInit), 6, RoundingMode.HALF_UP)
                .multiply(TEN_THOUSAND)
                .setScale(0, RoundingMode.HALF_UP)
                .longValue();
    }

    /**
     * 根据水池偏差选择对应的 PoolResultsCfg（水池模型）
     * 参考 AbstractSlotsGameManager.getLibCfgByPoolDiff
     *
     * 水池偏差落在 [enterLimitMin, enterLimitMax) 区间内的配置
     * 当 enterLimitMin <= -999999 时视为无下限
     * 当 enterLimitMax >= 999999 时视为无上限
     *
     * @param roomCfgId 房间配置ID（用于获取水池余额和初始值）
     * @return 匹配的配置，未找到则返回兜底配置
     */
    public PoolResultsCfg selectPoolResultsCfg(int roomCfgId) {
        List<PoolResultsCfg> cfgList = getPoolResultsCfgListByGameType();
        if (cfgList.isEmpty()) {
            return null;
        }

        long diff = getPoolDiff(roomCfgId);

        for (PoolResultsCfg cfg : cfgList) {
            boolean minOk = diff >= cfg.getEnterLimitMin() || cfg.getEnterLimitMin() <= -999999;
            boolean maxOk = diff < cfg.getEnterLimitMax() || cfg.getEnterLimitMax() >= 999999;
            if (minOk && maxOk) {
                log.debug("水池模型匹配 roomCfgId={}, diff={}, modelId={}, enterLimit=[{}, {})",
                        roomCfgId, diff, cfg.getModelId(), cfg.getEnterLimitMin(), cfg.getEnterLimitMax());
                return cfg;
            }
        }

        // 未匹配到，返回标准池(modelId=4)
        for (PoolResultsCfg cfg : cfgList) {
            if (cfg.getModelId() == 4) {
                log.warn("水池偏差 {} 未匹配到任何模型，使用默认 modelId=4, roomCfgId={}", diff, roomCfgId);
                return cfg;
            }
        }
        // 兜底：返回第一条
        log.warn("水池偏差 {} 未匹配到任何模型且无默认模型，使用第一条, roomCfgId={}", diff, roomCfgId);
        return cfgList.getFirst();
    }

    // ==================== 牌库操作 ====================

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
