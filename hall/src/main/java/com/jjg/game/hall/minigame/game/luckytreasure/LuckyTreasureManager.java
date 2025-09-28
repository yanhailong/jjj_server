package com.jjg.game.hall.minigame.game.luckytreasure;

import com.jjg.game.common.curator.MarsCurator;
import com.jjg.game.common.listener.IGameClusterLeaderListener;
import com.jjg.game.common.redis.RedisLock;
import com.jjg.game.common.timer.TimerCenter;
import com.jjg.game.common.timer.TimerEvent;
import com.jjg.game.common.timer.TimerListener;
import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.core.config.ConfigManager;
import com.jjg.game.core.config.bean.LuckyTreasureConfig;
import com.jjg.game.core.constant.LuckyTreasureConstant;
import com.jjg.game.core.dao.luckytreasure.LuckyTreasureDao;
import com.jjg.game.core.dao.luckytreasure.LuckyTreasureRedisDao;
import com.jjg.game.core.data.LuckyTreasure;
import com.jjg.game.hall.minigame.MinigameManager;
import com.jjg.game.hall.minigame.event.MinigameReadyEvent;
import com.jjg.game.hall.minigame.game.luckytreasure.bean.LuckyTreasureTimerEvent;
import com.jjg.game.hall.minigame.game.luckytreasure.service.LuckyTreasureService;
import com.jjg.game.hall.minigame.game.luckytreasure.util.RewardCodeGenerator;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.GlobalConfigCfg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 夺宝奇兵管理器
 */
@Component
public class LuckyTreasureManager implements IGameClusterLeaderListener, TimerListener<LuckyTreasureTimerEvent> {
    private static final Logger log = LoggerFactory.getLogger(LuckyTreasureManager.class);

    private final LuckyTreasureDao luckyTreasureDao;
    private final LuckyTreasureRedisDao luckyTreasureRedisDao;
    private final RedisLock redisLock;
    private final MarsCurator marsCurator;
    private final TimerCenter timerCenter;
    private final RewardCodeGenerator rewardCodeGenerator;
    private final LuckyTreasureService luckyTreasureService;
    private final MinigameManager minigameManager;
    private final ConfigManager configManager;

    /**
     * 活动定时器映射：期号 -> 定时器事件
     */
    private final Map<Long, TimerEvent<LuckyTreasureTimerEvent>> activityTimers = new ConcurrentHashMap<>();

    public LuckyTreasureManager(LuckyTreasureDao luckyTreasureDao,
                                LuckyTreasureRedisDao luckyTreasureRedisDao,
                                ConfigManager configManager,
                                RedisLock redisLock,
                                MarsCurator marsCurator,
                                TimerCenter timerCenter,
                                LuckyTreasureService luckyTreasureService,
                                MinigameManager minigameManager,
                                RewardCodeGenerator rewardCodeGenerator) {
        this.luckyTreasureDao = luckyTreasureDao;
        this.luckyTreasureRedisDao = luckyTreasureRedisDao;
        this.redisLock = redisLock;
        this.marsCurator = marsCurator;
        this.timerCenter = timerCenter;
        this.rewardCodeGenerator = rewardCodeGenerator;
        this.luckyTreasureService = luckyTreasureService;
        this.minigameManager = minigameManager;
        this.configManager = configManager;
    }

    /**
     * 初始化
     */
    @EventListener(MinigameReadyEvent.class)
    public void init(MinigameReadyEvent event) {
        if (event.getGameId() == LuckyTreasureConstant.Common.GAME_ID) {
            redisLock.tryLockAndRun(LuckyTreasureConstant.RedisLock.LUCKY_TREASURE_INIT, () -> {
                //检测服务器重启后是否有未处理数据
                recoverUnsettledRounds();
                //检查并启动缺失的活动
                startMissingActivities();
            });
            //初始化服务
            luckyTreasureService.init();
        }
        //监听配置文件变化 如果有新增的夺宝奇兵配置则直接开始
        configManager.addUpdateConfigListener(LuckyTreasureConfig.class, (a, b, c) -> {
            log.info("夺宝奇兵配置更新!检测是否需要新增!id={}",c.getId());
            startNewActivityForConfig(c);
        });
    }

    /**
     * 成为主节点时触发
     */
    @Override
    public void isLeader() {
        log.info("夺宝奇兵管理器成为主节点，开始启动活动管理");
        // 启动新活动（只有主节点才执行）
        startNewActivitiesIfNeeded();
    }

