package com.jjg.game.hall.pointsaward.leaderboard;

import com.jjg.game.common.curator.MarsCurator;
import com.jjg.game.common.redis.RedisLock;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.constant.AwardCodeType;
import com.jjg.game.core.data.LanguageParamData;
import com.jjg.game.core.manager.AwardCodeManager;
import com.jjg.game.core.service.MailService;
import com.jjg.game.core.utils.ItemUtils;
import com.jjg.game.hall.pointsaward.PointsAwardLogger;
import com.jjg.game.hall.pointsaward.constant.PointsAwardConstant;
import com.jjg.game.hall.pointsaward.pb.PointsAwardLeaderboardData;
import com.jjg.game.hall.pointsaward.pb.PointsAwardLeaderboardInfo;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.PointsAwardRankingCfg;
import org.apache.commons.lang.StringUtils;
import org.redisson.api.RBucket;
import org.redisson.api.RDeque;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * 积分大奖排行榜管理器
 */
@Component
public class PointsAwardLeaderboardManager {

    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * 积分大奖排行榜配置 k1=类型 k2=名次
     */
    private final ConcurrentHashMap<Integer, Map<Integer, PointsAwardRankingCfg>> configMap = new ConcurrentHashMap<>();

    private final PointsAwardLeaderboardService leaderboardService;
    private final RedisLock redisLock;
    private final MarsCurator marsCurator;
    private final RedissonClient redissonClient;
    private final MailService mailService;
    private final AwardCodeManager awardCodeManager;
    private final PointsAwardLogger pointsAwardLogger;

    // ==================== 构造函数 ====================

    /**
     * 构造函数 - 依赖注入
     *
     * @param leaderboardService 排行榜服务
     * @param redisLock          Redis锁服务
     * @param marsCurator        主节点选举服务
     * @param redissonClient     Redis客户端
     * @param mailService        邮件服务
     * @param awardCodeManager   奖励码管理器
     * @param pointsAwardLogger  积分奖励日志记录器
     */
    public PointsAwardLeaderboardManager(
            PointsAwardLeaderboardService leaderboardService,
            RedisLock redisLock,
            MarsCurator marsCurator,
            RedissonClient redissonClient,
            @Lazy MailService mailService,
            @Lazy AwardCodeManager awardCodeManager,
            PointsAwardLogger pointsAwardLogger) {
        this.leaderboardService = leaderboardService;
        this.redisLock = redisLock;
        this.marsCurator = marsCurator;
        this.redissonClient = redissonClient;
        this.mailService = mailService;
        this.awardCodeManager = awardCodeManager;
        this.pointsAwardLogger = pointsAwardLogger;
    }

    // ==================== 初始化方法 ====================

    /**
     * 初始化排行榜管理器
     *
     * <p>加载配置并初始化各类型排行榜</p>
     */
    public void init() {
        try {
            log.info("开始初始化积分奖励排行榜管理器");

            initConfig();
            // 初始化服务的管理器引用
            leaderboardService.init(this);
            initLeaderboards();
            cacheRankData();

            log.info("积分奖励排行榜管理器初始化完成");
        } catch (Exception e) {
            log.error("积分奖励排行榜管理器初始化失败", e);
            throw new RuntimeException("排行榜管理器初始化失败", e);
        }
    }

    /**
     * 初始化配置
     */
    private void initConfig() {
        try {
            reloadConfig();
            log.debug("排行榜配置初始化完成");
        } catch (Exception e) {
            log.error("排行榜配置初始化失败", e);
            throw e;
        }
    }

