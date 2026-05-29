package com.jjg.game.sim.service;

import com.jjg.game.core.constant.Code;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BuildingAreaTableCfg;
import com.jjg.game.sampledata.bean.BuildingUpgradeTableCfg;
import com.jjg.game.sampledata.bean.CasinoStatsSheetCfg;
import com.jjg.game.sim.constant.SimConstant;
import com.jjg.game.sim.data.BuildingData;
import com.jjg.game.sim.data.SimCasinoData;
import com.jjg.game.sim.data.SimPlayerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 建筑服务: 解锁、升级、CD 清除
 *
 * @author 11
 * @date 2026/5/28
 */
@Service
public class SimBuildingService {
    private static final Logger log = LoggerFactory.getLogger(SimBuildingService.class);

    //初始等级 (解锁后)
    private static final int INITIAL_LEVEL = 1;

    @Autowired
    private SimConfigCacheService configCache;

    /**
     * 解锁建筑
     *
     * @return
     */
    public int unlockBuilding(SimPlayerContext ctx, int buildingId) {
        SimCasinoData casino = ctx.getCurrentCasino();
        if (casino == null) {
            log.warn("解锁建筑失败, 当前赌场不存在 playerId={}", ctx.playerId());
            return Code.NOT_FOUND;
        }
        BuildingAreaTableCfg cfg = GameDataManager.getBuildingAreaTableCfg(buildingId);
        if (cfg == null) {
            log.warn("解锁建筑失败, 配置不存在 playerId={},buildingId={}", ctx.playerId(), buildingId);
            return Code.NOT_FOUND;
        }
        if (cfg.getCasinoID() != casino.getCasinoId()) {
            log.warn("解锁建筑失败, 建筑不属于当前赌场 playerId={},buildingId={},casinoId={}", ctx.playerId(), buildingId, casino.getCasinoId());
            return Code.PARAM_ERROR;
        }
        if (casino.findBuilding(buildingId) != null) {
            log.warn("解锁建筑失败, 已解锁 playerId={},buildingId={}", ctx.playerId(), buildingId);
            return Code.PARAM_ERROR;
        }
        //前置建筑必须已解锁
        if (cfg.getUnlockMethod() > 0 && casino.findBuilding(cfg.getUnlockMethod()) == null) {
            log.warn("解锁建筑失败, 前置建筑未解锁 playerId={},buildingId={},prereq={}", ctx.playerId(), buildingId, cfg.getUnlockMethod());
            return Code.PARAM_ERROR;
        }
        //资源足够?
        if (!checkAndConsumeItems(ctx, cfg.getUnlockCost())) {
            log.warn("解锁建筑失败, 资源不足 playerId={},buildingId={},cost={}", ctx.playerId(), buildingId, cfg.getUnlockCost());
            return Code.NOT_ENOUGH;
        }

        BuildingData data = new BuildingData();
        data.setId(buildingId);
        data.setLevel(INITIAL_LEVEL);
        casino.putBuilding(data);
        log.info("解锁建筑成功 playerId={},buildingId={}", ctx.playerId(), buildingId);
        return Code.SUCCESS;
    }

    /**
     * 升级建筑 (启动 CD)
     */
    public int upgradeBuilding(SimPlayerContext ctx, int buildingId) {
        long now = System.currentTimeMillis();
        SimCasinoData casino = ctx.getCurrentCasino();
        if (casino == null) {
            return Code.NOT_FOUND;
        }
        BuildingData data = casino.findBuilding(buildingId);
        if (data == null) {
            log.warn("升级建筑失败, 建筑未解锁 playerId={},buildingId={}", ctx.playerId(), buildingId);
            return Code.NOT_FOUND;
        }
        if (data.getCdEndTime() > now) {
            log.warn("升级建筑失败, 升级中 playerId={},buildingId={},cdEndTime={}", ctx.playerId(), buildingId, data.getCdEndTime());
            return Code.PARAM_ERROR;
        }
        int targetLevel = data.getLevel() + 1;
        BuildingUpgradeTableCfg next = configCache.getBuildingUpgradeCfg(buildingId, targetLevel);
        if (next == null) {
            log.warn("升级建筑失败, 已达上限 playerId={},buildingId={},level={}", ctx.playerId(), buildingId, data.getLevel());
            return Code.PARAM_ERROR;
        }
        //建筑等级 <= 经营等级 (简化: 以赌场 stats level 为经营等级)
        CasinoStatsSheetCfg statsCfg = GameDataManager.getCasinoStatsSheetCfg(casino.getStatsId());
        int operationLevel = statsCfg == null ? 0 : statsCfg.getLevel();
        if (targetLevel > operationLevel + 1) {
            log.warn("升级建筑失败, 经营等级不足 playerId={},buildingId={},targetLevel={},operationLevel={}", ctx.playerId(), buildingId, targetLevel, operationLevel);
            return Code.NOT_ENOUGH;
        }
        if (!checkAndConsumeItems(ctx, next.getUpgradeCost())) {
            log.warn("升级建筑失败, 资源不足 playerId={},buildingId={},cost={}", ctx.playerId(), buildingId, next.getUpgradeCost());
            return Code.NOT_ENOUGH;
        }
        long cdMs = (long) next.getUpgradeCD() * 60_000L;
        data.setCdEndTime(now + cdMs);
        log.info("升级建筑启动 playerId={},buildingId={},targetLevel={},cdMs={}", ctx.playerId(), buildingId, targetLevel, cdMs);
        return Code.SUCCESS;
    }

