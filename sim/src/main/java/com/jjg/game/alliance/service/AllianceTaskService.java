package com.jjg.game.alliance.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.jjg.game.alliance.constant.AllianceConst;
import com.jjg.game.alliance.dao.AllianceDao;
import com.jjg.game.alliance.dao.AlliancePlayerDao;
import com.jjg.game.alliance.data.AllianceData;
import com.jjg.game.alliance.data.AlliancePlayerData;
import com.jjg.game.alliance.data.AllianceTaskSlot;
import com.jjg.game.alliance.data.PlayerTakenTask;
import com.jjg.game.alliance.pb.AlliancePbConverter;
import com.jjg.game.alliance.pb.res.NotifyAllianceTask;
import com.jjg.game.alliance.pb.res.ResAbandonTask;
import com.jjg.game.alliance.pb.res.ResAllianceAcceptTask;
import com.jjg.game.alliance.pb.res.ResAllianceFinishedTask;
import com.jjg.game.alliance.pb.res.ResAllianceTaskList;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.utils.ItemUtils;
import com.jjg.game.sampledata.bean.TaskCfg;
import com.jjg.game.sim.constant.SimConstant;
import com.jjg.game.sim.service.SimPackService;
import com.jjg.game.sim.data.SpinStatInfo;
import com.jjg.game.social.service.SocialSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 联盟循环任务: 任务池补齐 / 接取 / 进度 / 完成 / 放弃。
 * <p>
 * 关键设计:
 * <ul>
 *   <li>任务池整点补齐采用<b>惰性刷新</b>: 任一玩家访问任务时发现 taskRefreshHour 落后即现场补齐,
 *       用旧值条件更新保证多节点只补一次 —— 避免 leader 定时遍历全部联盟的扫描;</li>
 *   <li>接取 = 联盟文档 $pull 原子摘取(独占), 放弃不退回池(需求: 直接删除);</li>
 *   <li>进度计数在 Redis INCR (spin 等高频事件不打 Mongo), 玩家文档只存任务快照;
 *       进度上报路径用本地缓存的任务快照(同一玩家的事件只发生在其会话所在节点, 本地失效即可);</li>
 *   <li>超期惰性判定: 查询/上报进度时发现过期即结算失败。</li>
 * </ul>
 *
 * @author 11
 * @date 2026/6/11
 */
@Service
public class AllianceTaskService {
    private static final Logger log = LoggerFactory.getLogger(AllianceTaskService.class);

    //任务完成结果
    public static final int TASK_RESULT_FINISH = 1;
    public static final int TASK_RESULT_FAIL = 2;

    @Autowired
    private AllianceDao allianceDao;
    @Autowired
    private AlliancePlayerDao alliancePlayerDao;
    @Autowired
    private AllianceCacheService cacheService;
    @Autowired
    private AllianceConfigService configService;
    @Autowired
    private AllianceAssetService assetService;
    @Autowired
    private SimPackService simPackService;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private SocialSender socialSender;

    /**
     * 玩家当前任务快照本地缓存: 进度上报(spin)高频, 不能每次读 Mongo。
     * 同一玩家的游戏事件只发生在其会话所在节点, 接取/放弃/完成时本地失效即可, 无需跨节点广播。
     */
    private final Cache<Long, Optional<PlayerTakenTask>> takenTaskCache = Caffeine.newBuilder()
            .expireAfterWrite(60, TimeUnit.SECONDS)
            .maximumSize(20000)
            .build();

    // =====================================================================
    // 查询 (含惰性补齐)
    // =====================================================================