    /**
     * 重新加载配置
     */
    public void reloadConfig() {
        try {
            log.debug("开始重新加载排行榜配置");

            List<PointsAwardRankingCfg> configList = GameDataManager.getPointsAwardRankingCfgList();
            if (configList == null || configList.isEmpty()) {
                log.warn("排行榜配置列表为空");
                return;
            }

            // 清空旧配置
            configMap.clear();

            // 按类型分组配置
            Arrays.asList(
                    PointsAwardConstant.Leaderboard.TYPE_MONTH,
                    PointsAwardConstant.Leaderboard.AM,
                    PointsAwardConstant.Leaderboard.PM
            ).forEach(type -> {
                Map<Integer, PointsAwardRankingCfg> typeConfig = filterConfig(type, configList, LocalDate.now());
                if (!typeConfig.isEmpty()) {
                    configMap.put(type, typeConfig);
                    log.debug("加载排行榜配置，类型: {}, 配置数量: {}", type, typeConfig.size());
                }
            });

            log.info("排行榜配置重新加载完成，配置类型数: {}", configMap.size());

        } catch (Exception e) {
            log.error("重新加载排行榜配置失败", e);
            throw e;
        }
    }

    /**
     * 初始化排行榜
     */
    private void initLeaderboards() {
        try {
            long currentTime = getCurrentTimeMillis();

            // 只初始化有配置的排行榜类型
            if (configMap.containsKey(PointsAwardConstant.Leaderboard.AM)) {
                initOrSettleType(PointsAwardConstant.Leaderboard.AM, currentTime);
            }
            if (configMap.containsKey(PointsAwardConstant.Leaderboard.PM)) {
                initOrSettleType(PointsAwardConstant.Leaderboard.PM, currentTime);
            }
            if (configMap.containsKey(PointsAwardConstant.Leaderboard.TYPE_MONTH)) {
                initOrSettleType(PointsAwardConstant.Leaderboard.TYPE_MONTH, currentTime);
            }

            log.debug("所有排行榜初始化完成");
        } catch (Exception e) {
            log.error("排行榜初始化失败", e);
            throw e;
        }
    }

    /**
     * 缓存排行榜数据
     */
    public void cacheRankData() {
        if (configMap.containsKey(PointsAwardConstant.Leaderboard.AM)) {
            leaderboardService.loadRank(PointsAwardConstant.Leaderboard.AM);
        }
        if (configMap.containsKey(PointsAwardConstant.Leaderboard.PM)) {
            leaderboardService.loadRank(PointsAwardConstant.Leaderboard.PM);
        }
        if (configMap.containsKey(PointsAwardConstant.Leaderboard.TYPE_MONTH)) {
            leaderboardService.loadRank(PointsAwardConstant.Leaderboard.TYPE_MONTH);
        }
        log.debug("缓存排行榜数据完成");
    }

    // ==================== 定时任务方法 ====================

    /**
     * 根据指定的小时数执行相应的业务逻辑。
     * 12 点：保存上午榜快照
     * 0 点：保存下午榜（全天最终）快照并清空日榜；若为每月第一天，保存上月榜并清空月榜
     */
    public void clock(int hour) {
        try {
            if (isMaster()) {
                long currentTime = getCurrentTimeMillis();
                if (hour == 12) {
                    handleNoonSettlement(currentTime);
                } else if (hour == 0) {
                    handleMidnightSettlement(currentTime);
                }
            }

            // 重载配置
            if (hour == 0) {
                reloadConfig();
            }

        } catch (Exception e) {
            log.error("定时任务执行失败，小时: {}", hour, e);
        }
    }

    /**
     * 处理中午12点的结算逻辑
     */
    private void handleNoonSettlement(long currentTime) {
        try {
            // 12 点：结算上午榜并开启下午榜
            if (configMap.containsKey(PointsAwardConstant.Leaderboard.AM)) {
                snapshotUnderLock(PointsAwardConstant.Leaderboard.AM, true);
                leaderboardService.reset(PointsAwardConstant.Leaderboard.AM);
            }

            // 开启下午榜周期并确保为空
            if (configMap.containsKey(PointsAwardConstant.Leaderboard.PM)) {
                startNewLeaderboardPeriod(PointsAwardConstant.Leaderboard.PM, currentTime);
                leaderboardService.reset(PointsAwardConstant.Leaderboard.PM);
            }

            log.info("中午12点结算完成");
        } catch (Exception e) {
            log.error("中午12点结算失败", e);
        }
    }

