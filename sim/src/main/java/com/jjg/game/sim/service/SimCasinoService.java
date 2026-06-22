package com.jjg.game.sim.service;

import com.alibaba.fastjson.JSONObject;
import com.jjg.game.alliance.service.AllianceCacheService;
import com.jjg.game.core.constant.Code;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BuildingUpgradeTableCfg;
import com.jjg.game.sampledata.bean.CasinoListCfg;
import com.jjg.game.sampledata.bean.CasinoStatsSheetCfg;
import com.jjg.game.sim.constant.BonusType;
import com.jjg.game.sim.constant.SimConstant;
import com.jjg.game.sim.dao.SimCasinoDao;
import com.jjg.game.sim.data.*;
import com.jjg.game.sim.pb.SimPbConverter;
import com.jjg.game.sim.pb.res.ResSimCasinoInfo;
import com.jjg.game.sim.pb.res.ResSwitchCasino;
import com.jjg.game.sim.pb.res.ResUnlockCasino;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * @author 11
 * @date 2026/5/28
 */
@Service
public class SimCasinoService {

    private final Logger log = LoggerFactory.getLogger(SimCasinoService.class);

    //新建建筑初始等级
    private static final int INITIAL_BUILDING_LEVEL = 1;

    private final String TABLE_NAME = "simCasinoUnlock";

    @Autowired
    private SimCasinoDao simCasinoDao;
    @Autowired
    private SimBuildingService simBuildingService;
    @Autowired
    private SimConfigCacheService configCacheService;
    @Autowired
    private SimEmployeeService employeeService;
    @Autowired
    private SimAutoSaveService autoSaveService;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private SimSkillService simSkillService;
    @Autowired
    private AllianceCacheService allianceCacheService;

    /**
     * 开辟新场景 (校验 condition 后创建并落库, 不自动切换)
     */
    public void onUnlockCasino(SimPlayerContext ctx, int targetCasinoId) {
        ResUnlockCasino res = new ResUnlockCasino(Code.SUCCESS);
        res.casinoId = targetCasinoId;
        try {
            CasinoListCfg cfg = GameDataManager.getCasinoListCfg(targetCasinoId);
            if (cfg == null) {
                log.warn("开辟新场景失败, 配置不存在 playerId={},casinoId={}", ctx.playerId(), targetCasinoId);
                res.code = Code.NOT_FOUND;
                ctx.send(res);
                return;
            }
            //已拥有?
            if (simCasinoDao.findOne(ctx.playerId(), targetCasinoId) != null) {
                log.warn("开辟新场景失败, 已拥有 playerId={},casinoId={}", ctx.playerId(), targetCasinoId);
                res.code = Code.PARAM_ERROR;
                ctx.send(res);
                return;
            }
            //condition: 前置场景经营等级达标
            if (!checkCondition(ctx, cfg)) {
                res.code = Code.NOT_ENOUGH;
                ctx.send(res);
                return;
            }
            SimCasinoData casino = buildNewCasino(ctx, targetCasinoId);
            simCasinoDao.save(casino);
            log.info("开辟新场景成功 playerId={},casinoId={}", ctx.playerId(), targetCasinoId);
        } catch (Exception e) {
            log.error("", e);
            res.code = Code.EXCEPTION;
        }
        ctx.send(res);
    }

    /**
     * 切换场景 (落库旧场景, 加载并下发目标场景数据)
     */
    public void onSwitchCasino(SimPlayerContext ctx, int targetCasinoId) {
        ResSwitchCasino res = new ResSwitchCasino(Code.SUCCESS);
        try {
            if (targetCasinoId == ctx.getSimBaseData().getCurrentCasinoId()) {
                log.warn("切换场景失败, 已在该场景 playerId={},casinoId={}", ctx.playerId(), targetCasinoId);
                res.code = Code.PARAM_ERROR;
                ctx.send(res);
                return;
            }
            //目标场景必须已拥有
            SimCasinoData target = simCasinoDao.findOne(ctx.playerId(), targetCasinoId);
            if (target == null) {
                log.warn("切换场景失败, 目标场景未拥有 playerId={},casinoId={}", ctx.playerId(), targetCasinoId);
                res.code = Code.NOT_FOUND;
                ctx.send(res);
                return;
            }
            //结算旧场景已积累产出, 再同步落库 (先排空异步在途写, 避免旧快照覆盖)
            SimCasinoData old = ctx.getCurrentCasino();
            if (old != null) {
                simBuildingService.settleOnlineOutput(ctx);
                autoSaveService.awaitPending();
                simCasinoDao.save(old);
            }
            //切换到目标场景 (新加载实体的运行时 transient 字段天然为初始值)
            ctx.setCurrentCasino(target);
            ctx.switchCasino(targetCasinoId);
            simBuildingService.completeAllBuildingUpgrade(target);

            SimCasinoData casino = ctx.getCurrentCasino();
            res.currentCasinoId = casino.getCasinoId();
            res.buildings = SimPbConverter.toBuildingInfos(casino);
            res.managerEmployInfos = SimPbConverter.toManagerInfos(casino);
            res.awareness = casino.getAwareness();

            log.info("切换场景 playerId={},res={}", ctx.playerId(), JSONObject.toJSONString(res));
        } catch (Exception e) {
            log.error("", e);
            res.code = Code.EXCEPTION;
        }
        ctx.send(res);
    }