    /**
     * 任务列表: 先确保任务池已按当前整点补齐, 再返回池 + 我的任务。
     */
    public ResAllianceTaskList taskList(long playerId) {
        ResAllianceTaskList res = new ResAllianceTaskList(Code.SUCCESS);
        try {
            long allianceId = cacheService.getAllianceId(playerId);
            if (allianceId < 1) {
                res.code = Code.NOT_FOUND;
                log.warn("获取联盟任务失败，玩家没有联盟 playerId={}", playerId);
                return res;
            }
            AllianceData alliance = cacheService.getAlliance(allianceId);
            if (alliance == null) {
                res.code = Code.NOT_FOUND;
                log.warn("获取联盟任务失败，未找到联盟信息 playerId={},allianceId={}", playerId, allianceId);
                return res;
            }
            alliance = ensurePoolRefreshed(alliance);

            long now = System.currentTimeMillis();
            res.poolTasks = new ArrayList<>();
            for (Map.Entry<Integer, AllianceTaskSlot> en : alliance.getTasks().entrySet()) {
                AllianceTaskSlot value = en.getValue();
                if (value.getExpireTime() > now && configService.taskCfg(value.getCfgId()) != null) {
                    res.poolTasks.add(AlliancePbConverter.toTaskInfo(value));
                }
            }
            res.nextRefreshTime = nextHourMillis(now);

            AlliancePlayerData playerData = alliancePlayerDao.getOrEmpty(playerId);
            int today = TimeHelper.getDayNumerical();
            res.dailyFinished = playerData.taskFinishCountOf(today);
            res.dailyLimit = AllianceConst.Cfg.DAILY_TASK_LIMIT;
            res.abandonCdUntil = playerData.getAbandonCdUntil();

            PlayerTakenTask taken = playerData.getTakenTask();
            if (taken != null) {
                if (taken.expired(now)) {
                    //超期惰性结算失败
                    failTask(playerId, taken);
                } else {
                    res.myTask = AlliancePbConverter.toTaskInfo(taken, progressOf(playerId, taken.getCfgId()), null);
                }
            }
        } catch (Exception e) {
            log.error("", e);
            res.code = Code.EXCEPTION;
        }
        return res;
    }

    /**
     * 已完成任务列表 (最近 N 条, 最新在前)。
     */
    public ResAllianceFinishedTask finishedTaskList(long playerId) {
        ResAllianceFinishedTask res = new ResAllianceFinishedTask(Code.SUCCESS);
        try {
            AlliancePlayerData playerData = alliancePlayerDao.getOrEmpty(playerId);
            List<PlayerTakenTask> finished = playerData.getFinishedTasks();
            res.tasks = new ArrayList<>();
            if (finished != null) {
                for (int i = finished.size() - 1; i >= 0; i--) {
                    res.tasks.add(AlliancePbConverter.toTaskInfo(finished.get(i), 0, null));
                }
            }
        } catch (Exception e) {
            log.error("", e);
            res.code = Code.EXCEPTION;
        }
        return res;
    }

    /**
     * 任务池惰性补齐: taskRefreshHour 落后于当前整点时现场补满, 条件更新保证多节点只补一次。
     */
    private AllianceData ensurePoolRefreshed(AllianceData alliance) {
        long currentHour = currentHourKey();
        if (alliance.getTaskRefreshHour() >= currentHour) {
            return alliance;
        }
        long now = System.currentTimeMillis();
        //保留未过期的(按 cfgId 去重), 再用"不在池中的其它任务"补满到池上限。
        //池按 cfgId 索引: 每种任务最多一条, 接取即按 cfgId 移除。
        Map<Integer, AllianceTaskSlot> pool = new HashMap<>();
        for (AllianceTaskSlot slot : alliance.getTasks().values()) {
            if (slot.getExpireTime() > now) {
                pool.put(slot.getCfgId(), slot);
            }
        }
        int need = AllianceConst.Cfg.TASK_POOL_SIZE - pool.size();
        for (TaskCfg cfg : configService.randomTasks(need, pool.keySet())) {
            long durationMs = Math.max(1, cfg.getDuration()) * 60_000L;
            pool.put(cfg.getId(), new AllianceTaskSlot(cfg.getId(), now, now + durationMs));
        }
        if (allianceDao.refreshTasks(alliance.getAllianceId(), alliance.getTaskRefreshHour(), currentHour, pool)) {
            cacheService.publishInvalidate(alliance.getAllianceId());
            log.info("联盟任务池补齐 allianceId={},hour={},added={}", alliance.getAllianceId(), currentHour, need);
        }
        //无论本节点是否补齐成功, 重读取最新视图
        AllianceData fresh = cacheService.getAlliance(alliance.getAllianceId());
        return fresh == null ? alliance : fresh;
    }

    // =====================================================================
    // 接取 / 放弃
    // =====================================================================