    /**
     * 处理午夜0点的结算逻辑
     */
    private void handleMidnightSettlement(long currentTime) {
        try {
            // 0 点：结算下午榜并开启新一天上午榜
            if (configMap.containsKey(PointsAwardConstant.Leaderboard.PM)) {
                snapshotUnderLock(PointsAwardConstant.Leaderboard.PM, true);
                leaderboardService.reset(PointsAwardConstant.Leaderboard.PM);
            }

            if (configMap.containsKey(PointsAwardConstant.Leaderboard.AM)) {
                startNewLeaderboardPeriod(PointsAwardConstant.Leaderboard.AM, currentTime);
                leaderboardService.reset(PointsAwardConstant.Leaderboard.AM);
            }

            // 每月第一天的 0 点，保存上月榜并清空月榜
            if (isFirstDayOfMonth() && configMap.containsKey(PointsAwardConstant.Leaderboard.TYPE_MONTH)) {
                snapshotUnderLock(PointsAwardConstant.Leaderboard.TYPE_MONTH, true);
                leaderboardService.reset(PointsAwardConstant.Leaderboard.TYPE_MONTH);
                startNewLeaderboardPeriod(PointsAwardConstant.Leaderboard.TYPE_MONTH, currentTime);
            }

            log.info("午夜0点结算完成，是否月初: {}", isFirstDayOfMonth());
        } catch (Exception e) {
            log.error("午夜0点结算失败", e);
        }
    }

    /**
     * 开始新的排行榜周期
     *
     * @param type        排行榜类型
     * @param currentTime 当前时间
     */
    private void startNewLeaderboardPeriod(int type, long currentTime) {
        try {
            String startTimeKey = buildStartTimeKey(type);
            redissonClient.getBucket(startTimeKey).set(currentTime);

            log.debug("开始新的排行榜周期，类型: {}, 开始时间: {}", type,
                    LocalDateTime.ofInstant(Instant.ofEpochMilli(currentTime), ZoneId.systemDefault()));
        } catch (Exception e) {
            log.error("开始新排行榜周期失败，类型: {}", type, e);
        }
    }

    /**
     * 筛选指定类型的配置
     *
     * @param type       排行榜类型
     * @param configList 配置列表
     * @return 筛选后的配置映射
     */
    private Map<Integer, PointsAwardRankingCfg> filterConfig(int type, List<PointsAwardRankingCfg> configList, LocalDate now) {
        Map<Integer, PointsAwardRankingCfg> configMap = new HashMap<>();

        try {
            YearMonth nowYearMonth = YearMonth.from(now);

            // 筛选需要的配置
            List<PointsAwardRankingCfg> filteredConfigs = configList.stream()
                    .filter(cfg -> cfg.getType() == type)
                    .filter(cfg -> isConfigTimeValid(cfg, type, now, nowYearMonth))
                    .toList();

            // 如果没有找到时间匹配的配置，使用默认配置
            if (filteredConfigs.isEmpty()) {
                filteredConfigs = configList.stream()
                        .filter(cfg -> cfg.getType() == type)
                        .filter(cfg -> cfg.getTime() == null || cfg.getTime().trim().isEmpty())
                        .toList();
            }

            // 构建配置映射
            filteredConfigs.forEach(cfg -> buildConfigMapping(cfg, configMap));

        } catch (Exception e) {
            log.error("筛选配置失败，类型: {}", type, e);
        }

        return configMap;
    }

