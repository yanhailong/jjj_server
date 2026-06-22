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
import com.jjg.game.alliance.pb.res.ResAllianceTaskList;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.manager.SnowflakeManager;
import com.jjg.game.social.service.SocialSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
    private SnowflakeManager snowflakeManager;
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
        long allianceId = cacheService.getAllianceId(playerId);
        AllianceData alliance = cacheService.getAlliance(allianceId);
        if (alliance == null) {
            res.code = Code.ALLIANCE_NOT_MEMBER;
            return res;
        }
        alliance = ensurePoolRefreshed(alliance);

        long now = System.currentTimeMillis();
        res.poolTasks = new ArrayList<>();
        for (AllianceTaskSlot slot : alliance.getTasks()) {
            if (slot.getExpireTime() > now) {
                res.poolTasks.add(AlliancePbConverter.toTaskInfo(slot, configService.taskCfg(slot.getCfgId())));
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
                res.myTask = AlliancePbConverter.toTaskInfo(taken,
                        configService.taskCfg(taken.getCfgId()), progressOf(playerId, taken.getUid()));
            }
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
        //保留未过期的, 补满到池上限 (任务可重复出现)
        List<AllianceTaskSlot> pool = new ArrayList<>(AllianceConst.Cfg.TASK_POOL_SIZE);
        for (AllianceTaskSlot slot : alliance.getTasks()) {
            if (slot.getExpireTime() > now) {
                pool.add(slot);
            }
        }
        int need = AllianceConst.Cfg.TASK_POOL_SIZE - pool.size();
        for (AllianceConfigService.TaskCfg cfg : configService.randomTasks(need)) {
            pool.add(new AllianceTaskSlot(snowflakeManager.nextId(), cfg.cfgId(), now, now + cfg.durationMs()));
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
    public ResAllianceAcceptTask acceptTask(long playerId, long taskUid) {
        ResAllianceAcceptTask res = new ResAllianceAcceptTask(Code.SUCCESS);
        long allianceId = cacheService.getAllianceId(playerId);
        AllianceData alliance = cacheService.getAlliance(allianceId);
        if (alliance == null) {
            res.code = Code.ALLIANCE_NOT_MEMBER;
            log.warn("接取联盟任务失败,玩家不在联盟 playerId={},taskUid={}", playerId, taskUid);
            return res;
        }
        long now = System.currentTimeMillis();
        AlliancePlayerData playerData = alliancePlayerDao.getOrEmpty(playerId);
        //放弃冷却
        if (playerData.getAbandonCdUntil() > now) {
            res.code = Code.ALLIANCE_IN_CD;
            log.warn("接取联盟任务失败,放弃冷却中 playerId={},taskUid={},cdUntil={}", playerId, taskUid, playerData.getAbandonCdUntil());
            return res;
        }
        //每日完成次数上限
        int today = TimeHelper.getDayNumerical();
        if (playerData.taskFinishCountOf(today) >= AllianceConst.Cfg.DAILY_TASK_LIMIT) {
            res.code = Code.ALLIANCE_TASK_TAKEN;
            log.warn("接取联盟任务失败,今日完成次数已达上限 playerId={},finished={},limit={}", playerId, playerData.taskFinishCountOf(today), AllianceConst.Cfg.DAILY_TASK_LIMIT);
            return res;
        }
        //单次只能接一条 (已有未超期任务)
        PlayerTakenTask current = playerData.getTakenTask();
        if (current != null) {
            if (!current.expired(now)) {
                res.code = Code.ALLIANCE_TASK_TAKEN;
                log.warn("接取联盟任务失败,已有进行中任务 playerId={},currentTaskUid={}", playerId, current.getUid());
                return res;
            }
            failTask(playerId, current);
        }
        //任务存在性与配置 (从最新缓存视图取)
        AllianceTaskSlot slot = alliance.findTask(taskUid);
        if (slot == null || slot.getExpireTime() <= now) {
            res.code = Code.NOT_FOUND;
            log.warn("接取联盟任务失败,任务不存在或已过期 playerId={},allianceId={},taskUid={}", playerId, allianceId, taskUid);
            return res;
        }
        AllianceConfigService.TaskCfg cfg = configService.taskCfg(slot.getCfgId());
        if (cfg == null) {
            res.code = Code.SAMPLE_ERROR;
            log.warn("接取联盟任务失败,任务配置缺失 playerId={},taskUid={},cfgId={}", playerId, taskUid, slot.getCfgId());
            return res;
        }
        //原子摘取 (并发接取只有一人成功)
        if (!allianceDao.pullTask(allianceId, taskUid)) {
            res.code = Code.ALLIANCE_TASK_TAKEN;
            log.warn("接取联盟任务失败,任务已被他人接取 playerId={},allianceId={},taskUid={}", playerId, allianceId, taskUid);
            return res;
        }
        cacheService.publishInvalidate(allianceId);

        PlayerTakenTask taken = new PlayerTakenTask(taskUid, slot.getCfgId(), allianceId, now, slot.getExpireTime());
        alliancePlayerDao.setTakenTask(playerId, taken);
        takenTaskCache.invalidate(playerId);
        //清残留进度 (需求: 放弃清进度, 再接取重新累计)
        stringRedisTemplate.delete(progressKey(playerId, taskUid));

        res.task = AlliancePbConverter.toTaskInfo(taken, cfg, 0);
        log.info("接取联盟任务 playerId={},allianceId={},taskUid={},cfgId={}", playerId, allianceId, taskUid, slot.getCfgId());
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
        long cdUntil = System.currentTimeMillis() + AllianceConst.Cfg.ABANDON_TASK_CD_SEC * 1000L;
        if (!clearTask(playerId, taken)) {
            res.code = Code.NOT_FOUND;
            log.warn("放弃联盟任务失败,清理任务快照失败 playerId={},taskUid={}", playerId, taken.getUid());
            return res;
        }
        alliancePlayerDao.setAbandonCd(playerId, cdUntil);
        res.cdUntil = cdUntil;
        log.info("放弃联盟任务 playerId={},taskUid={}", playerId, taken.getUid());
        return res;
    }

    // =====================================================================
    // 进度 (事件驱动, 高频路径)
    // =====================================================================

    /**
     * 任务进度上报 (由 {@code AllianceEventService} 统一调度)。
     * 高频路径: 任务快照走本地缓存, 无任务的玩家在缓存上直接短路, 不触达存储。
     *
     * @param goalType 目标类型 (AllianceConst.TaskGoalType)
     * @param param    事件参数 (EARN_GOLD=gameType / WIN_TIMES=本次倍数)
     * @param value    进度增量 (WIN_TIMES 场景固定 1)
     */
    public void onProgress(long playerId, int goalType, long param, long value) {
        if (value <= 0) {
            return;
        }
        //无盟玩家不可能有任务: 在最高频自旋路径上用本地缓存的"玩家->联盟"映射先短路,
        //避免给从未入盟的在线玩家也回源读其玩家文档 (getAllianceId 命中本地缓存, 近乎零成本)
        if (cacheService.getAllianceId(playerId) <= 0) {
            return;
        }
        PlayerTakenTask taken = cachedTakenTask(playerId);
        if (taken == null) {
            return;
        }
        AllianceConfigService.TaskCfg cfg = configService.taskCfg(taken.getCfgId());
        if (cfg == null || cfg.goalType() != goalType) {
            return;
        }
        //目标参数匹配
        switch (goalType) {
            case AllianceConst.TaskGoalType.EARN_GOLD -> {
                //cfg.goalParam=指定玩法(0不限), param=本次 gameType
                if (cfg.goalParam() > 0 && cfg.goalParam() != param) {
                    return;
                }
            }
            case AllianceConst.TaskGoalType.WIN_TIMES -> {
                //cfg.goalParam=最低倍数, param=本次倍数; 达标则次数+1
                if (param < cfg.goalParam()) {
                    return;
                }
                value = 1;
            }
            default -> {
                //COST_POWER / RECEIVE_HELP: 直接累计
            }
        }
        long now = System.currentTimeMillis();
        if (taken.expired(now)) {
            failTask(playerId, taken);
            return;
        }
        Long progress = stringRedisTemplate.opsForValue().increment(progressKey(playerId, taken.getUid()), value);
        if (progress != null && progress == value) {
            //新建的进度键补 TTL
            stringRedisTemplate.expire(progressKey(playerId, taken.getUid()),
                    AllianceConst.Cfg.TASK_PROGRESS_TTL_SEC, TimeUnit.SECONDS);
        }
        if (progress != null && progress >= cfg.goalCount()) {
            finishTask(playerId, taken, cfg);
        }
    }

    /**
     * 任务求助被帮助: 计入 RECEIVE_HELP 进度; 任务求助上限=1 次, 帮助即视为达成 (需求:
     * 同一任务只能由一名用户帮助, 被帮助者相当于完成任务)。
     *
     * @return true 该任务因此完成
     */
    public boolean onTaskHelped(long ownerId, long taskUid) {
        PlayerTakenTask taken = cachedTakenTask(ownerId);
        if (taken == null || taken.getUid() != taskUid) {
            return false;
        }
        AllianceConfigService.TaskCfg cfg = configService.taskCfg(taken.getCfgId());
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
    private boolean finishTask(long playerId, PlayerTakenTask taken, AllianceConfigService.TaskCfg cfg) {
        if (!clearTask(playerId, taken)) {
            return false;
        }
        int today = TimeHelper.getDayNumerical();
        alliancePlayerDao.incrementTaskFinish(playerId, today);

        assetService.grantContribution(playerId, cfg.rewardContribution(), cfg.rewardReputation(), taken.getAllianceId());
        assetService.grantReputation(taken.getAllianceId(), cfg.rewardReputation());

        NotifyAllianceTask notify = new NotifyAllianceTask(Code.SUCCESS);
        notify.result = TASK_RESULT_FINISH;
        notify.taskUid = taken.getUid();
        notify.cfgId = taken.getCfgId();
        notify.rewardContribution = cfg.rewardContribution();
        notify.rewardReputation = cfg.rewardReputation();
        socialSender.sendTo(playerId, notify);
        log.info("完成联盟任务 playerId={},taskUid={},cfgId={},allianceId={}",
                playerId, taken.getUid(), taken.getCfgId(), taken.getAllianceId());
        return true;
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
        notify.taskUid = taken.getUid();
        notify.cfgId = taken.getCfgId();
        socialSender.sendTo(playerId, notify);
        log.info("联盟任务超期失败 playerId={},taskUid={}", playerId, taken.getUid());
    }

    private boolean clearTask(long playerId, PlayerTakenTask taken) {
        boolean cleared = alliancePlayerDao.clearTakenTask(playerId, taken.getUid());
        takenTaskCache.invalidate(playerId);
        stringRedisTemplate.delete(progressKey(playerId, taken.getUid()));
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

    private long progressOf(long playerId, long taskUid) {
        try {
            String val = stringRedisTemplate.opsForValue().get(progressKey(playerId, taskUid));
            return val == null ? 0 : Long.parseLong(val);
        } catch (Exception e) {
            log.warn("读取任务进度失败 playerId={},taskUid={}", playerId, taskUid, e);
            return 0;
        }
    }

    private String progressKey(long playerId, long taskUid) {
        return AllianceConst.RedisKey.TASK_PROGRESS_PREFIX + playerId + ":" + taskUid;
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