    /**
     * 失去主节点身份时触发
     */
    @Override
    public void notLeader() {
        log.info("夺宝奇兵管理器失去主节点身份");
    }

    /**
     * 定时器事件回调
     *
     * @param timerEvent 定时器事件
     */
    @Override
    public void onTimer(TimerEvent<LuckyTreasureTimerEvent> timerEvent) {
        LuckyTreasureTimerEvent event = timerEvent.getParameter();
        if (event == null) {
            return;
        }

        long issueNumber = event.issueNumber();
        LuckyTreasureTimerEvent.TimerType timerType = event.timerType();

        try {
            // 只有主节点才处理定时器事件
            if (!isCurrentNodeMaster()) {
                return;
            }

            if (timerType == LuckyTreasureTimerEvent.TimerType.ACTIVITY_END) {
                handleActivityEndTimer(issueNumber);
            }

        } catch (Exception e) {
            log.error("处理夺宝奇兵定时器事件失败，期号: {}, 类型: {}", issueNumber, timerType, e);
        }
    }

    /**
     * 查看是否有关服前未处理数据
     */
    public void recoverUnsettledRounds() {
        GlobalConfigCfg globalConfigCfg = GameDataManager.getGlobalConfigCfg(LuckyTreasureConstant.Common.LUCKY_TREASURE_GLOBAL_CONFIG_ID);
        int intValue = globalConfigCfg.getIntValue();
        //查询数据库中是否存在上次未开奖的
        List<LuckyTreasure> luckyTreasures = luckyTreasureDao.findAllByEndTime(0, intValue);
        for (LuckyTreasure luckyTreasure : luckyTreasures) {
            long now = System.currentTimeMillis();
            long endTime = calculateEndTimeMillis(luckyTreasure);
            if (isExpired(endTime, now)) {
                // 已过期，立即处理
                handleActivityEndTimer(luckyTreasure.getIssueNumber());
            } else {
                // 未过期，添加定时器
                addActivityEndTimer(luckyTreasure);
            }
        }
    }


    /**
     * 启动新活动如果需要的话
     */
    public void startNewActivitiesIfNeeded() {
        List<LuckyTreasureConfig> configs = configManager.getConfigs(LuckyTreasureConfig.class);
        for (LuckyTreasureConfig config : configs) {
            if (isOpen() && config.isRepeated() && !luckyTreasureRedisDao.hasActiveRound(config.getId())) {
                startNewActivityForConfig(config);
            }
        }
    }

    /**
     * 检查并启动缺失的活动
     * 确保每个配置都有对应的活跃夺宝奇兵活动
     */
    private void startMissingActivities() {
        try {
            List<LuckyTreasureConfig> configs = configManager.getConfigs(LuckyTreasureConfig.class);
            if (configs.isEmpty()) {
                log.warn("夺宝奇兵配置列表为空，跳过活动检查");
                return;
            }

            int startedCount = 0;
            for (LuckyTreasureConfig config : configs) {
                // 检查该配置是否已有活跃活动
                if (!luckyTreasureRedisDao.hasActiveRound(config.getId())) {
                    log.info("检测到配置 {} 没有活跃活动，准备启动新活动", config.getId());
                    if (isOpen() && config.isRepeated()) {
                        startNewActivityForConfig(config);
                        startedCount++;
                    } else {
                        log.debug("配置 {} 未开启活动!", config.getId());
                    }
                } else {
                    log.debug("配置 {} 已有活跃活动，跳过", config.getId());
                }
            }

            log.info("夺宝奇兵活动检查完成，共启动 {} 个新活动", startedCount);

        } catch (Exception e) {
            log.error("检查并启动缺失活动时发生错误", e);
        }
    }

    /**
     * 为指定配置启动新活动
     */
    private void startNewActivityForConfig(LuckyTreasureConfig config) {
        String lockKey = LuckyTreasureConstant.RedisLock.LUCKY_TREASURE_START + config.getId();
        redisLock.tryLockAndRun(lockKey, () -> {
            // 检查该配置ID是否已有活跃活动
            if (luckyTreasureRedisDao.hasActiveRound(config.getId())) {
                return;
            }
            // 创建新活动
            LuckyTreasure newRound = createNewRound(config);
            // 保存到数据库
            luckyTreasureDao.save(newRound);
            // 保存到Redis
            saveActiveRoundToRedis(newRound);
            // 添加活动结束定时器
            addActivityEndTimer(newRound);

            log.info("启动新的夺宝奇兵活动，配置ID: {}, 期号: {}", config.getId(), newRound.getIssueNumber());
        });
    }