    /**
     * 获取场景信息
     *
     * @param ctx
     */
    public void onCasinoInfo(SimPlayerContext ctx) {
        ResSimCasinoInfo res = new ResSimCasinoInfo(Code.SUCCESS);
        try {
            SimCasinoData casinoData = ctx.getCurrentCasino();
            if (casinoData == null) {
                log.warn("获取场景信息失败, 当前场景为空 playerId={}", ctx.playerId());
                res.code = Code.NOT_FOUND;
                ctx.send(res);
                return;
            }
            res.level = casinoData.getCasinoLevel();
            res.exp = casinoData.getExp();

            CasinoStatsSheetCfg cfg = configCacheService.getCasinoStatsSheetCfg(casinoData.getCasinoId(), casinoData.getCasinoLevel());
            res.upgradeCost = cfg.getUpgradeCost();
            res.allianceId = allianceCacheService.getAllianceId(ctx.playerId());
        } catch (Exception e) {
            log.error("", e);
            res.code = Code.EXCEPTION;
        }
        ctx.send(res);
    }


    /**
     * 加载场景数据
     *
     * @param ctx
     * @param baseData
     * @return 当前场景; 加载失败返回 null
     */
    public SimCasinoData loadCasinoData(SimPlayerContext ctx, SimBaseData baseData) {
        SimCasinoData currentCasino;
        if (baseData.getCurrentCasinoId() > 0) {
            //加载当前所在场景
            currentCasino = simCasinoDao.findOne(ctx.playerId(), baseData.getCurrentCasinoId());
        } else {
            //新玩家: 初始化默认场景并落库
            currentCasino = initDefaultCasino(ctx);
            if (currentCasino != null) {
                baseData.setCurrentCasinoId(currentCasino.getCasinoId());
                simCasinoDao.save(currentCasino);
            }
        }

        if (currentCasino == null) {
            log.warn("加载场景数据失败 playerId={}", ctx.playerId());
            return null;
        }

        ctx.setCurrentCasino(currentCasino);
        //检查是否有建筑完成升级
        simBuildingService.completeAllBuildingUpgrade(currentCasino);
        return currentCasino;
    }

    /**
     * 初始化新玩家的默认场景 (取 CasinoList 中无解锁条件的场景, 通常为 casinoId=1)
     */
    public SimCasinoData initDefaultCasino(SimPlayerContext ctx) {
        int defaultCasinoId = SimConstant.Common.DEFAULT_CASINO_ID;
        for (Map.Entry<Integer, CasinoListCfg> en : GameDataManager.getCasinoListCfgMap().entrySet()) {
            Map<Integer, Integer> condition = en.getValue().getCondition();
            if (condition == null || condition.isEmpty()) {
                defaultCasinoId = en.getValue().getId();
                break;
            }
        }
        return buildNewCasino(ctx, defaultCasinoId);
    }

    /**
     * 配置驱动创建一个新场景 (初始化/开辟共用): 设置经营等级、繁荣度, 并放入该场景的初始建筑。
     *
     * @param casinoId 场景id (= CasinoListCfg.id = BuildingAreaTableCfg.RegionID)
     */
    public SimCasinoData buildNewCasino(SimPlayerContext ctx, int casinoId) {
        SimCasinoData casino = new SimCasinoData();
        casino.setPlayerId(ctx.playerId());
        casino.setCasinoId(casinoId);

        int statsId = resolveInitialStatsId(casinoId);
        CasinoStatsSheetCfg statsCfg = GameDataManager.getCasinoStatsSheetCfg(statsId);
        if (statsCfg != null) {
            casino.setProsperity(statsCfg.getProsperity());
            casino.setCasinoLevel(statsCfg.getLevel());
        }

        updateCasinoUnlock(ctx.playerId(), casinoId, INITIAL_BUILDING_LEVEL);
        simSkillService.initUnlock(ctx, casinoId);
        ctx.getSimBaseData().addAllLevel(casino.getCasinoLevel());
        //TODO 初始游客: VisitorQuest 无场景维度配置, 待策划补充配置后在此初始化 guestMap
        log.info("创建新场景 playerId={},casinoId={},statsId={},buildingCount={}", ctx.playerId(), casinoId, statsId,
                casino.getBuildingData() == null ? 0 : casino.getBuildingData().size());
        return casino;
    }