    /**
     * 接取任务: $pull 原子摘取, 摘到者独占。
     */
    public ResAllianceAcceptTask acceptTask(long playerId, int taskCfgId) {
        ResAllianceAcceptTask res = new ResAllianceAcceptTask(Code.SUCCESS);
        long allianceId = cacheService.getAllianceId(playerId);
        AllianceData alliance = cacheService.getAlliance(allianceId);
        if (alliance == null) {
            res.code = Code.NOT_FOUND;
            log.warn("接取联盟任务失败,玩家不在联盟 playerId={},taskCfgId={}", playerId, taskCfgId);
            return res;
        }
        long now = System.currentTimeMillis();
        AlliancePlayerData playerData = alliancePlayerDao.getOrEmpty(playerId);
        //放弃冷却
        if (playerData.getAbandonCdUntil() > now) {
            res.code = Code.FORBID;
            log.warn("接取联盟任务失败,放弃冷却中 playerId={},taskCfgId={},cdUntil={}", playerId, taskCfgId, playerData.getAbandonCdUntil());
            return res;
        }
        //每日完成次数上限
        int today = TimeHelper.getDayNumerical();
        if (playerData.taskFinishCountOf(today) >= AllianceConst.Cfg.DAILY_TASK_LIMIT) {
            res.code = Code.REPEAT_OP;
            log.warn("接取联盟任务失败,今日完成次数已达上限 playerId={},finished={},limit={}", playerId, playerData.taskFinishCountOf(today), AllianceConst.Cfg.DAILY_TASK_LIMIT);
            return res;
        }
        //单次只能接一条 (已有未超期任务)
        PlayerTakenTask current = playerData.getTakenTask();
        if (current != null) {
            if (!current.expired(now)) {
                res.code = Code.REPEAT_OP;
                log.warn("接取联盟任务失败,已有进行中任务 playerId={},currentTaskCfgIf={}", playerId, current.getCfgId());
                return res;
            }
            failTask(playerId, current);
        }
        //任务存在性与配置 (从最新缓存视图取)
        AllianceTaskSlot slot = alliance.findTask(taskCfgId);
        if (slot == null || slot.getExpireTime() <= now) {
            res.code = Code.NOT_FOUND;
            log.warn("接取联盟任务失败,任务不存在或已过期 playerId={},allianceId={},taskCfgId={}", playerId, allianceId, taskCfgId);
            return res;
        }
        TaskCfg cfg = configService.taskCfg(slot.getCfgId());
        if (cfg == null) {
            res.code = Code.SAMPLE_ERROR;
            log.warn("接取联盟任务失败,任务配置缺失 playerId={},taskCfgId={},cfgId={}", playerId, taskCfgId, slot.getCfgId());
            return res;
        }
        //原子摘取 (并发接取只有一人成功)
        if (!allianceDao.pullTask(allianceId, taskCfgId)) {
            res.code = Code.REPEAT_OP;
            log.warn("接取联盟任务失败,任务已被他人接取 playerId={},allianceId={},taskCfgId={}", playerId, allianceId, taskCfgId);
            return res;
        }
        cacheService.publishInvalidate(allianceId);

        PlayerTakenTask taken = new PlayerTakenTask(slot.getCfgId(), allianceId, now, (long) cfg.getDuration() * TimeHelper.ONE_MINUTE_OF_MILLIS + now);
        alliancePlayerDao.setTakenTask(playerId, taken);
        takenTaskCache.invalidate(playerId);
        //清残留进度 (需求: 放弃清进度, 再接取重新累计)
        stringRedisTemplate.delete(progressKey(playerId, taskCfgId));

        res.task = AlliancePbConverter.toTaskInfo(taken, 0, cfg);
        log.info("接取联盟任务 playerId={},allianceId={},taskCfgId={},cfgId={}", playerId, allianceId, taskCfgId, slot.getCfgId());
        return res;
    }