    /**
     * 创建新的活动轮次
     */
    private LuckyTreasure createNewRound(LuckyTreasureConfig config) {
        LuckyTreasure round = new LuckyTreasure();
        // 生成期号
        long issueNumber = generateIssueNumber(config.getId());
        round.setIssueNumber(issueNumber);
        //记录开启时的配置
        round.setConfig(config);
        // 设置时间信息
        long now = System.currentTimeMillis();
        round.setStartTime(now);
        round.setEndTime(0); // 初始为0，表示未结束
        // 初始化购买数据
        round.setBuyMap(new HashMap<>());
        return round;
    }

    /**
     * 生成期号 格式：年月日 + 配置ID(2位) + 5位序号 (如：202509170800001)
     */
    private long generateIssueNumber(int configId) {
        String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long dailyCounter = luckyTreasureRedisDao.generateIssueNumber(configId, today);
        // 组合期号：年月日 + 配置ID(2位) + 序号(5位)
        String issueNumberStr = today + String.format("%02d", configId) + String.format("%05d", dailyCounter);
        return Long.parseLong(issueNumberStr);
    }

    /**
     * 保存活跃活动到Redis
     */
    private void saveActiveRoundToRedis(LuckyTreasure round) {
        // 设置过期时间
        int expireMinutes = round.getConfig().getTime() + round.getConfig().getCollectTime() + 10;
        luckyTreasureRedisDao.saveActiveRound(round, expireMinutes);
    }


    /**
     * 为活动添加结束定时器
     *
     * @param round 活动数据
     */
    private void addActivityEndTimer(LuckyTreasure round) {
        long issueNumber = round.getIssueNumber();

        // 如果已有定时器，先移除
        removeActivityTimer(issueNumber);

        // 计算活动结束时间
        long endTime = calculateEndTimeMillis(round);
        long currentTime = System.currentTimeMillis();

        if (endTime <= currentTime) {
            // 如果已经过期，立即处理
            handleActivityEndTimer(issueNumber);
            return;
        }

        // 创建定时器事件
        LuckyTreasureTimerEvent timerEvent = new LuckyTreasureTimerEvent(
                issueNumber, LuckyTreasureTimerEvent.TimerType.ACTIVITY_END);

        // 计算延迟时间（毫秒）
        long delayMillis = endTime - currentTime;

        log.info("夺宝奇兵{}结束延迟时间millis: {}", issueNumber, delayMillis);
        // 确保延迟时间不为负数
        if (delayMillis <= 0) {
            log.warn("活动 {} 延迟时间计算异常，延迟时间: {}ms，立即处理", issueNumber, delayMillis);
            handleActivityEndTimer(issueNumber);
            return;
        }

        TimerEvent<LuckyTreasureTimerEvent> timer = new TimerEvent<>(
                this,
                timerEvent,
                0, // 不重复执行
                1, // 只执行一次
                (int) delayMillis, // 延迟时间
                false // 相对时间
        );

        // 添加到定时器中心
        timerCenter.add(timer);
        activityTimers.put(issueNumber, timer);

        log.debug("为夺宝奇兵活动 {} 添加结束定时器，结束时间: {}", issueNumber, endTime);
    }

    /**
     * 移除活动的定时器
     *
     * @param issueNumber 期号
     */
    private void removeActivityTimer(long issueNumber) {
        TimerEvent<LuckyTreasureTimerEvent> timer = activityTimers.remove(issueNumber);
        if (timer != null) {
            timerCenter.remove(timer);
            log.debug("移除夺宝奇兵活动 {} 的定时器", issueNumber);
        }
    }