    /**
     * 检查配置时间是否有效
     */
    private boolean isConfigTimeValid(PointsAwardRankingCfg cfg, int type, LocalDate now, YearMonth nowYearMonth) {
        String cfgTime = cfg.getTime();
        if (cfgTime == null || cfgTime.trim().isEmpty()) {
            return false;
        }

        long timeMills = TimeHelper.getTimestamp(cfgTime.trim());
        if (timeMills <= 0) {
            return false;
        }

        LocalDateTime configDate = LocalDateTime.ofInstant(Instant.ofEpochMilli(timeMills), ZoneId.systemDefault());

        return switch (type) {
            case PointsAwardConstant.Leaderboard.TYPE_MONTH -> {
                YearMonth configYearMonth = YearMonth.from(configDate);
                yield configYearMonth.equals(nowYearMonth);
            }
            case PointsAwardConstant.Leaderboard.AM -> {
                LocalDateTime amStart = now.atTime(0, 0, 0);
                LocalDateTime pmStart = now.atTime(PointsAwardConstant.Leaderboard.TimeConstants.PM_START_HOUR, 0, 0);
                yield (configDate.isAfter(amStart) || configDate.isEqual(amStart)) && configDate.isBefore(pmStart);
            }
            case PointsAwardConstant.Leaderboard.PM -> {
                LocalDateTime pmStart = now.atTime(PointsAwardConstant.Leaderboard.TimeConstants.PM_START_HOUR, 0, 0);
                LocalDateTime pmEnd = now.atTime(PointsAwardConstant.Leaderboard.TimeConstants.DAY_END_HOUR, PointsAwardConstant.Leaderboard.TimeConstants.DAY_END_MINUTE, PointsAwardConstant.Leaderboard.TimeConstants.DAY_END_SECOND);
                yield (configDate.isAfter(pmStart) || configDate.isEqual(pmStart)) && configDate.isBefore(pmEnd);
            }
            default -> false;
        };
    }

    /**
     * 构建配置映射
     */
    private void buildConfigMapping(PointsAwardRankingCfg cfg, Map<Integer, PointsAwardRankingCfg> configMap) {
        List<Integer> ranking = cfg.getRanking();
        if (ranking != null && !ranking.isEmpty()) {
            int size = ranking.size();
            int min = ranking.getFirst();
            if (size >= 2) {
                int max = ranking.getLast();
                for (int i = min; i <= max; i++) {
                    configMap.put(i, cfg);
                }
            } else {
                configMap.put(min, cfg);
            }
        }
    }

    /**
     * 获取排行榜配置
     *
     * @param type 排行榜类型
     */
    public Map<Integer, PointsAwardRankingCfg> getRankingCfgMap(int type) {
        return configMap.get(type);
    }

    /**
     * 根据排行榜类型获取排行榜最大人数
     *
     * @param type 排行榜类型
     * @return 最大人数
     */
    public int getMaxSize(int type) {
        Map<Integer, PointsAwardRankingCfg> map = configMap.get(type);
        return map != null ? map.size() : 0;
    }

    /**
     * 获取排行榜结束时间
     */
    public long getEndTime(int rankType) {
        try {
            RBucket<Long> bucket = redissonClient.getBucket(buildStartTimeKey(rankType));
            Long startTime = bucket.get();

            if (startTime == null) {
                log.warn("排行榜开始时间不存在，类型: {}", rankType);
                return getCurrentTimeMillis();
            }

            return calculateEndTime(rankType, getCurrentTimeMillis(), startTime);

        } catch (Exception e) {
            log.error("获取排行榜结束时间失败，类型: {}", rankType, e);
            return getCurrentTimeMillis();
        }
    }

    /**
     * 计算排行榜结束时间
     */
    private long calculateEndTime(int type, long currentTime, long startTime) {
        LocalDateTime startDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(startTime), ZoneId.systemDefault());
        LocalDate startDate = startDateTime.toLocalDate();