    /**
     * 取某场景 (RegionID) level 最小的经营等级 statsId; 未配置则回退默认值
     */
    private int resolveInitialStatsId(int casinoId) {
        int bestId = SimConstant.Common.DEFAULT_CASINO_STATS_ID;
        int bestLevel = Integer.MAX_VALUE;
        boolean found = false;
        for (CasinoStatsSheetCfg cfg : GameDataManager.getCasinoStatsSheetCfgList()) {
            if (cfg.getRegionID() != casinoId) {
                continue;
            }
            if (cfg.getLevel() < bestLevel) {
                bestLevel = cfg.getLevel();
                bestId = cfg.getId();
                found = true;
            }
        }
        if (!found) {
            log.warn("场景未配置 CasinoStatsSheet, 回退默认 statsId casinoId={},defaultStatsId={}", casinoId, bestId);
        }
        return bestId;
    }

    /**
     * 校验开辟条件: condition 为 (前置场景id -> 需达经营等级); 任一前置场景不达标即失败。
     */
    private boolean checkCondition(SimPlayerContext ctx, CasinoListCfg cfg) {
        Map<Integer, Integer> condition = cfg.getCondition();
        if (condition == null || condition.isEmpty()) {
            return true;
        }
        for (Map.Entry<Integer, Integer> en : condition.entrySet()) {
            int preCasinoId = en.getKey();
            int needLevel = en.getValue();
            int level = casinoOperationLevel(ctx, preCasinoId);
            if (level < needLevel) {
                log.info("开辟新场景条件不满足 playerId={},targetCasinoId={},preCasinoId={},needLevel={},level={}",
                        ctx.playerId(), cfg.getId(), preCasinoId, needLevel, level);
                return false;
            }
        }
        return true;
    }

    /**
     * 取玩家某场景的经营等级 (优先内存当前场景, 否则查 DB); 未拥有返回 -1。
     */
    private int casinoOperationLevel(SimPlayerContext ctx, int casinoId) {
        SimCasinoData casino;
        if (ctx.getCurrentCasino() != null && ctx.getCurrentCasino().getCasinoId() == casinoId) {
            casino = ctx.getCurrentCasino();
        } else {
            casino = simCasinoDao.findOne(ctx.playerId(), casinoId);
        }
        if (casino == null) {
            return -1;
        }
        CasinoStatsSheetCfg statsCfg = configCacheService.getCasinoStatsSheetCfg(casinoId, casino.getCasinoLevel());
        return statsCfg == null ? 0 : statsCfg.getLevel();
    }

    /**
     * 获取知名度
     *
     * @param ctx
     * @return
     */
    public long awareness(SimPlayerContext ctx) {
        SimCasinoData casinoData = ctx.getCurrentCasino();
        if (casinoData == null) {
            return 0;
        }

        if (casinoData.getBuildingData() == null || casinoData.getBuildingData().isEmpty()) {
            return 0;
        }

        //获取运营部的建筑
        BuildingData buildingData = casinoData.getBuildingData().get(SimConstant.Building.ID_OPERATIONS_DEPART);
        if (buildingData == null) {
            return 0;
        }

        //获取配置
        BuildingUpgradeTableCfg cfg = configCacheService.getBuildingUpgradeCfg(buildingData.getId(), buildingData.getLevel());
        if (cfg == null || cfg.getUpgradeOutput() < 1) {
            return 0;
        }

        //获取雇员加成
        Map<BonusType, Integer> bonusesMap = new HashMap<>();
        employeeService.computeTypeBonusFixed(ctx, bonusesMap);
        Integer bouns = bonusesMap.get(BonusType.AWARENESS);
        if (bouns == null) {
            return cfg.getUpgradeOutput();
        }
        long extra = cfg.getUpgradeOutput() * bouns / SimConstant.Common.EMPLOYEE_BONUS_DIVISOR;
        return cfg.getUpgradeOutput() + extra;
    }

    /**
     * 更新 SimCasinoUnlock 信息
     *
     * @param playerId
     * @param casinoId
     * @param level
     */
    public void updateCasinoUnlock(long playerId, int casinoId, int level) {
        SimCasinoUnlock casinoUnlock = getCasinoUnlock(playerId);
        if (casinoUnlock == null) {
            casinoUnlock = new SimCasinoUnlock();
        }
        casinoUnlock.changeUnlockLevel(casinoId, level);
        redisTemplate.opsForHash().put(TABLE_NAME, playerId, casinoUnlock);
    }

    public SimCasinoUnlock getCasinoUnlock(long playerId) {
        return (SimCasinoUnlock) redisTemplate.opsForHash().get(TABLE_NAME, playerId);
    }
}
