package com.jjg.game.poker.game.common.cardlib;

import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.PoolResultsCfg;
import com.jjg.game.sampledata.bean.Room_ChessCfg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Poker 牌库通用管理器基类
 * 水池控制参考 slots 的 {@code AbstractSlotsGameManager}
 * <p>
 * 职责：
 * <ul>
 *     <li>水池余额（init/get/add/diff）</li>
 *     <li>PoolResultsCfg 模型选择（按水池偏差万分比匹配）</li>
 *     <li>分区 key 排序（从 typeProp 提取）</li>
 *     <li>牌库生成模板方法 {@link #generateCardLib(int)}</li>
 * </ul>
 * <p>
 * 子类只需要实现：游戏类型常量、DAO 注入访问、生成前准备、单局模拟
 *
 * @param <T> 牌库条目类型
 * @param <D> DAO 类型
 */
public abstract class AbstractCardLibManager<T extends CardLibEntry, D extends AbstractCardLibDao<T>> {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    private static final BigDecimal TEN_THOUSAND = BigDecimal.valueOf(10000);

    /** 每批写入 Redis 的阈值 */
    private static final int BATCH_SAVE_THRESHOLD = 500;

    // ==================== 抽象方法 ====================

    /**
     * 返回本游戏的 gameType（对应 Room_Chess.xlsx 中的 gameID）
     */
    protected abstract int getGameType();

    /**
     * 返回具体的 DAO 实例（通常子类用 @Autowired 注入后返回该字段）
     */
    protected abstract D getCardLibDao();

    /**
     * 生成前的准备工作，例如预加载配置。
     * 返回 false 表示无法生成，{@link #generateCardLib(int)} 会立即中止。
     */
    protected abstract boolean prepareGeneration();

    /**
     * 模拟单局游戏，返回该局产生的若干牌库条目（可能多条，也可能为空）
     */
    protected abstract List<T> simulateOneGame();

    // ==================== 水池余额操作 ====================

    /**
     * 应用启动时初始化水池余额
     * 读取 Room_Chess.xlsx 中的 initBasePool，按 gameType 过滤后 putIfAbsent 写入 Redis
     */
    public void initPool() {
        try {
            Map<Integer, Room_ChessCfg> cfgMap = GameDataManager.getRoom_ChessCfgMap();
            if (cfgMap == null || cfgMap.isEmpty()) {
                log.warn("Room_ChessCfg 配置为空，跳过水池初始化 gameType={}", getGameType());
                return;
            }
            int gameType = getGameType();
            for (Map.Entry<Integer, Room_ChessCfg> en : cfgMap.entrySet()) {
                Room_ChessCfg cfg = en.getValue();
                if (cfg.getGameID() != gameType) {
                    continue;
                }
                int roomCfgId = cfg.getId();
                long initBasePool = cfg.getInitBasePool();
                if (initBasePool <= 0) {
                    log.warn("Room_ChessCfg roomCfgId={} 的 initBasePool={} 无效，跳过", roomCfgId, initBasePool);
                    continue;
                }
                getCardLibDao().initPoolBalance(gameType, roomCfgId, initBasePool);
            }
            log.info("水池初始化完成 gameType={}", gameType);
        } catch (Exception e) {
            log.error("水池初始化异常 gameType={}", getGameType(), e);
        }
    }

    /**
     * 获取当前水池余额
     */
    public long getPoolBalance(int roomCfgId) {
        return getCardLibDao().getPoolBalance(getGameType(), roomCfgId);
    }

    /**
     * 水池余额增减
     * 正数 = 系统收钱（玩家输），负数 = 系统赔钱（玩家赢）
     *
     * @return 操作后的余额
     */
    public long addPoolBalance(int roomCfgId, long value) {
        return getCardLibDao().addPoolBalance(getGameType(), roomCfgId, value);
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
     * @param roomCfgId 房间配置 ID
     * @return 水池偏差（万分比），initPool 为 0 时返回 0
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
     * 根据水池偏差选择对应的 {@link PoolResultsCfg}（水池模型）
     * 参考 AbstractSlotsGameManager.getLibCfgByPoolDiff
     * <p>
     * 水池偏差落在 [enterLimitMin, enterLimitMax) 区间内的配置
     * 当 enterLimitMin <= -999999 时视为无下限
     * 当 enterLimitMax >= 999999 时视为无上限
     *
     * @param roomCfgId 房间配置 ID（用于获取水池余额和初始值）
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

    /**
     * 根据 gameType 获取所有 PoolResultsCfg
     */
    public List<PoolResultsCfg> getPoolResultsCfgListByGameType() {
        List<PoolResultsCfg> allCfgList = GameDataManager.getPoolResultsCfgList();
        if (allCfgList == null || allCfgList.isEmpty()) {
            return Collections.emptyList();
        }
        int gameType = getGameType();
        return allCfgList.stream()
                .filter(cfg -> cfg.getGameType() == gameType)
                .collect(Collectors.toList());
    }

    /**
     * 获取排序后的分区 Key 列表
     * 从任意一条 PoolResultsCfg 的 typeProp 中提取 key 并排序
     */
    public List<Integer> getSortedSectionKeys() {
        List<PoolResultsCfg> cfgList = getPoolResultsCfgListByGameType();
        if (cfgList.isEmpty()) {
            return Collections.emptyList();
        }
        // 所有 model 的 typeProp key 相同，取第一条即可
        Map<Integer, Integer> typeProp = cfgList.getFirst().getTypeProp();
        if (typeProp == null || typeProp.isEmpty()) {
            return Collections.emptyList();
        }
        List<Integer> keys = new ArrayList<>(typeProp.keySet());
        Collections.sort(keys);
        return keys;
    }

    // ==================== 牌库操作 ====================

    /**
     * 批量生成牌库（模板方法）
     * <p>
     * 执行顺序：
     * 加锁 → prepareGeneration → 取分区 key → newLibName → 循环 simulateOneGame
     * → 分批 batchSaveToRedis → flush → afterSave → clearOldLib → addGenerateTime → 解锁（finally）
     *
     * @param count 模拟局数
     */
    public void generateCardLib(int count) {
        D dao = getCardLibDao();

        // 1. 加锁
        if (!dao.addGenerateLock()) {
            log.warn("牌库正在生成中 gameType={}", getGameType());
            return;
        }

        try {
            log.info("开始生成牌库 gameType={}, count={}", getGameType(), count);
            long startTime = System.currentTimeMillis();

            // 2. 生成前准备（子类加载配置，失败则中止）
            if (!prepareGeneration()) {
                return;
            }

            // 3. 获取分区 key 列表
            List<Integer> sortedSectionKeys = getSortedSectionKeys();
            if (sortedSectionKeys.isEmpty()) {
                log.error("缺少 PoolResultsCfg 配置或 typeProp 为空, gameType={}", getGameType());
                return;
            }

            // 4. 获取新库名
            String newLibName = dao.getNewLibName();

            // 5. 批量模拟生成
            List<T> allLibs = new ArrayList<>();
            int successCount = 0;
            int failCount = 0;
            int totalCount = 0;

            for (int i = 0; i < count; i++) {
                try {
                    List<T> libs = simulateOneGame();
                    if (libs != null && !libs.isEmpty()) {
                        allLibs.addAll(libs);
                        totalCount += libs.size();
                        successCount++;
                    } else {
                        failCount++;
                    }
                } catch (Exception e) {
                    failCount++;
                    log.error("模拟第{}局失败 gameType={}", i + 1, getGameType(), e);
                }

                // 每 BATCH_SAVE_THRESHOLD 条批量写入一次 Redis，避免内存占用过大
                if (allLibs.size() >= BATCH_SAVE_THRESHOLD) {
                    dao.batchSaveToRedis(newLibName, allLibs, sortedSectionKeys);
                    allLibs.clear();
                }
            }

            // 6. 剩余的写入 Redis
            if (!allLibs.isEmpty()) {
                dao.batchSaveToRedis(newLibName, allLibs, sortedSectionKeys);
            }

            // 7. 切换到新库
            dao.afterSave(newLibName);

            // 8. 清理旧库
            dao.clearOldLib();

            // 9. 记录生成时间
            dao.addGenerateTime();

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("牌库生成完成 gameType={}, 成功={}局, 失败={}局, 总条目={}条, 分区数={}, 耗时={}ms",
                    getGameType(), successCount, failCount, totalCount, sortedSectionKeys.size(), elapsed);

        } catch (Exception e) {
            log.error("生成牌库异常 gameType={}", getGameType(), e);
        } finally {
            // 10. 解锁
            dao.removeGenerateLock();
        }
    }

    /**
     * 从牌库中根据 sectionKey 获取一条记录
     *
     * @param sectionKey 分区 key（来自 typeProp 配置）
     * @return 牌库条目，可能为 null
     */
    public T getCardLib(int sectionKey) {
        return getCardLibDao().getCardLib(sectionKey);
    }

    /**
     * 检查牌库是否存在
     */
    public boolean hasCardLib() {
        return getCardLibDao().hasCardLib();
    }

    /**
     * 检查是否正在生成
     */
    public boolean isGenerating() {
        return getCardLibDao().hasGenerateLock();
    }
}