    /**
     * 处理活动结束定时器事件
     *
     * @param issueNumber 期号
     */
    private void handleActivityEndTimer(long issueNumber) {
        String lockKey = LuckyTreasureConstant.RedisLock.LUCKY_TREASURE_END + issueNumber;
        redisLock.tryLockAndRun(lockKey, () -> {
            // 从数据库获取活动数据
            LuckyTreasure round = luckyTreasureDao.findById(issueNumber).orElse(null);
            if (round == null || round.getEndTime() > 0) {
                return;
            }

            // 进行开奖
            reward(round);

            // 更新结束时间
            round.setEndTime(System.currentTimeMillis());
            luckyTreasureDao.save(round);

            LuckyTreasureConfig luckyTreasureConfig = round.getConfig();
            // 清理活跃状态
            luckyTreasureRedisDao.removeActiveRoundByIssueNumber(issueNumber);

            // 移除定时器
            removeActivityTimer(issueNumber);

            // 立即启动下一期活动
            startNextRoundForConfig(luckyTreasureConfig.getId());

            log.info("夺宝奇兵活动结束并开奖完成，期号: {}, 中奖玩家: {}", issueNumber, round.getAwardPlayerId());
        });
    }

    /**
     * 为指定配置启动下一期活动
     *
     * @param configId 配置ID
     */
    private void startNextRoundForConfig(int configId) {
        //验证小游戏是否开启
        if (isOpen()) {
            List<LuckyTreasureConfig> configs = configManager.getConfigs(LuckyTreasureConfig.class);
            configs.stream()
                    .filter(c -> c.getId() == configId && c.isRepeated())
                    .findFirst().ifPresent(this::startNewActivityForConfig);
        }
    }

    /**
     * 计算活动实际结束时间（毫秒）
     */
    private long calculateEndTimeMillis(LuckyTreasure luckyTreasure) {
        return luckyTreasure.getStartTime() + TimeUnit.MINUTES.toMillis(luckyTreasure.getConfig().getTime());
    }

    /**
     * 判断是否过期
     */
    private boolean isExpired(long endTimeMillis, long nowMillis) {
        return endTimeMillis <= nowMillis;
    }

    /**
     * 对指定的夺宝奇兵活动进行奖励发放
     */
    public void reward(LuckyTreasure luckyTreasure) {
        Map<Long, Integer> buyMap = luckyTreasure.getBuyMap();
        LuckyTreasureConfig config = luckyTreasure.getConfig();
        //计算中奖玩家
        long winnerPlayerId = randomKey(buyMap, config.getTotal());
        //有玩家中奖
        if (winnerPlayerId > 0) {
            luckyTreasure.setAwardPlayerId(winnerPlayerId);
            // 根据type类型处理奖励
            if (config.getType() == 1) {
                String rewardCode = rewardCodeGenerator.generateRewardCode(luckyTreasure.getIssueNumber(), winnerPlayerId);
                luckyTreasure.setRewardCode(rewardCode);
            }
            //标记未领取
            luckyTreasure.setReceived(false);
        }
    }

    /**
     * 随机获取一个key
     * 根据map的value和传入的总权重计算随机key
     *
     * @param buyMap     购买映射表，key为用户ID，value为购买数量
     * @param totalCount 总权重（可能大于map中value的总和）
     * @return 随机选中的key，如果没有选中则返回0
     */
    public long randomKey(Map<Long, Integer> buyMap, long totalCount) {
        // 如果总权重小于等于0或map为空，返回0
        if (totalCount <= 0 || buyMap == null || buyMap.isEmpty()) {
            return 0;
        }
        // 计算map中所有value的总和
        long mapValueSum = buyMap.values().stream().mapToLong(Integer::longValue).sum();
        // 如果map的value总和为0，返回0
        if (mapValueSum <= 0) {
            return 0;
        }
        // 生成随机数，范围是[0, totalCount)
        long randomValue = RandomUtils.randomLongMinMax(0L, totalCount);
        // 如果随机数大于等于map的value总和，说明没有选中任何key（按既有业务语义：可能不开奖）
        if (randomValue >= mapValueSum) {
            return 0;
        }
        // 遍历map，累加value直到找到对应的key
        long currentSum = 0;
        for (Map.Entry<Long, Integer> entry : buyMap.entrySet()) {
            currentSum += entry.getValue();
            if (randomValue < currentSum) {
                return entry.getKey();
            }
        }
        // 理论上不会到达这里，但为了安全起见返回0
        return 0;
    }

    /**
     * 判断当前节点是否为主节点
     */
    private boolean isCurrentNodeMaster() {
        return true;
//        return marsCurator.isMaster();
    }

    /**
     * 检查当前游戏是否处于开启状态。
     */
    public boolean isOpen() {
        return minigameManager.isOpenGame(LuckyTreasureConstant.Common.GAME_ID);
    }

}