    /**
     * 放弃任务: 清进度 + 冷却, 任务不退回池 (需求: 直接删除)。
     */
    public ResAbandonTask abandonTask(long playerId) {
        ResAbandonTask res = new ResAbandonTask(Code.SUCCESS);
        AlliancePlayerData playerData = alliancePlayerDao.getOrEmpty(playerId);
        PlayerTakenTask taken = playerData.getTakenTask();
        if (taken == null) {
            res.code = Code.NOT_FOUND;
            log.warn("放弃联盟任务失败,无进行中任务 playerId={}", playerId);
            return res;
        }
        TaskCfg cfg = configService.taskCfg(taken.getCfgId());
        if (cfg == null) {
            res.code = Code.SAMPLE_ERROR;
            log.warn("放弃联盟任务失败,任务配置缺失 playerId={},cfgId={}", playerId, taken.getCfgId());
            return res;
        }
        if (!cfg.getAllowAbandon()) {
            res.code = Code.FORBID;
            log.warn("放弃联盟任务失败,配置不允许放弃 playerId={},cfgId={}", playerId, taken.getCfgId());
            return res;
        }
        long cdUntil = System.currentTimeMillis() + Math.max(0, cfg.getAbandonCooldown()) * 1000L;
        if (!clearTask(playerId, taken)) {
            res.code = Code.NOT_FOUND;
            log.warn("放弃联盟任务失败,清理任务快照失败 playerId={},taskCfgId={}", playerId, taken.getCfgId());
            return res;
        }
        alliancePlayerDao.setAbandonCd(playerId, cdUntil);
        res.cdUntil = cdUntil;
        log.info("放弃联盟任务 playerId={},taskCfgId={}", playerId, taken.getCfgId());
        return res;
    }

    // =====================================================================
    // 进度 (事件驱动, 高频路径)
    // =====================================================================

    /**
     * 任务进度上报 (由 {@code AllianceEventService} 统一调度)。
     * 高频路径: 任务快照走本地缓存, 无任务的玩家在缓存上直接短路, 不触达存储。
     *
     * @param conditionId task.xlsx 的 taskConditionId 首位
     * @param param       事件参数
     * @param value       进度增量
     */
    public void onProgress(long playerId, int conditionId, long param, long value) {
        onProgress(playerId, new TaskEvent(conditionId, param, value, 0, 0, 0));
    }

    /**
     * 任务求助被帮助: 求助上限=1 次, 帮助即视为达成 (需求:
     * 同一任务只能由一名用户帮助, 被帮助者相当于完成任务)。
     *
     * @return true 该任务因此完成
     */
    public void onSpin(long playerId, int gameType, int winTimes, int costPower, SpinStatInfo statInfo) {
        long bet = statInfo == null ? costPower : statInfo.getBet();
        long win = statInfo == null ? 0 : statInfo.getWin();
        if (costPower > 0 || bet > 0) {
            onProgress(playerId, new TaskEvent(AllianceConst.TaskConditionType.BET_TIMES, gameType, 1, bet, gameType, 0));
        }
        if (winTimes > 0) {
            onProgress(playerId, new TaskEvent(AllianceConst.TaskConditionType.WIN_TIMES, winTimes, 1, bet, gameType, 0));
        }
        if (win > 0) {
            onProgress(playerId, new TaskEvent(AllianceConst.TaskConditionType.WIN_AMOUNT, 0, win, bet, gameType, ItemUtils.getGoldItemId()));
        }
    }

    public void onEarnGold(long playerId, int gameType, long gold) {
        onProgress(playerId, new TaskEvent(AllianceConst.TaskConditionType.WIN_AMOUNT, 0, gold,
                Long.MAX_VALUE, gameType, ItemUtils.getGoldItemId()));
    }

    private void onProgress(long playerId, TaskEvent event) {
        if (event.value() <= 0) {
            return;
        }
        if (cacheService.getAllianceId(playerId) <= 0) {
            return;
        }
        PlayerTakenTask taken = cachedTakenTask(playerId);
        if (taken == null) {
            return;
        }
        TaskCfg cfg = configService.taskCfg(taken.getCfgId());
        if (cfg == null || !matchesEvent(cfg, event)) {
            return;
        }
        long now = System.currentTimeMillis();
        if (taken.expired(now)) {
            failTask(playerId, taken);
            return;
        }
        Long progress = stringRedisTemplate.opsForValue().increment(progressKey(playerId, taken.getCfgId()), event.value());
        if (progress != null && progress == event.value()) {
            stringRedisTemplate.expire(progressKey(playerId, taken.getCfgId()),
                    AllianceConst.Cfg.TASK_PROGRESS_TTL_SEC, TimeUnit.SECONDS);
        }
        if (progress != null && progress >= targetValue(cfg)) {
            finishTask(playerId, taken, cfg);
        }
    }