        return switch (type) {
            case PointsAwardConstant.Leaderboard.AM -> {
                LocalDateTime endTime = startDate.atTime(PointsAwardConstant.Leaderboard.TimeConstants.AM_END_HOUR, PointsAwardConstant.Leaderboard.TimeConstants.AM_END_MINUTE, PointsAwardConstant.Leaderboard.TimeConstants.AM_END_SECOND);
                yield endTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            }
            case PointsAwardConstant.Leaderboard.PM -> {
                LocalDateTime endTime = startDate.atTime(PointsAwardConstant.Leaderboard.TimeConstants.DAY_END_HOUR, PointsAwardConstant.Leaderboard.TimeConstants.DAY_END_MINUTE, PointsAwardConstant.Leaderboard.TimeConstants.DAY_END_SECOND);
                yield endTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            }
            case PointsAwardConstant.Leaderboard.TYPE_MONTH -> {
                LocalDateTime endOfMonth = startDate
                        .with(TemporalAdjusters.lastDayOfMonth())
                        .atTime(PointsAwardConstant.Leaderboard.TimeConstants.DAY_END_HOUR, PointsAwardConstant.Leaderboard.TimeConstants.DAY_END_MINUTE, PointsAwardConstant.Leaderboard.TimeConstants.DAY_END_SECOND);
                yield endOfMonth.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            }
            default -> currentTime + PointsAwardConstant.Leaderboard.TimeConstants.TWELVE_HOURS_MILLIS;
        };
    }

    /**
     * 初始化或结算指定类型的排行榜
     *
     * @param type        排行榜类型
     * @param currentTime 当前时间
     */
    public void initOrSettleType(int type, long currentTime) {
        try {
            if (isNotMaster()) {
                log.debug("非主节点，跳过排行榜处理，类型: {}", type);
                return;
            }

            // 检查是否有对应的配置
            if (!configMap.containsKey(type)) {
                log.debug("排行榜类型 {} 没有配置，跳过初始化", type);
                return;
            }

            String lockKey = PointsAwardConstant.RedisLockKey.POINTS_AWARD_RANKING_LOCK + type;
            redisLock.lockAndRun(lockKey, PointsAwardConstant.Leaderboard.LOCK_LEASE_MILLIS, () -> processLeaderboardInitOrSettle(type, currentTime));
        } catch (Exception e) {
            log.error("初始化或结算排行榜失败，类型: {}", type, e);
        }
    }

    /**
     * 处理排行榜初始化或结算
     *
     * @param type        排行榜类型
     * @param currentTime 当前时间
     */
    private void processLeaderboardInitOrSettle(int type, long currentTime) {
        String startTimeKey = buildStartTimeKey(type);
        RBucket<Long> bucket = redissonClient.getBucket(startTimeKey);
        Long startTime = bucket.get();

        if (startTime == null) {
            // 初始化新排行榜
            initializeNewLeaderboard(type, currentTime, startTimeKey);
        } else {
            // 检查是否需要结算
            long endTime = calculateEndTime(type, currentTime, startTime);
            if (currentTime >= endTime) {
                log.info("检测到排行榜过期，开始结算，类型: {}, 开始时间: {}, 结束时间: {}, 当前时间: {}",
                        type,
                        LocalDateTime.ofInstant(Instant.ofEpochMilli(startTime), ZoneId.systemDefault()),
                        LocalDateTime.ofInstant(Instant.ofEpochMilli(endTime), ZoneId.systemDefault()),
                        LocalDateTime.ofInstant(Instant.ofEpochMilli(currentTime), ZoneId.systemDefault()));
                settleAndResetLeaderboard(type, currentTime, startTimeKey, endTime);
            } else {
                log.debug("排行榜未过期，类型: {}, 结束时间: {}, 当前时间: {}",
                        type,
                        LocalDateTime.ofInstant(Instant.ofEpochMilli(endTime), ZoneId.systemDefault()),
                        LocalDateTime.ofInstant(Instant.ofEpochMilli(currentTime), ZoneId.systemDefault()));
            }
        }
    }

    /**
     * 初始化新排行榜
     *
     * @param type         排行榜类型
     * @param currentTime  当前时间
     * @param startTimeKey 开始时间键
     */
    private void initializeNewLeaderboard(int type, long currentTime, String startTimeKey) {
        redissonClient.getBucket(startTimeKey).set(currentTime);
        log.info("初始化新排行榜，类型: {}, 开始时间: {}", type,
                LocalDateTime.ofInstant(Instant.ofEpochMilli(currentTime), ZoneId.systemDefault()));
    }

    /**
     * 结算并重置排行榜
     *
     * @param type         排行榜类型
     * @param currentTime  当前时间
     * @param startTimeKey 开始时间键
     * @param endTime      结束时间
     */
    private void settleAndResetLeaderboard(int type, long currentTime, String startTimeKey, long endTime) {
        try {
            // 结算当前排行榜
            snapshotUnderLock(type, false);

            // 重置排行榜数据
            leaderboardService.reset(type);

            // 开始新的排行榜周期
            redissonClient.getBucket(startTimeKey).set(currentTime);

            log.info("排行榜结算并重置完成，类型: {}, 新开始时间: {}", type,
                    LocalDateTime.ofInstant(Instant.ofEpochMilli(currentTime), ZoneId.systemDefault()));

        } catch (Exception e) {
            log.error("结算并重置排行榜失败，类型: {}", type, e);
        }
    }

    /**
     * 在对应排行榜锁下读取 TopN 并持久化快照
     *
     * @param snapshotType 快照类型
     * @param lock         如果外部有锁则传入false，内部则不加锁
     */
    private void snapshotUnderLock(int snapshotType, boolean lock) {
        Supplier<PointsAwardLeaderboardData> supplier = () -> {
            List<PointsAwardLeaderboardInfo> topList = leaderboardService.topN(snapshotType, PointsAwardConstant.Leaderboard.MAX_RANK_SIZE);
            PointsAwardLeaderboardData data = new PointsAwardLeaderboardData();
            data.setRankType(snapshotType);
            data.setEndTime(getEndTime(snapshotType));
            data.setRankingInfoList(topList);
            return data;
        };
        PointsAwardLeaderboardData rankingData;
        if (lock) {
            rankingData = supplier.get();
        } else {
            String lockKey = PointsAwardConstant.RedisLockKey.POINTS_AWARD_RANKING_LOCK + snapshotType;
            rankingData = redisLock.lockAndGet(lockKey, PointsAwardConstant.Leaderboard.LOCK_LEASE_MILLIS, supplier);
        }
        // 发奖
        sendAward(rankingData);
        // 记录排行榜历史记录
        addHistory(rankingData);
    }

    /**
     * 发送排行榜奖励
     *
     * @param rankingData 排行信息
     */
    public void sendAward(PointsAwardLeaderboardData rankingData) {
        List<PointsAwardLeaderboardInfo> rankingInfoList = rankingData.getRankingInfoList();
        if (rankingInfoList == null || rankingInfoList.isEmpty()) {
            log.debug("排行榜数据为空，跳过发奖，类型: {}", rankingData.getRankType());
            return;
        }
        //查看排行开始时间
        String startTimeKey = buildStartTimeKey(rankingData.getRankType());
        //这里使用这个时间来计算奖励配置 启动服务器的时候可能结算的排行榜是之前的 但是现在加载的是当前的配置 所以这里单独计算一次
        RBucket<Long> startTimeBucket = redissonClient.getBucket(startTimeKey);
        Long startTime = startTimeBucket.get();
        if (startTime == null) {
            return;
        }
        //排行开始时间
        LocalDate rankDate = LocalDate.ofInstant(Instant.ofEpochMilli(startTime), ZoneId.systemDefault());
        //当前排行榜结算的配置
        Map<Integer, PointsAwardRankingCfg> cfgMap = filterConfig(rankingData.getRankType(), GameDataManager.getPointsAwardRankingCfgList(), rankDate);
        if (cfgMap.isEmpty()) {
            log.warn("排行榜配置为空，跳过发奖，类型: {}", rankingData.getRankType());
            return;
        }

        rankingInfoList.forEach(info -> {
            try {
                sendPlayerAward(info, cfgMap, rankingData);
            } catch (Exception e) {
                log.error("发送玩家奖励失败，玩家ID: {}, 排名: {}", info.getPlayerId(), info.getRank(), e);
            }
        });

        log.info("排行榜奖励发送完成，类型: {}, 发奖人数: {}", rankingData.getRankType(), rankingInfoList.size());
    }

    /**
     * 发送单个玩家奖励
     */
    private void sendPlayerAward(PointsAwardLeaderboardInfo info, Map<Integer, PointsAwardRankingCfg> cfgMap, PointsAwardLeaderboardData rankingData) {
        int rank = info.getRank();
        PointsAwardRankingCfg cfg = cfgMap.get(rank);

        if (cfg == null) {
            log.debug("排名 {} 没有对应配置，跳过发奖，玩家ID: {}", rank, info.getPlayerId());
            return;
        }

        List<String> awardItems = cfg.getGetItem();
        if (awardItems == null || awardItems.isEmpty()) {
            log.debug("排名 {} 没有奖励配置，跳过发奖，玩家ID: {}", rank, info.getPlayerId());
            return;
        }

        String lockKey = PointsAwardConstant.RedisLockKey.PLAYER_RANKING_AWARD_LOCK + info.getPlayerId();
        redisLock.lockAndRun(lockKey, PointsAwardConstant.Leaderboard.LOCK_LEASE_MILLIS, () -> {
            processPlayerAward(info, cfg, rankingData);
        });
    }

    /**
     * 处理玩家奖励
     */
    private void processPlayerAward(PointsAwardLeaderboardInfo info, PointsAwardRankingCfg cfg, PointsAwardLeaderboardData rankingData) {
        String code = null;
        List<LanguageParamData> paramData = buildMailParams(info, rankingData);
        int templateId = getMailTemplateId(rankingData.getRankType(), cfg.getAwardType());

        if (cfg.getAwardType() == PointsAwardConstant.Leaderboard.AwardType.OTHER) {
            // 其他奖励 - 生成领奖码
            mailService.addCfgMail(info.getPlayerId(), templateId, null, paramData);
            code = awardCodeManager.generateCode(info.getPlayerId(), AwardCodeType.POINTS_AWARD);
        } else if (cfg.getAwardType() == PointsAwardConstant.Leaderboard.AwardType.ITEM) {
            // 道具奖励
            mailService.addCfgMail(info.getPlayerId(), templateId, ItemUtils.buildItemsByStrList(cfg.getGetItem()), paramData);
        }

        // 添加历史记录
        leaderboardService.addHistory(info, cfg, code, rankingData.getEndTime());

        log.debug("玩家奖励发送完成，排行榜类型: {}, 玩家ID: {}, 排名: {}, 奖励类型: {}",
                rankingData.getRankType(), info.getPlayerId(), info.getRank(), cfg.getAwardType());
    }

    /**
     * 构建邮件参数
     */
    private List<LanguageParamData> buildMailParams(PointsAwardLeaderboardInfo info, PointsAwardLeaderboardData rankingData) {
        List<LanguageParamData> paramData = new ArrayList<>();
        LocalDate date = LocalDate.ofInstant(Instant.ofEpochMilli(rankingData.getEndTime()), ZoneId.systemDefault());

        paramData.add(new LanguageParamData(0, date.toString()));

        if (rankingData.getRankType() != PointsAwardConstant.Leaderboard.TYPE_MONTH) {
            String rankName = getRankName(rankingData.getRankType());
            paramData.add(new LanguageParamData(0, rankName));
        }

        paramData.add(new LanguageParamData(0, String.valueOf(info.getRank())));

        return paramData;
    }

    /**
     * 获取邮件模板ID
     *
     * @param rankType  排行榜类型
     * @param awardType 奖励类型
     * @return 模板ID
     */
    private int getMailTemplateId(int rankType, int awardType) {
        if (rankType == PointsAwardConstant.Leaderboard.TYPE_MONTH) {
            return awardType == PointsAwardConstant.Leaderboard.AwardType.ITEM
                    ? PointsAwardConstant.Leaderboard.MailTemplateId.MONTHLY_ITEM_AWARD
                    : PointsAwardConstant.Leaderboard.MailTemplateId.MONTHLY_OTHER_AWARD;
        } else {
            return awardType == PointsAwardConstant.Leaderboard.AwardType.ITEM
                    ? PointsAwardConstant.Leaderboard.MailTemplateId.DAILY_ITEM_AWARD
                    : PointsAwardConstant.Leaderboard.MailTemplateId.DAILY_OTHER_AWARD;
        }
    }

    /**
     * 添加排行榜历史记录
     *
     * @param rankingData 排行榜数据
     */
    public void addHistory(PointsAwardLeaderboardData rankingData) {
        try {
            String lockKey = PointsAwardConstant.RedisLockKey.POINTS_AWARD_RANKING_HISTORY_LOCK + rankingData.getRankType();
            redisLock.lockAndRun(lockKey, PointsAwardConstant.Leaderboard.LOCK_LEASE_MILLIS, () -> {
                String historyKey = PointsAwardConstant.RedisKey.POINTS_AWARD_RANKING_HISTORY + rankingData.getRankType();
                RDeque<PointsAwardLeaderboardData> deque = redissonClient.getDeque(historyKey);

                deque.addFirst(rankingData);

                // 保持历史记录数量限制
                while (deque.size() > PointsAwardConstant.Leaderboard.MAX_HISTORY_SIZE) {
                    deque.removeLast();
                }
            });

            // 记录排行榜历史日志
            pointsAwardLogger.addLeaderboardHistory(rankingData);

            log.debug("排行榜历史记录添加完成，类型: {}", rankingData.getRankType());
        } catch (Exception e) {
            log.error("添加排行榜历史记录失败，类型: {}", rankingData.getRankType(), e);
        }
    }

    /**
     * 获取排行榜历史记录
     *
     * @param type 排行榜类型
     * @return 历史记录列表
     */
    public List<PointsAwardLeaderboardData> getRankingHistory(int type) {
        try {
            String historyKey = PointsAwardConstant.RedisKey.POINTS_AWARD_RANKING_HISTORY + type;
            RDeque<PointsAwardLeaderboardData> deque = redissonClient.getDeque(historyKey);
            return deque.readAll();
        } catch (Exception e) {
            log.error("获取排行榜历史记录失败，类型: {}", type, e);
            return new ArrayList<>();
        }
    }

    /**
     * 检查是否是主节点
     *
     * @return true如果不是主节点
     */
    private boolean isMaster() {
        try {
            return marsCurator.isMaster();
        } catch (Exception e) {
            log.warn("检查主节点状态失败，默认为非主节点", e);
            return false;
        }
    }

    /**
     * 检查是否不是主节点
     *
     * @return true如果不是主节点
     */
    private boolean isNotMaster() {
        try {
            return !marsCurator.isMaster();
        } catch (Exception e) {
            log.warn("检查主节点状态失败，默认为非主节点", e);
            return true;
        }
    }

    /**
     * 检查是否是月初第一天
     *
     * @return true如果是月初第一天
     */
    private boolean isFirstDayOfMonth() {
        return LocalDate.now().getDayOfMonth() == 1;
    }

    /**
     * 获取当前时间毫秒数
     *
     * @return 当前时间毫秒数
     */
    private long getCurrentTimeMillis() {
        return System.currentTimeMillis();
    }

    /**
     * 构建开始时间Redis键
     *
     * @param type 排行榜类型
     * @return Redis键
     */
    private String buildStartTimeKey(int type) {
        return PointsAwardConstant.RedisKey.POINTS_AWARD_RANKING_START_TS + type;
    }

    /**
     * 获取排行榜名称
     *
     * @param type 排行榜类型
     * @return 排行榜名称
     */
    private String getRankName(int type) {
        return switch (type) {
            case PointsAwardConstant.Leaderboard.AM -> PointsAwardConstant.Leaderboard.RANK_NAME_AM;
            case PointsAwardConstant.Leaderboard.PM -> PointsAwardConstant.Leaderboard.RANK_NAME_PM;
            default -> StringUtils.EMPTY;
        };
    }
}
