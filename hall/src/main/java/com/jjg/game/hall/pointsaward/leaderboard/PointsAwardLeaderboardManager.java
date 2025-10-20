package com.jjg.game.hall.pointsaward.leaderboard;

import com.jjg.game.common.curator.MarsCurator;
import com.jjg.game.common.redis.RedisLock;
import com.jjg.game.core.constant.AwardCodeType;
import com.jjg.game.core.data.LanguageParamData;
import com.jjg.game.core.manager.AwardCodeManager;
import com.jjg.game.core.service.MailService;
import com.jjg.game.core.utils.ItemUtils;
import com.jjg.game.hall.pointsaward.constant.PointsAwardConstant;
import com.jjg.game.hall.pointsaward.pb.PointsAwardLeaderboardData;
import com.jjg.game.hall.pointsaward.pb.PointsAwardLeaderboardInfo;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.PointsAwardRankingCfg;
import org.redisson.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private final Map<Integer, Map<Integer, PointsAwardRankingCfg>> configMap = new HashMap<>();

    private final PointsAwardLeaderboardService leaderboardService;
    private final RedisLock redisLock;
    private final MarsCurator marsCurator;
    private final RedissonClient redissonClient;
    private final MailService mailService;
    private final AwardCodeManager awardCodeManager;

    public PointsAwardLeaderboardManager(PointsAwardLeaderboardService leaderboardService,
                                         RedisLock redisLock,
                                         RedissonClient redissonClient,
                                         MailService mailService,
                                         AwardCodeManager awardCodeManager,
                                         MarsCurator marsCurator) {
        this.leaderboardService = leaderboardService;
        this.redisLock = redisLock;
        this.redissonClient = redissonClient;
        this.mailService = mailService;
        this.awardCodeManager = awardCodeManager;
        this.marsCurator = marsCurator;
    }

    /**
     * 初始化
     */
    public void init() {
        initConfig();
        // 初始化服务的管理器
        leaderboardService.init(this);
        // 初始化并确保各排行榜类型的周期与结算逻辑
        initLeaderboards();
    }

    /**
     * 根据指定的小时数执行相应的业务逻辑。
     * 12 点：保存上午榜快照
     * 0 点：保存下午榜（全天最终）快照并清空日榜；若为每月第一天，保存上月榜并清空月榜
     */
    public void clock(int hour) {
        // 主节点优先执行，非主节点直接返回
        if (isMaster()) {
            return;
        }
        if (hour == 12) {
            // 12 点：结算上午榜并开启下午榜
            snapshotUnderLock(PointsAwardConstant.Leaderboard.AM, PointsAwardConstant.Leaderboard.AM, true);
            leaderboardService.reset(PointsAwardConstant.Leaderboard.AM);
            long now = nowMillis();
            // 开启下午榜周期并确保为空
            redissonClient.getBucket(startKey(PointsAwardConstant.Leaderboard.PM)).set(now);
            leaderboardService.reset(PointsAwardConstant.Leaderboard.PM);
        } else if (hour == 0) {
            // 0 点：结算下午榜并开启新一天上午榜
            snapshotUnderLock(PointsAwardConstant.Leaderboard.PM, PointsAwardConstant.Leaderboard.PM, true);
            leaderboardService.reset(PointsAwardConstant.Leaderboard.PM);
            long now = nowMillis();
            redissonClient.getBucket(startKey(PointsAwardConstant.Leaderboard.AM)).set(now);
            leaderboardService.reset(PointsAwardConstant.Leaderboard.AM);
            // 每月第一天的 0 点，保存上月榜并清空月榜
            if (LocalDate.now().getDayOfMonth() == 1) {
                snapshotUnderLock(PointsAwardConstant.Leaderboard.TYPE_MONTH, PointsAwardConstant.Leaderboard.TYPE_MONTH, true);
                leaderboardService.reset(PointsAwardConstant.Leaderboard.TYPE_MONTH);
                redissonClient.getBucket(startKey(PointsAwardConstant.Leaderboard.TYPE_MONTH)).set(now);
            }
        }
    }

    private boolean isMaster() {
        try {
            return !marsCurator.isMaster();
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * 生成排行榜名字
     *
     * @param rankType 类型
     * @param endTime  结束时间
     */
    public String buildName(int rankType, long endTime) {
        String name = null;
        LocalDate date = LocalDate.ofInstant(Instant.ofEpochMilli(endTime), ZoneId.systemDefault());
        if (rankType == PointsAwardConstant.Leaderboard.AM) {
            name = date + PointsAwardConstant.Leaderboard.NAME + PointsAwardConstant.Leaderboard.RANK_NAME_AM;
        } else if (rankType == PointsAwardConstant.Leaderboard.PM) {
            name = date + PointsAwardConstant.Leaderboard.NAME + PointsAwardConstant.Leaderboard.RANK_NAME_PM;
        } else if (rankType == PointsAwardConstant.Leaderboard.TYPE_MONTH) {
            // 本月第一天
            LocalDate firstDay = date.withDayOfMonth(1);
            // 本月最后一天
            LocalDate lastDay = date.withDayOfMonth(date.lengthOfMonth());
            name = firstDay + PointsAwardConstant.Leaderboard.NAME + lastDay;
        }
        return name;
    }

    /**
     * 在对应排行榜锁下读取 TopN 并持久化快照
     */
    private void snapshotUnderLock(int zsetType, int snapshotType, boolean lock) {
        Supplier<PointsAwardLeaderboardData> supplier = () -> {
            List<PointsAwardLeaderboardInfo> topList = leaderboardService.topN(zsetType, PointsAwardConstant.Leaderboard.MAX_RANK_SIZE);
            PointsAwardLeaderboardData data = new PointsAwardLeaderboardData();
            data.setRankType(snapshotType);
            data.setEndTime(System.currentTimeMillis());
            data.setRankingInfoList(topList);
            data.setName(buildName(snapshotType, data.getEndTime()));
            return data;
        };
        PointsAwardLeaderboardData rankingData;
        if (lock) {
            String lockKey = PointsAwardConstant.RedisLockKey.POINTS_AWARD_RANKING_LOCK + zsetType;
            rankingData = redisLock.lockAndGet(lockKey, PointsAwardConstant.Leaderboard.LOCK_LEASE_MILLIS, supplier);
        } else {
            rankingData = supplier.get();
        }
        //发奖
        sendAward(rankingData);
        //记录排行榜历史记录
        addHistory(rankingData);
    }

    /**
     * 初始化所有配置
     */
    public void initConfig() {
        configMap.clear();
        List<PointsAwardRankingCfg> pointsAwardRankingCfgList = GameDataManager.getPointsAwardRankingCfgList();
        //初始化所有排行榜数据
        if (pointsAwardRankingCfgList != null) {
            pointsAwardRankingCfgList.forEach(cfg -> {
                List<Integer> ranking = cfg.getRanking();
                if (ranking != null) {
                    int size = ranking.size();
                    int min = ranking.getFirst();
                    if (size >= 2) {
                        int max = ranking.getLast();
                        for (int i = min; i <= max; i++) {
                            configMap.computeIfAbsent(cfg.getType(), k -> new HashMap<>()).put(i, cfg);
                        }
                    } else {
                        configMap.computeIfAbsent(cfg.getType(), k -> new HashMap<>()).put(min, cfg);
                    }
                }
            });
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
        if (map == null) {
            return 0;
        }
        return map.size();
    }

    /**
     * 发送排行榜奖励
     *
     * @param rankingData 排行信息
     */
    public void sendAward(PointsAwardLeaderboardData rankingData) {
        List<PointsAwardLeaderboardInfo> rankingInfoList = rankingData.getRankingInfoList();
        if (rankingInfoList == null || rankingInfoList.isEmpty()) {
            return;
        }
        Map<Integer, PointsAwardRankingCfg> cfgMap = getRankingCfgMap(rankingData.getRankType());
        rankingInfoList.forEach(info -> {
            int rank = info.getRank();
            PointsAwardRankingCfg cfg = cfgMap.get(rank);
            //发奖
            if (cfg != null) {
                List<String> awardItems = cfg.getGetItem();
                //有奖励才发奖
                if (awardItems != null && !awardItems.isEmpty()) {
                    redisLock.tryLockAndRun(PointsAwardConstant.RedisLockKey.PLAYER_RANKING_AWARD_LOCK + info.getPlayerId(), () -> {
                        String code = null;
                        List<LanguageParamData> paramData = new ArrayList<>();
                        LocalDate date = LocalDate.ofInstant(Instant.ofEpochMilli(rankingData.getEndTime()), ZoneId.systemDefault());
                        if (rankingData.getRankType() == PointsAwardConstant.Leaderboard.AM) {
                            paramData.add(new LanguageParamData(0, date.toString()));
                            paramData.add(new LanguageParamData(0, PointsAwardConstant.Leaderboard.RANK_NAME_AM));
                            paramData.add(new LanguageParamData(0, String.valueOf(info.getRank())));

                        } else if (rankingData.getRankType() == PointsAwardConstant.Leaderboard.PM) {
                            paramData.add(new LanguageParamData(0, date.toString()));
                            paramData.add(new LanguageParamData(0, PointsAwardConstant.Leaderboard.RANK_NAME_PM));
                            paramData.add(new LanguageParamData(0, String.valueOf(info.getRank())));
                        } else if (rankingData.getRankType() == PointsAwardConstant.Leaderboard.TYPE_MONTH) {
                            paramData.add(new LanguageParamData(0, date.toString()));
                            paramData.add(new LanguageParamData(0, String.valueOf(info.getRank())));
                        }
                        //其他奖励
                        if (cfg.getAwardType() == PointsAwardConstant.Leaderboard.AwardType.OTHER) {
                            mailService.addCfgMail(info.getPlayerId(), 5, null, paramData);
                            code = awardCodeManager.generateCode(info.getPlayerId(), AwardCodeType.POINTS_AWARD);
                        }
                        //道具
                        else if (cfg.getAwardType() == PointsAwardConstant.Leaderboard.AwardType.ITEM) {
                            mailService.addCfgMail(info.getPlayerId(), 4, ItemUtils.buildItemsByStrList(awardItems), paramData);
                        }
                        //添加历史记录
                        leaderboardService.addHistory(info, cfg, code, rankingData.getName());
                    });
                }
            }
            log.debug("rank:[{}] playerId: [{}] rank: [{}]", rankingData.getRankType(), info.getPlayerId(), info.getRank());
        });
    }

    /**
     * 记录排行榜起始时间的 Redis Key 前缀（沿用现有前缀风格）
     */
    private static final String RANK_START_TS_KEY_PREFIX = PointsAwardConstant.RedisKey.POINTS_AWARD_RANKING + "start_ts:";

    private String startKey(int type) {
        return RANK_START_TS_KEY_PREFIX + type;
    }

    private long nowMillis() {
        return System.currentTimeMillis();
    }

    /**
     * 初始化并根据当前时间进行结算检查：
     * 1) 内存不存在则创建并记录起始时间
     * 2) 已存在则比较记录时间与当前时间，按类型判断是否达到结算条件（跨月/超过12点/超过24点）
     * 3) 对满足条件的排行榜执行结算，并立即开启新一期
     */
    private void initLeaderboards() {
        // 根据当前配置决定需要维护的排行榜类型
        boolean needAM = configMap.containsKey(PointsAwardConstant.Leaderboard.AM);
        boolean needPM = configMap.containsKey(PointsAwardConstant.Leaderboard.PM);
        boolean needMonth = configMap.containsKey(PointsAwardConstant.Leaderboard.TYPE_MONTH);

        if (needAM) {
            initOrSettleType(PointsAwardConstant.Leaderboard.AM);
        }
        if (needPM) {
            initOrSettleType(PointsAwardConstant.Leaderboard.PM);
        }
        if (needMonth) {
            initOrSettleType(PointsAwardConstant.Leaderboard.TYPE_MONTH);
        }
    }

    /**
     * 初始化或结算指定类型排行榜
     */
    private void initOrSettleType(int type) {
        String lockKey = PointsAwardConstant.RedisLockKey.POINTS_AWARD_RANKING_LOCK + type;
        redisLock.lockAndRun(lockKey, PointsAwardConstant.Leaderboard.LOCK_LEASE_MILLIS, () -> {
            RBucket<Long> startBucket = redissonClient.getBucket(startKey(type));
            Long startTs = startBucket.get();
            // 1) 若不存在则创建新排行榜并记录创建时间
            if (startTs == null) {
                long now = nowMillis();
                startBucket.set(now);
                return;
            }
            // 非主节点不执行结算（避免重复结算与重复发奖）
            if (isMaster()) {
                return;
            }
            // 3) 比较记录时间与当前时间，根据类型判断是否需要结算
            long now = nowMillis();
            LocalDate startDate = LocalDate.ofInstant(Instant.ofEpochMilli(startTs), ZoneId.systemDefault());
            LocalDate nowDate = LocalDate.now();
            if (type == PointsAwardConstant.Leaderboard.AM) {
                // 上午榜：当日 12:00 结算并开启下午榜
                long startOfDayMillis = nowDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
                long noonMillis = startOfDayMillis + 12L * 60 * 60 * 1000;
                if (now >= noonMillis && startTs < noonMillis && nowDate.equals(startDate)) {
                    snapshotUnderLock(PointsAwardConstant.Leaderboard.AM, PointsAwardConstant.Leaderboard.AM, false);
                    leaderboardService.reset(PointsAwardConstant.Leaderboard.AM);
                    // 开启下午榜
                    redissonClient.getBucket(startKey(PointsAwardConstant.Leaderboard.PM)).set(now);
                    leaderboardService.reset(PointsAwardConstant.Leaderboard.PM);
                }
            } else if (type == PointsAwardConstant.Leaderboard.PM) {
                // 下午榜：跨天（0 点）结算并开启新一天上午榜
                if (nowDate.isAfter(startDate)) {
                    snapshotUnderLock(PointsAwardConstant.Leaderboard.PM, PointsAwardConstant.Leaderboard.PM, false);
                    leaderboardService.reset(PointsAwardConstant.Leaderboard.PM);
                    long newStartAm = nowDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
                    redissonClient.getBucket(startKey(PointsAwardConstant.Leaderboard.AM)).set(newStartAm);
                    leaderboardService.reset(PointsAwardConstant.Leaderboard.AM);
                }
            } else if (type == PointsAwardConstant.Leaderboard.TYPE_MONTH) {
                // 跨月：结算月榜并开启新一期月榜
                boolean monthChanged = nowDate.getYear() != startDate.getYear()
                        || nowDate.getMonthValue() != startDate.getMonthValue();
                if (monthChanged) {
                    snapshotUnderLock(PointsAwardConstant.Leaderboard.TYPE_MONTH, PointsAwardConstant.Leaderboard.TYPE_MONTH, false);
                    leaderboardService.reset(PointsAwardConstant.Leaderboard.TYPE_MONTH);
                    startBucket.set(now);
                }
            }
        });
    }

    /**
     * 添加排行榜历史记录
     *
     * @param data 排行数据
     */
    public void addHistory(PointsAwardLeaderboardData data) {
        RMap<Integer, RDeque<PointsAwardLeaderboardData>> rankingHistoryMap = redissonClient.getMap(PointsAwardConstant.RedisKey.POINTS_AWARD_RANKING_HISTORY + data.getRankType());
        RLock rLock = rankingHistoryMap.getLock(data.getRankType());
        boolean tryLock = rLock.tryLock();
        try {
            if (!tryLock) {
                return;
            }
            RDeque<PointsAwardLeaderboardData> rankingHistoryDeque = rankingHistoryMap.get(data.getRankType());
            if (rankingHistoryDeque != null) {
                rankingHistoryDeque.addFirst(data);
                if (rankingHistoryDeque.size() > PointsAwardConstant.Leaderboard.MAX_HISTORY_SIZE) {
                    rankingHistoryDeque.removeLast();
                }
            }
        } finally {
            rLock.unlock();
        }
    }

    /**
     * 获取排行榜所有历史记录
     *
     * @param rankType 排行榜类型
     */
    public List<PointsAwardLeaderboardData> getRankingHistory(int rankType) {
        RMap<Integer, RDeque<PointsAwardLeaderboardData>> rankingHistoryMap = redissonClient.getMap(PointsAwardConstant.RedisKey.POINTS_AWARD_RANKING_HISTORY + rankType);
        if (!rankingHistoryMap.containsKey(rankType)) {
            return new ArrayList<>();
        }
        RDeque<PointsAwardLeaderboardData> rankingHistory = rankingHistoryMap.get(rankType);
        return rankingHistory.readAll();
    }

}