    private boolean matchesEvent(TaskCfg cfg, TaskEvent event) {
        List<Long> cond = cfg.getTaskConditionId();
        if (cond == null || cond.isEmpty() || longAt(cond, 0, 0) != event.conditionId()) {
            return false;
        }
        return switch (event.conditionId()) {
            case AllianceConst.TaskConditionType.WIN_TIMES -> matchOptional(longAt(cond, 1, 0), event.gameType())
                    && event.bet() >= longAt(cond, 2, 0)
                    && event.param() >= longAt(cond, 3, 0);
            case AllianceConst.TaskConditionType.BET_TIMES -> matchOptional(longAt(cond, 1, 0), event.gameType())
                    && event.bet() >= longAt(cond, 2, 0);
            case AllianceConst.TaskConditionType.WIN_AMOUNT -> matchOptional(longAt(cond, 1, 0), event.gameType())
                    && event.bet() >= longAt(cond, 2, 0)
                    && matchOptional(longAt(cond, 4, 0), event.coinId());
            case AllianceConst.TaskConditionType.POOL_DRAW_TIMES,
                 AllianceConst.TaskConditionType.SKILL_RESEARCH_TIMES,
                 AllianceConst.TaskConditionType.BUILDING_UPGRADE_TIMES ->
                    matchOptional(longAt(cond, 1, 0), event.param());
            case AllianceConst.TaskConditionType.RECHARGE_AMOUNT -> matchOptional(longAt(cond, 2, 0), event.param());
            case AllianceConst.TaskConditionType.DONATE_TIMES -> event.param() >= longAt(cond, 1, 0);
            default -> matchOptional(longAt(cond, 1, 0), event.param());
        };
    }

    private long targetValue(TaskCfg cfg) {
        List<Long> cond = cfg.getTaskConditionId();
        if (cond == null || cond.isEmpty()) {
            return Long.MAX_VALUE;
        }
        int conditionId = cond.getFirst().intValue();
        return switch (conditionId) {
            case AllianceConst.TaskConditionType.WIN_AMOUNT -> longAt(cond, 3, Long.MAX_VALUE);
            case AllianceConst.TaskConditionType.RECHARGE_AMOUNT -> longAt(cond, 1, Long.MAX_VALUE);
            default -> cond.getLast();
        };
    }

    private boolean matchOptional(long expected, long actual) {
        return expected <= 0 || expected == actual;
    }

    private long longAt(List<Long> values, int index, long defaultValue) {
        return values.size() > index ? values.get(index) : defaultValue;
    }

    private record TaskEvent(int conditionId, long param, long value, long bet, int gameType, int coinId) {
    }

    public boolean onTaskHelped(long ownerId, int taskCfgId) {
        PlayerTakenTask taken = cachedTakenTask(ownerId);
        if (taken == null || taken.getCfgId() != taskCfgId) {
            return false;
        }
        TaskCfg cfg = configService.taskCfg(taken.getCfgId());
        if (cfg == null) {
            return false;
        }
        if (taken.expired(System.currentTimeMillis())) {
            failTask(ownerId, taken);
            return false;
        }
        return finishTask(ownerId, taken, cfg);
    }

    // =====================================================================
    // 完成 / 失败
    // =====================================================================

    /**
     * 完成: 发奖(贡献值+联盟声誉) -> 清快照 -> 每日计数 -> 通知。
     * 声誉入账给"接取时所在联盟" —— 期间换盟则老盟得声誉, 与对决积分归属口径一致。
     */
    private boolean finishTask(long playerId, PlayerTakenTask taken, TaskCfg cfg) {
        if (!clearTask(playerId, taken)) {
            return false;
        }
        taken.setFinishTime(System.currentTimeMillis());
        alliancePlayerDao.pushFinishedTask(playerId, taken, AllianceConst.Cfg.FINISHED_TASK_KEEP);
        int today = TimeHelper.getDayNumerical();
        alliancePlayerDao.incrementTaskFinish(playerId, today);

        grantTaskRewards(playerId, taken.getAllianceId(), cfg);

        NotifyAllianceTask notify = new NotifyAllianceTask(Code.SUCCESS);
        notify.result = TASK_RESULT_FINISH;
        notify.cfgId = taken.getCfgId();
        socialSender.sendTo(playerId, notify);
        log.info("完成联盟任务 playerId={},cfgId={},allianceId={}",
                playerId, taken.getCfgId(), taken.getAllianceId());
        return true;
    }

