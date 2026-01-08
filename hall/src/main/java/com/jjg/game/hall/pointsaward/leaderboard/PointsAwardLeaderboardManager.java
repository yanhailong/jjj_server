package com.jjg.game.hall.pointsaward.leaderboard;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.RandomUtil;
import com.alibaba.fastjson.JSON;
import com.jjg.game.common.concurrent.BaseHandler;
import com.jjg.game.common.concurrent.PlayerExecutorGroupDisruptor;
import com.jjg.game.common.curator.MarsCurator;
import com.jjg.game.common.listener.IGameClusterLeaderListener;
import com.jjg.game.common.proto.Pair;
import com.jjg.game.common.redis.RedisLock;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.common.utils.WheelTimerUtil;
import com.jjg.game.core.constant.AwardCodeType;
import com.jjg.game.core.data.*;
import com.jjg.game.core.listener.ConfigExcelChangeListener;
import com.jjg.game.core.listener.GmListener;
import com.jjg.game.core.manager.AwardCodeManager;
import com.jjg.game.core.service.MailService;
import com.jjg.game.core.service.RankService;
import com.jjg.game.core.utils.ItemUtils;
import com.jjg.game.core.utils.RobotUtil;
import com.jjg.game.hall.pointsaward.PointsAwardLogger;
import com.jjg.game.hall.pointsaward.constant.PointsAwardConstant;
import com.jjg.game.hall.pointsaward.pb.PointsAwardLeaderboardData;
import com.jjg.game.hall.pointsaward.pb.PointsAwardLeaderboardInfo;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.GlobalConfigCfg;
import com.jjg.game.sampledata.bean.PointsAwardRankingCfg;
import com.jjg.game.sampledata.bean.PointsAwardRobotCfg;
import com.jjg.game.sampledata.bean.RobotCfg;
import io.netty.util.Timeout;
import org.apache.commons.lang.StringUtils;
import org.apache.curator.framework.recipes.leader.LeaderLatchListener;
import org.redisson.api.RDeque;
import org.redisson.api.RList;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 积分大奖排行榜管理器
 */
