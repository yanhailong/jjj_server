package com.jjg.game.season.service;

import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.GlobalConfigCfg;
import com.jjg.game.sampledata.bean.SeasonStartCfg;
import com.jjg.game.season.dao.SeasonPlayerDao;
import com.jjg.game.season.data.SeasonFreeSpinResult;
import com.jjg.game.season.data.SeasonPlayerData;
import com.jjg.game.season.model.SeasonPhase;
import com.jjg.game.season.model.SeasonSnapshot;
import com.jjg.game.sim.constant.SimConstant;
import com.jjg.game.sim.data.SimPlayerContext;
import com.jjg.game.sim.service.SimAutoSaveService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * 赛季每日免费局 (进阶/循环赛季): 每天前 N 局赛季机台默认下注的普通旋转不扣下注额, 收益照常。
 * 次数记在 {@link SeasonPlayerData#getDailyFreeGameUsed()} 上, 随现有 dailyKey 机制每日 0 点重置;
 * PK 对局中的旋转不占用免费次数。消耗入口是 slots 扣费前的同步 RPC, 本服务在玩家线程执行。
 */
@Service
public class SeasonFreeGameService {
    private static final Logger log = LoggerFactory.getLogger(SeasonFreeGameService.class);

    private final SeasonLifecycleService lifecycleService;
    private final SeasonConfigService configService;
    private final SeasonPlayerDao seasonPlayerDao;
    private final SimAutoSaveService autoSaveService;

    public SeasonFreeGameService(SeasonLifecycleService lifecycleService, SeasonConfigService configService,
                                 SeasonPlayerDao seasonPlayerDao, SimAutoSaveService autoSaveService) {
        this.lifecycleService = lifecycleService;
        this.configService = configService;
        this.seasonPlayerDao = seasonPlayerDao;
        this.autoSaveService = autoSaveService;
    }

    /**
     * 每日免费局总次数 (全局配置)。
     */
    public int freeGameCount() {
        GlobalConfigCfg cfg = GameDataManager.getGlobalConfigCfg(SimConstant.Global.ID_SEASON_FEEE_GAME_COUNT);
        return cfg == null ? 0 : cfg.getIntValue();
    }

    /**
     * 今日剩余免费次数 (调用方须先经 ensureCurrent 完成每日重置)。
     */
    public int remainFreeGameCount(SeasonPlayerData data) {
        return Math.max(0, freeGameCount() - data.getDailyFreeGameUsed());
    }

    /**
     * 消耗一次免费次数。业务结果都以 SUCCESS + 结果体返回, 不可用时带原因供 slots 决定缓存策略。
     */
    public CommonResult<SeasonFreeSpinResult> useFreeGame(SimPlayerContext ctx, int gameType) {
        long systemTime = System.currentTimeMillis();
        SeasonSnapshot snapshot = lifecycleService.ensureCurrent(ctx, systemTime);
        SeasonPlayerData data = ctx.getSeasonPlayerData();
        SeasonFreeSpinResult result = new SeasonFreeSpinResult();
        int total = freeGameCount();
        int used = data.getDailyFreeGameUsed();
        result.setRemaining(Math.max(0, total - used));
        SeasonStartCfg cfg = configService.season(snapshot.seasonId());
        if (total <= 0 || snapshot.phase() == SeasonPhase.NOVICE
                || cfg == null || cfg.getAvailableGames() != gameType) {
            result.setReason(SeasonFreeSpinResult.REASON_UNAVAILABLE);
            return new CommonResult<>(Code.SUCCESS, result);
        }
        //PK 对局中的旋转按对局结算, 不占用免费次数
        if (data.getActiveMatch() != null) {
            result.setReason(SeasonFreeSpinResult.REASON_IN_MATCH);
            return new CommonResult<>(Code.SUCCESS, result);
        }
        if (used >= total) {
            result.setReason(SeasonFreeSpinResult.REASON_EXHAUSTED);
            return new CommonResult<>(Code.SUCCESS, result);
        }
        data.setDailyFreeGameUsed(used + 1);
        result.setFree(true);
        result.setRemaining(total - used - 1);
        autoSaveService.enqueueSave(data);
        log.info("赛季免费局消耗成功 playerId={},gameType={},used={},total={}",
                ctx.playerId(), gameType, used + 1, total);
        return new CommonResult<>(Code.SUCCESS, result);
    }

    /**
     * slots 进机台时的候选判定 (读 Mongo 快照, 只做门槛过滤, 消耗时以 sim 内存数据为准):
     * 当前处于进阶/循环赛季且该机台是本赛季游戏才需要在旋转前申请免费次数。
     */
    public boolean freeGameCandidate(long playerId, int gameType, long systemTime) {
        try {
            if (freeGameCount() <= 0) {
                return false;
            }
            SeasonPlayerData data = seasonPlayerDao.findById(playerId).orElse(null);
            if (data == null || data.getSeasonKey() == null) {
                return false;
            }
            SeasonPhase phase = data.seasonPhase();
            if (phase == null || phase == SeasonPhase.NOVICE) {
                return false;
            }
            long now = systemTime + data.getGmTimeOffset();
            if (now < data.getStartTime() || now >= data.getEndTime()) {
                return false;
            }
            return phaseAllowsFreeGame(data.getSeasonId(), phase, gameType);
        } catch (Exception e) {
            log.error("赛季免费局候选判定异常 playerId={},gameType={}", playerId, gameType, e);
            return false;
        }
    }

    /**
     * 使用 sim 内存中的权威赛季快照判定免费候选，避免首次进入时读取到尚未落库的 Mongo 旧数据。
     */
    public boolean freeGameCandidate(SeasonSnapshot snapshot, int gameType) {
        return freeGameCount() > 0 && snapshot != null
                && phaseAllowsFreeGame(snapshot.seasonId(), snapshot.phase(), gameType);
    }

    private boolean phaseAllowsFreeGame(int seasonId, SeasonPhase phase, int gameType) {
        if (phase == null || phase == SeasonPhase.NOVICE) {
            return false;
        }
        SeasonStartCfg cfg = configService.season(seasonId);
        return cfg != null && cfg.getAvailableGames() == gameType;
    }

    /**
     * 系统时间的日期 key (yyyyMMdd), 与赛季每日重置的 dailyKey 同构, 供 slots 侧按天缓存"已耗尽"。
     */
    public static int dailyKey(long millis) {
        LocalDate date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate();
        return date.getYear() * 10_000 + date.getMonthValue() * 100 + date.getDayOfMonth();
    }
}