    private void grantTaskRewards(long playerId, long allianceId, TaskCfg cfg) {
        Map<Integer, Long> rewards = cfg.getGetItem();
        if (rewards == null || rewards.isEmpty()) {
            return;
        }
        long reputation = 0;
        long contribution = 0;
        Map<Integer, Long> packRewards = new HashMap<>();
        for (Map.Entry<Integer, Long> en : rewards.entrySet()) {
            if (en.getKey() == null || en.getValue() == null || en.getValue() <= 0) {
                continue;
            }
            if (en.getKey() == SimConstant.Item.ID_ALLIANCE_REPUTATION) {
                reputation += en.getValue();
            } else if (en.getKey() == SimConstant.Item.ID_ALLIANCE_Contribution) {
                contribution += en.getValue();
            } else {
                packRewards.merge(en.getKey(), en.getValue(), Long::sum);
            }
        }
        if (contribution > 0 || reputation > 0) {
            assetService.grantContribution(playerId, contribution, reputation, allianceId);
        }
        if (reputation > 0) {
            assetService.grantReputation(allianceId, reputation);
        }
        if (!packRewards.isEmpty()) {
            //统一走 sim 道具入口: 在线入内存资源/背包, 离线则进背包待上线迁移 (能量等 sim 特殊资源)
            simPackService.addItemsByPlayerId(playerId, packRewards, AddType.ALLIANCE_TASK_REWARD, "", true);
        }
    }

    /**
     * 超期失败: 清快照 + 通知 (无惩罚, 视为自动放弃)。
     */
    private void failTask(long playerId, PlayerTakenTask taken) {
        if (!clearTask(playerId, taken)) {
            return;
        }
        NotifyAllianceTask notify = new NotifyAllianceTask(Code.SUCCESS);
        notify.result = TASK_RESULT_FAIL;
        notify.cfgId = taken.getCfgId();
        socialSender.sendTo(playerId, notify);
        log.info("联盟任务超期失败 playerId={},taskCfgId={}", playerId, taken.getCfgId());
    }

    private boolean clearTask(long playerId, PlayerTakenTask taken) {
        boolean cleared = alliancePlayerDao.clearTakenTask(playerId, taken.getCfgId());
        takenTaskCache.invalidate(playerId);
        stringRedisTemplate.delete(progressKey(playerId, taken.getCfgId()));
        return cleared;
    }

    // =====================================================================
    // 工具
    // =====================================================================

    /**
     * 当前任务快照 (本地缓存, 无任务缓存 empty 短路高频事件)。
     */
    private PlayerTakenTask cachedTakenTask(long playerId) {
        Optional<PlayerTakenTask> opt = takenTaskCache.get(playerId,
                pid -> Optional.ofNullable(alliancePlayerDao.getTakenTask(pid)));
        return opt == null ? null : opt.orElse(null);
    }

    private long progressOf(long playerId, int taskCfgId) {
        try {
            String val = stringRedisTemplate.opsForValue().get(progressKey(playerId, taskCfgId));
            return val == null ? 0 : Long.parseLong(val);
        } catch (Exception e) {
            log.warn("读取任务进度失败 playerId={},taskCfgId={}", playerId, taskCfgId, e);
            return 0;
        }
    }

    private String progressKey(long playerId, int taskCfgId) {
        return AllianceConst.RedisKey.TASK_PROGRESS_PREFIX + playerId + ":" + taskCfgId;
    }

    /**
     * 当前整点标识 (yyyyMMddHH)
     */
    private long currentHourKey() {
        LocalDateTime now = LocalDateTime.now();
        return now.getYear() * 1000000L + now.getMonthValue() * 10000L + now.getDayOfMonth() * 100L + now.getHour();
    }

    /**
     * 下一个整点的毫秒时间戳
     */
    private long nextHourMillis(long now) {
        LocalDateTime next = LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(now),
                ZoneId.systemDefault()).plusHours(1).withMinute(0).withSecond(0).withNano(0);
        return next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}