@Component
public class PointsAwardLeaderboardManager implements IGameClusterLeaderListener {

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
    private final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    //RobotUserID ->PointsAwardRobotCfg Id
    private List<Pair<Long, Integer>> robotList;
    private final RankService rankService;
    private final RobotUtil robotUtil;
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
            PointsAwardLogger pointsAwardLogger, RankService rankService, RobotUtil robotUtil) {
        this.leaderboardService = leaderboardService;
        this.redisLock = redisLock;
        this.marsCurator = marsCurator;
        this.redissonClient = redissonClient;
        this.mailService = mailService;
        this.awardCodeManager = awardCodeManager;
        this.pointsAwardLogger = pointsAwardLogger;
        this.rankService = rankService;
        this.robotUtil = robotUtil;
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
            cacheRankData();
            addRobotSchedule();
            log.info("积分奖励排行榜管理器初始化完成");
        } catch (Exception e) {
            log.error("积分奖励排行榜管理器初始化失败", e);
            throw new RuntimeException("排行榜管理器初始化失败", e);
        }
    }

    /**
     * 添加机器人定时任务
     */
    private void addRobotSchedule() {
        if (!isMaster()) {
            return;
        }
        try {
            if (GameDataManager.getInstance().hasCfgContainer(GlobalConfigCfg.class)) {
                //单位秒【倒计时下限_倒计时上限_最小增长积分_前X名用机器人占榜】
                GlobalConfigCfg globalConfigCfg = GameDataManager.getGlobalConfigCfg(48);
                if (globalConfigCfg != null && StringUtils.isNotEmpty(globalConfigCfg.getValue())) {
                    String[] config = StringUtils.split(globalConfigCfg.getValue(), "_");
                    if (config.length != 4) {
                        return;
                    }
                    WheelTimerUtil.schedule(this::robotAction, RandomUtil.randomInt(Integer.parseInt(config[0]), Integer.parseInt(config[1])), TimeUnit.SECONDS);
                }
            }
        } catch (Exception e) {
            log.error("添加积分大奖机器人定时任务失败", e);
        }
    }

    /**
     * 机器人行为
     */
    public void robotAction() {
        PlayerExecutorGroupDisruptor.getDefaultExecutor().tryPublish(0, 0, new BaseHandler<String>() {
            @Override
            public void action() {
                try {
                    //单位秒【倒计时下限_倒计时上限_最小增长积分_前X名用机器人占榜】
                    GlobalConfigCfg globalConfigCfg = GameDataManager.getGlobalConfigCfg(48);
                    if (globalConfigCfg != null && StringUtils.isNotEmpty(globalConfigCfg.getValue())) {
                        String[] config = StringUtils.split(globalConfigCfg.getValue(), "_");
                        if (config.length != 4) {
                            return;
                        }
                        //a)	机器人积分 = 当前排行榜对应位置的积分 + 最小增长积分 × 排行榜最大名次（300） × 随机数（0~1） × Max { 1，（当前排名 × （当前排名 - 1））}  + 最小增长积分  × 排行榜最大名次（300） ÷ 当前排名
                        //i.	当前排行榜对应位置的积分
                        //ii.	最小增长积分，globalConfig 表中 ID 48 ，最后一个参数
                        //iii.	 随机数（0~1），执行时在0~1的范围内随机一个值
                        //iv.	当前排名，机器人对标的占位排名位置
                        //v.	排行榜最大名次（300），配置中的最大名次，当前300
                        int minAdd = Integer.parseInt(config[2]);
                        int maxRank = Integer.parseInt(config[3]);
                        //缓存中没有从redis中取
                        if (CollectionUtil.isEmpty(robotList)) {
                            robotList = new ArrayList<>(maxRank);
                            //取机器人配置的id，积分大奖机器人配置的数据生成玩家id和数据保存到redis确保每次执行都一样
                            //list String 积分_机器人
                            RList<String> redisIdList = getRedisRobotList();
                            if (redisIdList.isEmpty()) {
                                //redis中没有就初始化
                                List<PointsAwardRobotCfg> pointsRobot = new ArrayList<>(GameDataManager.getPointsAwardRobotCfgList());
                                List<RobotCfg> robotCfgList = new ArrayList<>(GameDataManager.getRobotCfgList());
                                //打乱
                                Collections.shuffle(robotCfgList);
                                Collections.shuffle(pointsRobot);
                                maxRank = Math.min(maxRank, robotCfgList.size());
                                maxRank = Math.min(maxRank, pointsRobot.size());
                                List<String> add = new ArrayList<>(maxRank);
                                for (int i = 0; i < maxRank; i++) {
                                    RobotCfg robotCfg = robotCfgList.get(i);
                                    long robotId = robotUtil.getId(robotCfg.getId());
                                    PointsAwardRobotCfg pointsRobotCfg = pointsRobot.get(i);
                                    add.add(robotId + "_" + pointsRobotCfg.getId());
                                    robotList.add(Pair.newPair(robotId, pointsRobotCfg.getId()));
                                }
                                redisIdList.addAll(add);
                                log.info("积分大奖初始化机器人数据成功");
                            } else {
                                loadRobotData(redisIdList);
                            }
                        }
                        if (CollectionUtil.isEmpty(robotList)) {
                            return;
                        }
                        //取出对应的排名
                        String rankKey = leaderboardService.getRankKey(PointsAwardConstant.Leaderboard.DAY);
                        List<RankEntry> rankEntries = rankService.topN(rankKey, robotList.size());
                        List<RankChange> rankChanges = new ArrayList<>(robotList.size());
                        for (int i = 0; i < robotList.size(); i++) {
                            Pair<Long, Integer> robotCfgPair = robotList.get(i);
                            PointsAwardRobotCfg rankingCfg = GameDataManager.getPointsAwardRobotCfg(robotCfgPair.getSecond());
                            if (rankingCfg == null) {
                                continue;
                            }
                            long oldPoint = 0;
                            if (i < rankEntries.size()) {
                                oldPoint = rankEntries.get(i).getPoints();
                            }
                            int currentRank = i + 1;
                            //a)	机器人积分 = 当前排行榜对应位置的积分 + (最小增长积分 × 排行榜最大名次（300） × 随机数（0~1） / Max { 1，（当前排名 × （当前排名 - 1））}  + 最小增长积分  × 排行榜最大名次（300） ÷ 当前排名)/100
                            double newPoint = oldPoint + ((minAdd * maxRank * RandomUtil.randomDouble(0, 1) / Math.max(1, currentRank * (currentRank - 1)) + (double) minAdd * maxRank / currentRank) / 100);
                            rankChanges.add(new RankChange(robotCfgPair.getFirst(), (int) newPoint));
                        }
                        if (rankChanges.isEmpty()) {
                            return;
                        }
                        //批量获取原积分,然后计算真正的积分
                        Map<Long, Long> oldPointMap = rankService.batchGetPoints(rankKey, rankChanges.stream().map(RankChange::getPlayerId).collect(Collectors.toList()));
                        //从新计算需要增加的分数
                        for (RankChange rankChange : rankChanges) {
                            rankChange.setAddPoints(rankChange.getAddPoints() - (oldPointMap.getOrDefault(rankChange.getPlayerId(), 0L)).intValue());
                        }

                        log.info("变化积分 ：{}", JSON.toJSONString(rankChanges));
                        //更新排行榜
                        rankService.batchAddPoints(rankKey, rankChanges);
                        log.info("积分大奖更新日榜成功 size:{}", rankChanges.size());
                        rankService.batchAddPoints(leaderboardService.getRankKey(PointsAwardConstant.Leaderboard.WEEK), rankChanges);
                        log.info("积分大奖更新周榜成功size:{}", rankChanges.size());
                        rankService.batchAddPoints(leaderboardService.getRankKey(PointsAwardConstant.Leaderboard.TYPE_MONTH), rankChanges);
                        log.info("积分大奖更新月榜成功size:{}", rankChanges.size());
                    }
                } catch (Exception e) {
                    log.error("积分大奖机器人逻辑处理失败");
                } finally {
                    addRobotSchedule();
                }
            }
        }.setHandlerParamWithSelf("pointsAward robotAction"));
    }

    public RList<String> getRedisRobotList() {
        return redissonClient.getList(PointsAwardConstant.RedisKey.POINTS_AWARD_ROBOT_ID);
    }

    public Map<Long, Integer> getRobotMap() {
        if (robotList == null) {
            loadRobotData(getRedisRobotList());
        }
        if (CollectionUtil.isEmpty(robotList)) {
            return Map.of();
        }
        return robotList.stream().collect(Collectors.toMap(Pair::getFirst, Pair::getSecond));
    }

    public boolean isRobot(long playerId) {
        return robotUtil.isRobot(playerId);
    }

    /**
     * 从redis加载机器人列表
     *
     * @param redisIdList
     */
    private void loadRobotData(RList<String> redisIdList) {
        if (CollectionUtil.isEmpty(robotList)) {
            robotList = new ArrayList<>();
            //从redis解析
            List<String> cfgCache = redisIdList.readAll();
            for (String cfg : cfgCache) {
                String[] configStr = cfg.split("_");
                robotList.add(Pair.newPair(Long.parseLong(configStr[0]), Integer.parseInt(configStr[1])));
            }
            log.info("积分大奖缓存机器人数据成功");
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
                    PointsAwardConstant.Leaderboard.DAY,
                    PointsAwardConstant.Leaderboard.WEEK
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
     * 缓存排行榜数据
     */
    public void cacheRankData() {
        if (configMap.containsKey(PointsAwardConstant.Leaderboard.DAY)) {
            leaderboardService.loadRank(PointsAwardConstant.Leaderboard.DAY);
        }
        if (configMap.containsKey(PointsAwardConstant.Leaderboard.WEEK)) {
            leaderboardService.loadRank(PointsAwardConstant.Leaderboard.WEEK);
        }
        if (configMap.containsKey(PointsAwardConstant.Leaderboard.TYPE_MONTH)) {
            leaderboardService.loadRank(PointsAwardConstant.Leaderboard.TYPE_MONTH);
        }
        log.debug("缓存排行榜数据完成");
    }

    // ==================== 定时任务方法 ====================

    /**
     * 根据指定的小时数执行相应的业务逻辑。
     * 0 点：保存每日，每周榜（全天最终）快照并清空日榜；若为每月第一天，保存上月榜并清空月榜
     */
    public void clock(int hour) {
        try {
            if (isMaster()) {
                handleMidnightSettlement();
            }
            if (isFirstDayOfMonth()) {
                clearRobotData();
            }
            // 重载配置
            reloadConfig();
        } catch (Exception e) {
            log.error("定时任务执行失败，小时: {}", hour, e);
        }
    }


    private boolean isWeekStart() {
        return LocalDate.now().getDayOfWeek() == DayOfWeek.MONDAY;
    }

    /**
     * 处理午夜0点的结算逻辑
     */
    private void handleMidnightSettlement() {
        try {
            //日榜
            if (configMap.containsKey(PointsAwardConstant.Leaderboard.DAY)) {
                snapshotUnderLock(PointsAwardConstant.Leaderboard.DAY);
                leaderboardService.reset(PointsAwardConstant.Leaderboard.DAY);
            }
            // 周榜
            boolean weekStart = isWeekStart();
            if (weekStart && configMap.containsKey(PointsAwardConstant.Leaderboard.WEEK)) {
                snapshotUnderLock(PointsAwardConstant.Leaderboard.WEEK);
                leaderboardService.reset(PointsAwardConstant.Leaderboard.WEEK);
            }
            // 每月第一天的 0 点，保存上月榜并清空月榜
            boolean firstDayOfMonth = isFirstDayOfMonth();
            if (firstDayOfMonth && configMap.containsKey(PointsAwardConstant.Leaderboard.TYPE_MONTH)) {
                snapshotUnderLock(PointsAwardConstant.Leaderboard.TYPE_MONTH);
                leaderboardService.reset(PointsAwardConstant.Leaderboard.TYPE_MONTH);
            }
            log.info("午夜0点结算完成，是否周一:{} 是否月初: {}", weekStart, firstDayOfMonth);
        } catch (Exception e) {
            log.error("午夜0点结算失败", e);
        }
    }

    /**
     * 清除机器人数据
     */
    private void clearRobotData() {
        if (isMaster()) {
            RList<String> redisIdList = redissonClient.getList(PointsAwardConstant.RedisKey.POINTS_AWARD_ROBOT_ID);
            redisIdList.delete();
        }
        robotList = null;
        log.info("月榜结束 清除机器人数据成功");
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
            case PointsAwardConstant.Leaderboard.DAY, PointsAwardConstant.Leaderboard.WEEK ->
                    !(configDate.isBefore(now.atStartOfDay()));
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
     * 在对应排行榜锁下读取 TopN 并持久化快照
     *
     * @param snapshotType 快照类型
     */
    private void snapshotUnderLock(int snapshotType) {
        int maxRankSize = getMaxRankSize(snapshotType);
        List<PointsAwardLeaderboardInfo> topList = leaderboardService.topN(snapshotType, maxRankSize);
        PointsAwardLeaderboardData rankingData = new PointsAwardLeaderboardData();
        rankingData.setRankType(snapshotType);
        rankingData.setEndTime(TimeHelper.getCurrentDateZeroMilliTime());
        rankingData.setRankingInfoList(topList);
        // 发奖
        sendAward(rankingData);
        // 记录排行榜历史记录
        addHistory(rankingData);
    }

    /**
     * 获取排行榜结束时间
     *
     * @param rankType 排行榜类型
     * @return 结束的时间戳
     */
    public long getEndTime(int rankType) {
        switch (rankType) {
            case PointsAwardConstant.Leaderboard.DAY -> {
                return TimeHelper.getTimestamp(LocalDate.now().plusDays(1).atStartOfDay());
            }
            case PointsAwardConstant.Leaderboard.WEEK -> {
                return TimeHelper.getNextWeekdayEnd(DayOfWeek.MONDAY);
            }
            case PointsAwardConstant.Leaderboard.TYPE_MONTH -> {
                return TimeHelper.getTimestamp(LocalDate.now().plusMonths(1).withDayOfMonth(1).atStartOfDay());
            }
        }
        return System.currentTimeMillis();
    }


    /**
     * 获取排行榜的大小
     *
     * @param type 排行榜类型
     * @return 排行榜大小
     */
    public int getMaxRankSize(int type) {
        GlobalConfigCfg globalConfigCfg = GameDataManager.getGlobalConfigCfg(49);
        if (globalConfigCfg != null && StringUtils.isNotEmpty(globalConfigCfg.getValue())) {
            String[] split = StringUtils.split(globalConfigCfg.getValue());
            if (split.length != 3) {
                return PointsAwardConstant.Leaderboard.MAX_RANK_SIZE;
            }
            if (type > split.length) {
                return PointsAwardConstant.Leaderboard.MAX_RANK_SIZE;
            }
            return Integer.parseInt(split[type - 1]);
        }
        return PointsAwardConstant.Leaderboard.MAX_RANK_SIZE;
    }

    public LocalDate getStartTime(int rankType) {
        switch (rankType) {
            case PointsAwardConstant.Leaderboard.DAY -> {
                return LocalDate.now().minusDays(1);
            }
            case PointsAwardConstant.Leaderboard.WEEK -> {
                return LocalDate.now().minusWeeks(1);
            }
            case PointsAwardConstant.Leaderboard.TYPE_MONTH -> {
                return LocalDate.now().minusMonths(1);
            }
        }
        return LocalDate.now();
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
        //排行开始时间
        LocalDate rankDate = getStartTime(rankingData.getRankType());
        //当前排行榜结算的配置
        Map<Integer, PointsAwardRankingCfg> cfgMap = filterConfig(rankingData.getRankType(), GameDataManager.getPointsAwardRankingCfgList(), rankDate);
        if (cfgMap.isEmpty()) {
            log.warn("排行榜配置为空，跳过发奖，类型: {}", rankingData.getRankType());
            return;
        }
        for (PointsAwardLeaderboardInfo leaderboardInfo : rankingInfoList) {
            if (isRobot(leaderboardInfo.getPlayerId())) {
                log.debug("玩家为机器人，跳过发奖，玩家ID: {}, 排名: {}", leaderboardInfo.getPlayerId(), leaderboardInfo.getRank());
                continue;
            }
            try {
                sendPlayerAward(leaderboardInfo, cfgMap, rankingData);
            } catch (Exception e) {
                log.error("发送玩家奖励失败，玩家ID: {}, 排名: {}", leaderboardInfo.getPlayerId(), leaderboardInfo.getRank(), e);
            }
        }
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
        processPlayerAward(info, cfg, rankingData);
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

        paramData.add(new LanguageParamData(0, FORMATTER.format(date)));

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
        } else if (rankType == PointsAwardConstant.Leaderboard.DAY) {
            return awardType == PointsAwardConstant.Leaderboard.AwardType.ITEM
                    ? PointsAwardConstant.Leaderboard.MailTemplateId.DAILY_ITEM_AWARD
                    : PointsAwardConstant.Leaderboard.MailTemplateId.DAILY_OTHER_AWARD;
        } else if (rankType == PointsAwardConstant.Leaderboard.WEEK) {
            return awardType == PointsAwardConstant.Leaderboard.AwardType.ITEM
                    ? PointsAwardConstant.Leaderboard.MailTemplateId.WEEK_ITEM_AWARD
                    : PointsAwardConstant.Leaderboard.MailTemplateId.WEEK_OTHER_AWARD;
        }
        return PointsAwardConstant.Leaderboard.MailTemplateId.DAILY_ITEM_AWARD;
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
     * 检查是否是月初第一天
     *
     * @return true如果是月初第一天
     */
    private boolean isFirstDayOfMonth() {
        return LocalDate.now().getDayOfMonth() == 1;
    }


    @Override
    public void isLeader() {
        addRobotSchedule();
        log.info("成为主节点 添加机器人定时任务");
    }

    @Override
    public void notLeader() {
    }
}