    /**
     * 完成建筑升级 (客户端在 CD 到点后调用)
     */
    public int completeBuildingUpgrade(SimPlayerContext ctx, int buildingId) {
        SimCasinoData casino = ctx.getCurrentCasino();
        if (casino == null) {
            return Code.NOT_FOUND;
        }
        BuildingData data = casino.findBuilding(buildingId);
        if (data == null) {
            return Code.PARAM_ERROR;
        }
        return completeBuildingUpgrade(casino, data, System.currentTimeMillis());
    }

    /**
     * 完成建筑升级
     */
    public int completeBuildingUpgrade(SimCasinoData casino, BuildingData data, long now) {
        if (!data.isUpgradeReady(now)) {
            return Code.PARAM_ERROR;
        }
        data.setLevel(data.getLevel() + 1);
        data.setCdEndTime(0);
        data.setAdClearCount(0);
        log.info("完成建筑升级 playerId={},buildingId={},newLevel={}", casino.getPlayerId(), data.getId(), data.getLevel());
        return Code.SUCCESS;
    }

    /**
     * 完成建筑升级
     */
    public void completeAllBuildingUpgrade(SimCasinoData casino) {
        if (casino == null || casino.getBuildingData() == null || casino.getBuildingData().isEmpty()) {
            return;
        }

        long now = System.currentTimeMillis();
        casino.getBuildingData().forEach((k, v) -> completeBuildingUpgrade(casino, v, now));
    }

    /**
     * 清除升级 CD (钻石/道具)
     *
     * @param costItems 玩家承诺消耗的道具
     */
    public int clearBuildingCD(SimPlayerContext ctx, int buildingId, Map<Integer, Long> costItems) {
        long now = System.currentTimeMillis();
        SimCasinoData casino = ctx.getCurrentCasino();
        if (casino == null) {
            return Code.NOT_FOUND;
        }
        BuildingData data = casino.findBuilding(buildingId);
        if (data == null || !data.isUpgrading(now)) {
            return Code.PARAM_ERROR;
        }
        if (!checkAndConsumeItems(ctx, costItems)) {
            return Code.NOT_ENOUGH;
        }
        //简化: 直接清完 (TODO: 按道具数量换算清掉的 CD 时间)
        data.setCdEndTime(now);
        log.info("清除建筑升级CD playerId={},buildingId={}", ctx.playerId(), buildingId);
        return Code.SUCCESS;
    }

    /**
     * 当前赌场已解锁的建筑 ID 集合
     */
    public Set<Integer> getUnlockedBuildingIds(SimCasinoData casino) {
        if (casino == null || casino.getBuildingData() == null) {
            return Collections.emptySet();
        }
        return casino.getBuildingData().keySet();
    }

    /**
     * 当前赌场各分类下一个待解锁的建筑 (按 SequenceID 升序)
     */
    public Map<Integer, Integer> findNextUnlockable(SimCasinoData casino) {
        if (casino == null) {
            return Collections.emptyMap();
        }
        Map<Integer, Integer> result = new HashMap<>();
        int[] allTypes = {SimConstant.BuildingType.GAME, SimConstant.BuildingType.REST, SimConstant.BuildingType.MANAGEMENT};
        Set<Integer> unlocked = getUnlockedBuildingIds(casino);
        for (int type : allTypes) {
            Integer next = configCache.getNextUnlockBuilding(casino.getCasinoId(), type, unlocked);
            if (next != null) {
                result.put(type, next);
            }
        }
        return result;
    }

    /**
     * 资源检查 & 扣除 (占位实现)
     * TODO: 接入 PlayerPackService.removeItems / 货币系统
     */
    private boolean checkAndConsumeItems(SimPlayerContext ctx, Map<Integer, Long> items) {
        if (items == null || items.isEmpty()) {
            return true;
        }
        //占位: 假装成功扣除 (实际项目应该走 PlayerPackService)
        log.debug("[stub] 扣除资源 playerId={},items={}", ctx.playerId(), items);
        return true;
    }

    /**
     * 获取该建筑指定等级的产出 (基础值; 不含主管/雇员加成)
     */
    public Map<Integer, Long> getBaseOutput(int buildingId, int level) {
        BuildingUpgradeTableCfg cfg = configCache.getBuildingUpgradeCfg(buildingId, level);
        if (cfg == null || cfg.getUpgradeOutput() == null) {
            return Collections.emptyMap();
        }
        return cfg.getUpgradeOutput();
    }

    /**
     * 获取该建筑指定等级的最大交互人数
     */
    public int getMaxInteractionCount(int buildingId, int level) {
        BuildingUpgradeTableCfg cfg = configCache.getBuildingUpgradeCfg(buildingId, level);
        return cfg == null ? 0 : cfg.getMaxInteractionCount();
    }

    /**
     * 获取该建筑当前累积已解锁的设备列表 (level 1..currentLevel 的 UnlockEquipment 并集)
     */
    public Set<Integer> getUnlockedEquipments(int buildingId, int currentLevel) {
        Set<Integer> set = new HashSet<>();
        for (int lv = 1; lv <= currentLevel; lv++) {
            BuildingUpgradeTableCfg cfg = configCache.getBuildingUpgradeCfg(buildingId, lv);
            if (cfg == null) {
                continue;
            }
            List<Integer> list = cfg.getUnlockEquipment();
            if (list != null) {
                set.addAll(list);
            }
        }
        return set;
    }
}
