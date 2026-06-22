package com.jjg.game.sim.service;

import com.alibaba.fastjson.JSON;
import com.jjg.game.alliance.service.AllianceHelpService;
import com.jjg.game.common.pb.ItemInfo;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.pb.KVInfo;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BuildingAreaTableCfg;
import com.jjg.game.sampledata.bean.BuildingUpgradeTableCfg;
import com.jjg.game.sampledata.bean.CasinoStatsSheetCfg;
import com.jjg.game.sim.constant.*;
import com.jjg.game.sim.data.BuildingData;
import com.jjg.game.sim.data.SimCasinoData;
import com.jjg.game.sim.data.SimOfflineReward;
import com.jjg.game.sim.data.SimPlayerContext;
import com.jjg.game.sim.listener.SimPlayerTickListener;
import com.jjg.game.sim.pb.SimPbConverter;
import com.jjg.game.sim.pb.res.*;
import com.jjg.game.sim.pb.struct.OfflineReward;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 建筑服务: 解锁、升级、CD 清除
 *
 * @author 11
 * @date 2026/5/28
 */
@Service
public class SimBuildingService implements SimPlayerTickListener {
    private static final Logger log = LoggerFactory.getLogger(SimBuildingService.class);

    //初始等级 (解锁后)
    private static final int INITIAL_LEVEL = 1;

    @Autowired
    private SimConfigCacheService configCache;
    @Autowired
    private SimEmployeeService employeeService;
    @Autowired
    private SimPackService simPackService;
    @Autowired
    private AllianceHelpService allianceHelpService;

    @Override
    public void onTick(SimPlayerContext ctx, long now) {
        output(ctx, now);
    }

    @Override
    public int order() {
        return SimPlayerTickListener.super.order();
    }

    /**
     * 获取建筑信息
     *
     * @param ctx
     * @param buildingId
     */
    public void onBuildingInfo(SimPlayerContext ctx, int buildingId) {
        ResBuildingInfo res = new ResBuildingInfo(Code.SUCCESS);
        try {
            Map<Integer, BuildingData> buildingDataMap = ctx.getCurrentCasino().getBuildingData();
            if (buildingDataMap == null || buildingDataMap.isEmpty()) {
                log.warn("获取建筑信息失败, 建筑列表为空 playerId={}", ctx.playerId());
                res.code = Code.NOT_FOUND;
                ctx.send(res);
                return;
            }

            BuildingData buildingData = buildingDataMap.get(buildingId);
            if (buildingData == null) {
                log.warn("获取建筑信息失败, 该建筑不存在 playerId={},buildingId={}", ctx.playerId(), buildingId);
                res.code = Code.NOT_FOUND;
                ctx.send(res);
                return;
            }

            applyAllianceSpeedup(ctx.playerId(), buildingData, System.currentTimeMillis());
            res.buildingInfo = SimPbConverter.toBuildingInfo(buildingData);
            //建筑的基础产出，不包含加成
            Map<BuildingOutputType, Long> base = getBaseOutput(buildingData.getId(), buildingData.getLevel());
            //普通雇员加成
            res.employeeBonus = normalEmployeeBonus(ctx, base, buildingData);
            //主管加成
            res.manageEmployeeBonus = manageEmployeeBonus(ctx, base, buildingData);
            //观看广告次数限制
            res.watchAdLimit = GameDataManager.getGlobalConfigCfg(SimConstant.Global.ID_WATCH_AD_LIMIT).getIntValue();
            log.info("返回建筑信息 playerId={},res={}", ctx.playerId(), JSON.toJSONString(res));
        } catch (Exception e) {
            log.error("", e);
        }
        ctx.send(res);
    }

    /**
     * 领取离线收益
     */
    public void onClaimOfflineReward(SimPlayerContext ctx, boolean watchAd) {
        ResClaimOfflineReward res = new ResClaimOfflineReward(Code.SUCCESS);
        try {
            SimOfflineReward reward = ctx.getPendingOffline();
            res.code = claimOfflineReward(ctx, watchAd);
            if (res.code == Code.SUCCESS && reward != null) {
                res.watchAd = watchAd;
                double multiplier = (reward.getAdMultiplier() == null || reward.getAdMultiplier().isEmpty()) ? 1.0 : Double.parseDouble(reward.getAdMultiplier());
                Map<BuildingOutputType, Long> finalReward = new HashMap<>(reward.getBaseReward().size());
                reward.getBaseReward().forEach((k, v) -> finalReward.put(k, (long) Math.floor(v * multiplier)));
//                res.rewards = ItemUtils.buildItemInfo(finalReward);

                res.rewards = new ArrayList<>();
                for (Map.Entry<BuildingOutputType, Long> en : finalReward.entrySet()) {
                    KVInfo kvInfo = new KVInfo();
                    kvInfo.key = en.getKey().getCode();
                    kvInfo.value = en.getValue().intValue();
                    res.rewards.add(kvInfo);
                }
            }
        } catch (Exception e) {
            log.error("", e);
            res.code = Code.EXCEPTION;
        }
        ctx.send(res);
    }

    /**
     * 解锁建筑
     */
    public void onUnlockBuilding(SimPlayerContext ctx, int buildingId) {
        ResUnlockBuilding res = new ResUnlockBuilding(Code.SUCCESS);
        res.id = buildingId;
        try {
            SimCasinoData casino = ctx.getCurrentCasino();
            if (casino == null) {
                log.warn("解锁建筑失败, 当前场景不存在 playerId={}", ctx.playerId());
                res.code = Code.NOT_FOUND;
                ctx.send(res);
                return;
            }
            BuildingAreaTableCfg cfg = GameDataManager.getBuildingAreaTableCfg(buildingId);
            if (cfg == null) {
                log.warn("解锁建筑失败, 配置不存在 playerId={},buildingId={}", ctx.playerId(), buildingId);
                res.code = Code.NOT_FOUND;
                ctx.send(res);
                return;
            }
            if (cfg.getRegionID() != casino.getCasinoId()) {
                log.warn("解锁建筑失败, 建筑不属于当前场景 playerId={},buildingId={},casinoId={}", ctx.playerId(), buildingId, casino.getCasinoId());
                res.code = Code.PARAM_ERROR;
                ctx.send(res);
                return;
            }
            if (casino.findBuilding(buildingId) != null) {
                log.warn("解锁建筑失败, 已解锁 playerId={},buildingId={}", ctx.playerId(), buildingId);
                res.code = Code.PARAM_ERROR;
                ctx.send(res);
                return;
            }
            //todo 前置建筑必须已解锁
//            if (cfg.getUnlockMethod() > 0 && casino.findBuilding(cfg.getUnlockMethod()) == null) {
//                log.warn("解锁建筑失败, 前置建筑未解锁 playerId={},buildingId={},prereq={}", ctx.playerId(), buildingId, cfg.getUnlockMethod());
//                res.code = Code.PARAM_ERROR;
//                ctx.send(res);
//                return;
//            }
            //资源足够?
            boolean remove = simPackService.removeItems(ctx, cfg.getUnlockCost(), AddType.SIM_BUILDING_UPGRADE, null);
            if (!remove) {
                log.warn("解锁建筑失败, 资源不足 playerId={},buildingId={},cost={}", ctx.playerId(), buildingId, cfg.getUnlockCost());
                res.code = Code.NOT_ENOUGH;
                ctx.send(res);
                return;
            }

            BuildingData data = new BuildingData();
            data.setId(buildingId);
            data.setLevel(INITIAL_LEVEL);
            casino.putBuilding(data);
            log.info("解锁建筑成功 playerId={},buildingId={}", ctx.playerId(), buildingId);
        } catch (Exception e) {
            log.error("", e);
            res.code = Code.EXCEPTION;
        }
        ctx.send(res);
    }


    /**
     * 升级建筑 (启动 CD)
     */
    public void onUpgradeBuilding(SimPlayerContext ctx, int buildingId) {
        ResUpgradeBuilding res = new ResUpgradeBuilding(Code.SUCCESS);
        try {
            long now = System.currentTimeMillis();
            SimCasinoData casino = ctx.getCurrentCasino();
            if (casino == null) {
                res.code = Code.NOT_FOUND;
                ctx.send(res);
                return;
            }
            BuildingData data = casino.findBuilding(buildingId);
            if (data == null) {
                log.warn("升级建筑失败, 建筑未解锁 playerId={},buildingId={}", ctx.playerId(), buildingId);
                res.code = Code.NOT_FOUND;
                ctx.send(res);
                return;
            }
            if (data.getCdEndTime() > now) {
                log.warn("升级建筑失败, 升级中 playerId={},buildingId={},cdEndTime={}", ctx.playerId(), buildingId, data.getCdEndTime());
                res.code = Code.PARAM_ERROR;
                ctx.send(res);
                return;
            }

            //获取本级的配置
            BuildingUpgradeTableCfg currentCfg = configCache.getBuildingUpgradeCfg(buildingId, data.getLevel());
            if (currentCfg == null) {
                log.warn("升级建筑失败, 未找到获取配置表 playerId={},buildingId={},level={}", ctx.playerId(), buildingId, data.getLevel());
                res.code = Code.PARAM_ERROR;
                ctx.send(res);
                return;
            }

            if (currentCfg.getUpgradeCost() == null || currentCfg.getUpgradeCost().isEmpty()) {
                log.warn("升级建筑失败, 没有配置升级消耗道具 playerId={},buildingId={},level={}", ctx.playerId(), buildingId, data.getLevel());
                res.code = Code.PARAM_ERROR;
                ctx.send(res);
                return;
            }

            //先检查是不是添加进度条
            if (currentCfg.getCostPerLevel() != null && !currentCfg.getCostPerLevel().isEmpty()) {
                //添加进度条
                if (data.getProgress() < currentCfg.getCostPerLevel().size()) {

                    List<Integer> list = currentCfg.getCostPerLevel().get(data.getProgress());
                    boolean remove = simPackService.removeItem(ctx, list.get(0), list.get(1), AddType.SIM_BUILDING_UPGRADE);
                    if (!remove) {
                        log.warn("建筑添加进度条失败, 未找到获取配置表 playerId={},buildingId={},level={}", ctx.playerId(), buildingId, data.getLevel());
                        res.code = Code.PARAM_ERROR;
                        ctx.send(res);
                        return;
                    }

                    data.setProgress(data.getProgress() + 1);
                    res.buildingInfo = SimPbConverter.toBuildingInfo(data);
                    ctx.send(res);

                    log.info("建筑增加进度条 playerId={},buildingInfo={}", ctx.playerId(), JSON.toJSONString(res.buildingInfo));
                    return;
                }
            }

            int buildingNextLevel = data.getLevel() + 1;
            BuildingUpgradeTableCfg next = configCache.getBuildingUpgradeCfg(buildingId, buildingNextLevel);
            if (next == null) {
                log.warn("升级建筑失败, 已达上限 playerId={},buildingId={},level={}", ctx.playerId(), buildingId, data.getLevel());
                res.code = Code.PARAM_ERROR;
                ctx.send(res);
                return;
            }
            //建筑等级 <= 经营等级 (简化: 以场景 stats level 为经营等级)
            CasinoStatsSheetCfg statsCfg = configCache.getCasinoStatsSheetCfg(casino.getCasinoId(), casino.getCasinoLevel());
            if (buildingNextLevel > statsCfg.getLevel()) {
                log.warn("升级建筑失败, 经营等级不足 playerId={},buildingId={},buildingNextLevel={},casinoLevel={}", ctx.playerId(), buildingId, buildingNextLevel, statsCfg.getLevel());
                res.code = Code.NOT_ENOUGH;
                ctx.send(res);
                return;
            }
            boolean remove = simPackService.removeItems(ctx, currentCfg.getUpgradeCost(), AddType.SIM_BUILDING_UPGRADE, null);
            if (!remove) {
                log.warn("升级建筑失败, 资源不足 playerId={},buildingId={},cost={}", ctx.playerId(), buildingId, currentCfg.getUpgradeCost());
                res.code = Code.NOT_ENOUGH;
                ctx.send(res);
                return;
            }
            long cdMs = (long) currentCfg.getUpgradeCD() * 60_000L;
            allianceHelpService.consumeSpeedupSeconds(ctx.playerId(), buildingId);
            data.setCdEndTime(now + cdMs);
            res.buildingInfo = SimPbConverter.toBuildingInfo(data);
            log.info("升级建筑启动 playerId={},buildingInfo={}", ctx.playerId(), JSON.toJSONString(res.buildingInfo));
        } catch (Exception e) {
            log.error("", e);
            res.code = Code.EXCEPTION;
        }
        ctx.send(res);
    }

    /**
     * 完成建筑升级
     */
    public void onCompleteBuildingUpgrade(SimPlayerContext ctx, int buildingId) {
        ResCompleteBuildingUpgrade res = new ResCompleteBuildingUpgrade(Code.SUCCESS);
        res.id = buildingId;
        try {
            SimCasinoData casino = ctx.getCurrentCasino();
            if (casino == null) {
                res.code = Code.NOT_FOUND;
                ctx.send(res);
                log.warn("完成建筑升级失败, 未找到SimCasinoData数据 playerId={},buildingId={}", ctx.playerId(), buildingId);
                return;
            }
            BuildingData data = casino.findBuilding(buildingId);
            if (data == null) {
                res.code = Code.PARAM_ERROR;
                ctx.send(res);
                log.warn("完成建筑升级失败, 未找到BuildingData数据 playerId={},buildingId={}", ctx.playerId(), buildingId);
                return;
            }

            long now = System.currentTimeMillis();
            applyAllianceSpeedup(ctx.playerId(), data, now);
            res.code = completeBuildingUpgrade(casino, data, now);
            if (res.code == Code.SUCCESS) {
                res.level = data.getLevel();
            }
        } catch (Exception e) {
            log.error("", e);
            res.code = Code.EXCEPTION;
        }
        ctx.send(res);
    }

    /**
     * 清除建筑升级 CD
     */
    public void onClearBuildingCD(SimPlayerContext ctx, int buildingId, int costCount, boolean watchAd) {
        ResClearBuildingCD res = new ResClearBuildingCD(Code.SUCCESS);
        try {
            SimCasinoData casino = ctx.getCurrentCasino();
            if (casino == null) {
                res.code = Code.NOT_FOUND;
                ctx.send(res);
                return;
            }
            BuildingData data = casino.findBuilding(buildingId);
            if (data == null) {
                res.code = Code.PARAM_ERROR;
                ctx.send(res);
                log.warn("清除建筑升级 CD失败，当前建筑没有处于cd状态 playerId={},buildingId={}", ctx.playerId(), buildingId);
                return;
            }

            long now = System.currentTimeMillis();
            applyAllianceSpeedup(ctx.playerId(), data, now);
            if (!data.isUpgrading(now)) {
                if (data.isUpgradeReady(now)) {
                    completeBuildingUpgrade(casino, data, now);
                    res.buildingInfo = SimPbConverter.toBuildingInfo(data);
                    ctx.send(res);
                    return;
                }
                res.code = Code.PARAM_ERROR;
                ctx.send(res);
                log.warn("clear building cd failed, building is not upgrading playerId={},buildingId={}", ctx.playerId(), buildingId);
                return;
            }

            if (watchAd) {
                //观看广告次数限制
                int countLimit = GameDataManager.getGlobalConfigCfg(SimConstant.Global.ID_WATCH_AD_LIMIT).getIntValue();
                if (countLimit > 0 && data.getAdClearCount() >= countLimit) {
                    res.code = Code.FORBID;
                    ctx.send(res);
                    log.warn("清除建筑升级 CD失败，玩家观看广告清除cd次数达到限制 playerId={},buildingId={},hasWatchCount={},countLimit={}", ctx.playerId(), buildingId, data.getAdClearCount(), countLimit);
                    return;
                }
                //每次观看广告清除的时间
                int cfgMin = GameDataManager.getGlobalConfigCfg(SimConstant.Global.ID_WATCH_AD_CLEAR_TIME).getIntValue();
                long reduceMs = (long) cfgMin * TimeHelper.ONE_MINUTE_OF_MILLIS;
                data.setCdEndTime(data.getCdEndTime() - reduceMs);
                data.setAdClearCount(data.getAdClearCount() + 1);
                //经营信息: 观看广告数 +1
                casino.incWatchAdCount();
            } else {
                if (costCount < 1) {
                    res.code = Code.NOT_ENOUGH;
                    ctx.send(res);
                    log.warn("道具清除建筑升级 CD失败，costCount不能小于1 playerId={},buildingId={},costCount={}", ctx.playerId(), buildingId, costCount);
                    return;
                }
                boolean remove = simPackService.removeItem(ctx, SimConstant.Item.ID_CLEAR_CD, costCount, AddType.SIM_BUILDING_UPGRADE);
                if (!remove) {
                    res.code = Code.NOT_ENOUGH;
                    ctx.send(res);
                    log.warn("道具清除建筑升级 CD失败，道具不足 playerId={},buildingId={},costCount={}", ctx.playerId(), buildingId, costCount);
                    return;
                }
                //每消耗 1 个道具, CD 减少 1 分钟
                long reduceMs = (long) costCount * TimeHelper.ONE_MINUTE_OF_MILLIS;
                data.setCdEndTime(data.getCdEndTime() - reduceMs);
            }

            res.buildingInfo = SimPbConverter.toBuildingInfo(data);
            log.info("清除建筑升级CD playerId={},buildingId={},watchAd={},costCount={},level={},cdEndTime={}", ctx.playerId(), buildingId, watchAd, costCount, data.getLevel(), data.getCdEndTime());
        } catch (Exception e) {
            log.error("", e);
            res.code = Code.EXCEPTION;
        }
        ctx.send(res);
    }


    /**
     * 立即结算当前场景已积累的整分钟在线产出 (切换场景前调用, 避免余量丢失)
     */
    public void settleOnlineOutput(SimPlayerContext ctx) {
        output(ctx, System.currentTimeMillis());
    }

    /**
     * 在线产出: 每分钟自动结算一次, 保留不足 1 分钟的余量时间
     */
    private void output(SimPlayerContext ctx, long now) {
        try {
            //新手引导未完成不产出 (与游客生成一致)
            if (!ctx.getSimBaseData().isGuide()) {
                return;
            }
            SimCasinoData casino = ctx.getCurrentCasino();
            if (casino == null) {
                return;
            }
            if (casino.getLastOutputTime() == 0) {
                casino.setLastOutputTime(now);
                return;
            }
            long elapsed = now - casino.getLastOutputTime();
            long fullMinutes = elapsed / TimeHelper.ONE_MINUTE_OF_MILLIS;
            if (fullMinutes <= 0) {
                return;
            }
            Map<BuildingOutputType, Long> perMinute = computePerMinuteOutput(ctx, casino);
            if (!perMinute.isEmpty()) {
                Map<BuildingOutputType, Long> total = multiply(perMinute, fullMinutes);
                simPackService.addItem(ctx, total, AddType.SIM_BUILD_MINUTE_REWARDS, null, false);
                //经营信息: 累加每分钟自产金币收益
                casino.addBusinessIncome(total.getOrDefault(BuildingOutputType.GOLD, 0L));
            }
            //仅推进已结算的整分钟, 保留余量
            casino.setLastOutputTime(casino.getLastOutputTime() + fullMinutes * TimeHelper.ONE_MINUTE_OF_MILLIS);
        } catch (Exception e) {
            log.error("在线产出结算异常 playerId={}", ctx.playerId(), e);
        }
    }

    /**
     * 计算当前场景所有游戏区/休息区建筑每分钟产出 (含雇员加成) 之和
     *
     * @return itemId -> 每分钟数量 (金币/能量混合)
     */
    public Map<BuildingOutputType, Long> computePerMinuteOutput(SimPlayerContext ctx, SimCasinoData casino) {
        if (casino.getBuildingData() == null || casino.getBuildingData().isEmpty()) {
            return Collections.emptyMap();
        }

        //获取雇员加成
        Map<BonusType, Integer> bonusesMap = new HashMap<>();
        employeeService.computeTypeBonusFixed(ctx, bonusesMap);

        Map<BuildingOutputType, Long> total = new HashMap<>();
        for (BuildingData building : casino.getBuildingData().values()) {
            Map<BuildingOutputType, Long> actual = buildingActualPerMinute(ctx, building, bonusesMap);
            actual.forEach((buildingOutputType, count) -> total.merge(buildingOutputType, count, Long::sum));
        }
        return total;
    }

    /**
     * 计算单个建筑每分钟实际产出 (含普通雇员 + 主管加成); 非每分钟产出类型返回空。
     *
     * @param bonusesMap 已汇总的普通雇员加成 (按类型)
     */
    private Map<BuildingOutputType, Long> buildingActualPerMinute(SimPlayerContext ctx, BuildingData building, Map<BonusType, Integer> bonusesMap) {
        //获取建筑的基础产出，不包含加成
        Map<BuildingOutputType, Long> base = getBaseOutput(building.getId(), building.getLevel());
        if (base.isEmpty()) {
            return Collections.emptyMap();
        }

        BuildingAreaTableCfg areaCfg = GameDataManager.getBuildingAreaTableCfg(building.getId());
        if (areaCfg == null) {
            return Collections.emptyMap();
        }

        BuildingType buildingType = BuildingType.fromCode(areaCfg.getType());
        if (buildingType == null) {
            return Collections.emptyMap();
        }
        //获取加成
        BonusType bonusType = BonusType.fromBuildingType(buildingType);
        //检查是不是每分钟产出的建筑类型
        if (bonusType != null && !bonusType.isMin()) {
            return Collections.emptyMap();
        }
        return applyBuildingBonus(ctx, base, bonusType, areaCfg.getEmployeeProfile(), bonusesMap);
    }

    /**
     * 为单个建筑的基础产出叠加 (普通雇员 + 主管) 加成; 不做每分钟产出类型过滤。
     */
    private Map<BuildingOutputType, Long> applyBuildingBonus(SimPlayerContext ctx, Map<BuildingOutputType, Long> base, BonusType bonusType, int employeeProfile, Map<BonusType, Integer> bonusesMap) {
        //主管加成
        Map<BonusType, Integer> manageBonusesMap = employeeService.manageEmployeeBonus(ctx, employeeProfile);
        //合并: 普通雇员 + 主管加成相加
        Map<BonusType, Integer> tmpMap;
        if (manageBonusesMap != null && !manageBonusesMap.isEmpty()) {
            tmpMap = new HashMap<>(bonusesMap);
            for (Map.Entry<BonusType, Integer> en : manageBonusesMap.entrySet()) {
                tmpMap.merge(en.getKey(), en.getValue(), Integer::sum);
            }
        } else {
            tmpMap = bonusesMap;
        }

        int bonus = 0;
        if (bonusType != null) {
            bonus = tmpMap.getOrDefault(bonusType, 0);
        }
        return applyBonus(base, bonus);
    }

    /**
     * 经营信息-按房间分类统计每分钟产量:
     * 能量房间(休息区 POWER) / SLOT房间 / 扑克房间 / 捕鱼房间(游戏区 GOLD)。
     * <p>
     * 用于看板展示, 取各建筑配置的每分钟产出 (含加成), 不受在线产出结算的 isMin 门控影响。
     *
     * @return 房间统计KEY ({@link com.jjg.game.sim.constant.SimStatKey.Operation}) -> 每分钟产量
     */
    public Map<Integer, Long> computeRoomOutputs(SimPlayerContext ctx, SimCasinoData casino) {
        Map<Integer, Long> result = new HashMap<>();
        if (casino.getBuildingData() == null || casino.getBuildingData().isEmpty()) {
            return result;
        }
        Map<BonusType, Integer> bonusesMap = new HashMap<>();
        employeeService.computeTypeBonusFixed(ctx, bonusesMap);

        for (BuildingData building : casino.getBuildingData().values()) {
            BuildingAreaTableCfg areaCfg = GameDataManager.getBuildingAreaTableCfg(building.getId());
            if (areaCfg == null) {
                continue;
            }
            BuildingType buildingType = BuildingType.fromCode(areaCfg.getType());
            if (buildingType != BuildingType.REST && buildingType != BuildingType.GAME) {
                continue;
            }
            Map<BuildingOutputType, Long> base = getBaseOutput(building.getId(), building.getLevel());
            if (base.isEmpty()) {
                continue;
            }
            Map<BuildingOutputType, Long> actual = applyBuildingBonus(ctx, base, BonusType.fromBuildingType(buildingType), areaCfg.getEmployeeProfile(), bonusesMap);
            if (buildingType == BuildingType.REST) {
                //能量房间: 休息区 POWER 产量
                result.merge(SimStatKey.Operation.ENERGY_ROOM, actual.getOrDefault(BuildingOutputType.POWER, 0L), Long::sum);
            } else {
                //游戏区 GOLD 产量; TODO 待建筑细分(SLOT/扑克/捕鱼)配置后区分, 暂统一计入 SLOT房间
                result.merge(SimStatKey.Operation.SLOT_ROOM, actual.getOrDefault(BuildingOutputType.GOLD, 0L), Long::sum);
            }
        }
        return result;
    }

    /**
     * 经营信息-当前可容纳游客人数: 已解锁的游戏区+休息区建筑当前等级的最大交互数量之和。
     */
    public int computeCurrentCapacity(SimCasinoData casino) {
        if (casino.getBuildingData() == null || casino.getBuildingData().isEmpty()) {
            return 0;
        }
        int sum = 0;
        for (BuildingData building : casino.getBuildingData().values()) {
            BuildingAreaTableCfg areaCfg = GameDataManager.getBuildingAreaTableCfg(building.getId());
            if (areaCfg == null) {
                continue;
            }
            BuildingType buildingType = BuildingType.fromCode(areaCfg.getType());
            if (buildingType != BuildingType.GAME && buildingType != BuildingType.REST) {
                continue;
            }
            BuildingUpgradeTableCfg cfg = configCache.getBuildingUpgradeCfg(building.getId(), building.getLevel());
            if (cfg == null) {
                continue;
            }
            sum += cfg.getMaxInteractionCount();
        }
        return sum;
    }

    /**
     * 经营信息-职能部门 (管理区) 当前等级的属性值 (含雇员加成):
     * 接待区(服务能力) / 营销部(曝光度) / 运营部(知名度)。
     *
     * @param outputType 部门对应的产出类型 (按建筑 typeValue 匹配)
     * @param bonusType  对应的雇员加成类型 (无则传 null, 仅返回基础值)
     * @return 部门属性值; 未解锁对应建筑返回 0
     */
    public long computeDeptValue(SimPlayerContext ctx, SimCasinoData casino, BuildingOutputType outputType, BonusType bonusType) {
        BuildingData dept = findDeptBuilding(casino, outputType);
        if (dept == null) {
            return 0;
        }
        BuildingUpgradeTableCfg cfg = configCache.getBuildingUpgradeCfg(dept.getId(), dept.getLevel());
        if (cfg == null || cfg.getUpgradeOutput() < 1) {
            return 0;
        }
        long base = cfg.getUpgradeOutput();
        if (bonusType == null) {
            return base;
        }
        Map<BonusType, Integer> bonusesMap = new HashMap<>();
        employeeService.computeTypeBonusFixed(ctx, bonusesMap);
        Integer bonus = bonusesMap.get(bonusType);
        if (bonus == null || bonus < 1) {
            return base;
        }
        return base + base * bonus / SimConstant.Common.EMPLOYEE_BONUS_DIVISOR;
    }

    /**
     * 查找已解锁的管理区建筑中 typeValue 匹配指定产出类型的部门建筑
     */
    private BuildingData findDeptBuilding(SimCasinoData casino, BuildingOutputType outputType) {
        if (casino.getBuildingData() == null || casino.getBuildingData().isEmpty()) {
            return null;
        }
        for (BuildingData building : casino.getBuildingData().values()) {
            BuildingAreaTableCfg areaCfg = GameDataManager.getBuildingAreaTableCfg(building.getId());
            if (areaCfg == null) {
                continue;
            }
            if (BuildingType.fromCode(areaCfg.getType()) != BuildingType.MANAGE) {
                continue;
            }
            if (BuildingOutputType.fromCode(areaCfg.getTypeValue()) == outputType) {
                return building;
            }
        }
        return null;
    }

    /**
     * 普通雇员加成
     *
     * @param ctx
     * @param base
     * @param buildingData
     * @return KVInfo.key=itemId  KVInfo.value=bouns
     */
    public List<KVInfo> normalEmployeeBonus(SimPlayerContext ctx, Map<BuildingOutputType, Long> base, BuildingData buildingData) {
        if (base == null || base.isEmpty()) {
            return Collections.emptyList();
        }
        BuildingAreaTableCfg areaCfg = GameDataManager.getBuildingAreaTableCfg(buildingData.getId());
        if (areaCfg == null) {
            return Collections.emptyList();
        }
        BuildingType buildingType = BuildingType.fromCode(areaCfg.getType());
        if (buildingType == null) {
            return Collections.emptyList();
        }
        BonusType bonusType = BonusType.fromBuildingType(buildingType);
        if (bonusType == null) {
            return Collections.emptyList();
        }

        //所有已解锁雇员的等级加成 (按类型汇总)
        Map<BonusType, Integer> bonusesMap = new HashMap<>();
        employeeService.computeTypeBonusFixed(ctx, bonusesMap);
        int bonus = bonusesMap.getOrDefault(bonusType, 0);
        return buildBonusList(base, bonus);
    }

    /**
     * 主管加成
     *
     * @param ctx
     * @param base
     * @param buildingData
     * @return KVInfo.key=itemId  KVInfo.value=bouns
     */
    public List<KVInfo> manageEmployeeBonus(SimPlayerContext ctx, Map<BuildingOutputType, Long> base, BuildingData buildingData) {
        if (base == null || base.isEmpty()) {
            return Collections.emptyList();
        }
        BuildingAreaTableCfg areaCfg = GameDataManager.getBuildingAreaTableCfg(buildingData.getId());
        if (areaCfg == null) {
            return Collections.emptyList();
        }
        BuildingType buildingType = BuildingType.fromCode(areaCfg.getType());
        if (buildingType == null) {
            return Collections.emptyList();
        }
        BonusType bonusType = BonusType.fromBuildingType(buildingType);
        if (bonusType == null) {
            return Collections.emptyList();
        }

        //主管加成
        Map<BonusType, Integer> withSupervisor = employeeService.manageEmployeeBonus(ctx, areaCfg.getEmployeeProfile());

        int managerBonus = withSupervisor.getOrDefault(bonusType, 0);
        return buildBonusList(base, managerBonus);
    }

    /**
     * 按加成固定值计算每项基础产出的额外加成量: base * bonus / 1000 (向下取整)
     *
     * @param base  基础产出 itemId -> 数量
     * @param bonus 加成固定值 (/ 1000 = 加成百分比)
     * @return KVInfo.key=itemId  KVInfo.value=额外加成产出
     */
    private List<KVInfo> buildBonusList(Map<BuildingOutputType, Long> base, int bonus) {
        if (bonus < 1) {
            return Collections.emptyList();
        }
        List<KVInfo> list = new ArrayList<>(base.size());
        for (Map.Entry<BuildingOutputType, Long> en : base.entrySet()) {
            long extra = en.getValue() * bonus / SimConstant.Common.EMPLOYEE_BONUS_DIVISOR;
            if (extra <= 0) {
                continue;
            }
            KVInfo kv = new KVInfo();
            kv.key = en.getKey().getCode();
            kv.value = (int) extra;
            list.add(kv);
        }
        return list;
    }

    /**
     * 建筑按雇员加成固定值计算实际产出: base + base * bonusFixed / 1000 (向下取整)
     *
     * @param baseOutputMap 基础产出 itemId -> 数量
     * @param bonus         雇员加成之和 (固定值 / 1000 = 加成百分比)
     * @return 物品id -> 数量
     */
    public Map<BuildingOutputType, Long> applyBonus(Map<BuildingOutputType, Long> baseOutputMap, int bonus) {
        if (baseOutputMap == null || baseOutputMap.isEmpty() || bonus < 1) {
            return baseOutputMap;
        }
        HashMap<BuildingOutputType, Long> result = new HashMap<>(baseOutputMap.size());
        for (Map.Entry<BuildingOutputType, Long> en : baseOutputMap.entrySet()) {
            long extra = en.getValue() * bonus / SimConstant.Common.EMPLOYEE_BONUS_DIVISOR;
            result.put(en.getKey(), extra + en.getValue());
        }
        return result;
    }

    /**
     * 离线收益结算: 计算并存入待领取快照
     */
    public OfflineReward settleOfflineReward(SimPlayerContext ctx) {
        SimCasinoData casino = ctx.getCurrentCasino();
        if (casino == null) {
            return null;
        }
        long lastOfflineTime = ctx.getSimBaseData().getLastOfflineTime();
        long now = System.currentTimeMillis();
        //重置在线产出结算游标, 避免离线时段被在线 tick 重复计算
        casino.setLastOutputTime(now);
        if (lastOfflineTime <= 0) {
            return null;
        }
        SimOfflineReward reward = computeOfflineReward(ctx, casino, now - lastOfflineTime);
        if (reward == null) {
            return null;
        }
        ctx.setPendingOffline(reward);
        log.info("离线收益结算 playerId={},effectiveMinutes={},capMinutes={},reward={}", ctx.playerId(), reward.getEffectiveMinutes(), reward.getCapMinutes(), reward.getBaseReward());
        return buildOfflineRewardPb(reward);
    }

    /**
     * 由离线收益快照构建下发结构 (无快照返回 null)
     */
    public OfflineReward buildOfflineRewardPb(SimOfflineReward reward) {
        if (reward == null) {
            return null;
        }
        OfflineReward rewards = new OfflineReward();
        if (reward.getBaseReward() != null && !reward.getBaseReward().isEmpty()) {
            rewards.rewards = new ArrayList<>();
            for (Map.Entry<BuildingOutputType, Long> en : reward.getBaseReward().entrySet()) {
                ItemInfo itemInfo = new ItemInfo();
                itemInfo.itemId = en.getKey().getCode();
                itemInfo.count = en.getValue();
                rewards.rewards.add(itemInfo);
            }
        }
        rewards.offlineMinutes = reward.getEffectiveMinutes();
        rewards.capMinutes = reward.getCapMinutes();
        rewards.adMultiplier = reward.getAdMultiplier();
        return rewards;
    }

    /**
     * 计算离线收益 (上线时调用; 不入账, 仅生成待领取快照)
     *
     * @param offlineMs 离线总时长(ms)
     * @return 待领取离线收益; 不足 1 分钟或无产出返回 null
     */
    public SimOfflineReward computeOfflineReward(SimPlayerContext ctx, SimCasinoData casino, long offlineMs) {
        long offlineMinutes = offlineMs / TimeHelper.ONE_MINUTE_OF_MILLIS;
        if (offlineMinutes < 1) {
            return null;
        }
        //离线上限时长 (按当前经营等级)
        CasinoStatsSheetCfg statsCfg = configCache.getCasinoStatsSheetCfg(casino.getCasinoId(), casino.getCasinoLevel());
        int capMinutes = statsCfg == null ? 0 : statsCfg.getOfflineDuration();
        int effectiveMinutes = capMinutes > 0 ? (int) Math.min(offlineMinutes, capMinutes) : (int) offlineMinutes;
        if (effectiveMinutes <= 0) {
            return null;
        }
        Map<BuildingOutputType, Long> perMinute = computePerMinuteOutput(ctx, casino);
        if (perMinute.isEmpty()) {
            return null;
        }
        Map<BuildingOutputType, Long> baseReward = multiply(perMinute, effectiveMinutes);
        if (baseReward.isEmpty()) {
            return null;
        }
        String adMultiplier = configCache.pickAdMultiplier();
        return new SimOfflineReward(baseReward, effectiveMinutes, capMinutes, adMultiplier);
    }

    /**
     * 领取离线收益 (1倍直接领取 / 看广告领取广告倍数), 入账并清空待领取快照
     *
     * @param watchAd 是否看广告 (true 走广告倍数)
     * @return Code; 无待领取返回 PARAM_ERROR
     */
    public int claimOfflineReward(SimPlayerContext ctx, boolean watchAd) {
        SimOfflineReward reward = ctx.getPendingOffline();
        if (reward == null) {
            log.warn("领取离线收益失败, 无待领取收益 playerId={}", ctx.playerId());
            return Code.PARAM_ERROR;
        }
        SimCasinoData casino = ctx.getCurrentCasino();
        if (casino == null) {
            return Code.NOT_FOUND;
        }

        double multiplier = 1.0;
        if (watchAd && reward.getAdMultiplier() != null && !reward.getAdMultiplier().isEmpty()) {
            multiplier = Double.parseDouble(reward.getAdMultiplier());
        }
        Map<BuildingOutputType, Long> finalReward = scale(reward.getBaseReward(), multiplier);
        simPackService.addItem(ctx, finalReward, AddType.SIM_BUILD_OFFLINE_REWARDS, null, false);
        //经营信息: 离线产出金币计入经营收益; 看广告领取计入观看广告数
        casino.addBusinessIncome(finalReward.getOrDefault(BuildingOutputType.GOLD, 0L));
        if (watchAd) {
            casino.incWatchAdCount();
        }
        //领取后重置
        ctx.setPendingOffline(null);
        log.info("领取离线收益 playerId={},watchAd={},multiplier={},reward={}", ctx.playerId(), watchAd, multiplier, finalReward);
        return Code.SUCCESS;
    }

    private Map<BuildingOutputType, Long> multiply(Map<BuildingOutputType, Long> src, long factor) {
        Map<BuildingOutputType, Long> result = new HashMap<>(src.size());
        src.forEach((k, v) -> result.put(k, v * factor));
        return result;
    }

    /**
     * 按倍数放大 (小数倍数向下取整, 文档约定)
     */
    private Map<BuildingOutputType, Long> scale(Map<BuildingOutputType, Long> src, double multiplier) {
        Map<BuildingOutputType, Long> result = new HashMap<>(src.size());
        src.forEach((k, v) -> result.put(k, (long) Math.floor(v * multiplier)));
        return result;
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
        data.setProgress(0);
        log.info("完成建筑升级 playerId={},buildingId={},newLevel={}", casino.getPlayerId(), data.getId(), data.getLevel());
        return Code.SUCCESS;
    }

    /**
     * 完成建筑升级
     */
    private long applyAllianceSpeedup(long playerId, BuildingData data, long now) {
        if (data == null || !data.isUpgrading(now)) {
            return 0;
        }
        long seconds = allianceHelpService.consumeSpeedupSeconds(playerId, data.getId());
        long reduced = data.applySpeedupSeconds(seconds, now);
        if (reduced > 0) {
            log.info("apply alliance building speedup playerId={},buildingId={},seconds={},cdEndTime={}", playerId, data.getId(), reduced, data.getCdEndTime());
        }
        return reduced;
    }

    public void completeAllBuildingUpgrade(SimCasinoData casino) {
        if (casino == null || casino.getBuildingData() == null || casino.getBuildingData().isEmpty()) {
            return;
        }

        long now = System.currentTimeMillis();
        casino.getBuildingData().forEach((k, v) -> completeBuildingUpgrade(casino, v, now));
    }

    /**
     * 获取该建筑指定等级的产出 (基础值; 不含主管/雇员加成)
     */
    public Map<BuildingOutputType, Long> getBaseOutput(int buildingId, int level) {
        BuildingAreaTableCfg buildingAreaTableCfg = GameDataManager.getBuildingAreaTableCfg(buildingId);
        if (buildingAreaTableCfg == null) {
            return Collections.emptyMap();
        }

        BuildingUpgradeTableCfg cfg = configCache.getBuildingUpgradeCfg(buildingId, level);
        if (cfg == null) {
            return Collections.emptyMap();
        }
        Map<BuildingOutputType, Long> map = new HashMap<>();
        map.put(BuildingOutputType.fromCode(buildingAreaTableCfg.getTypeValue()), cfg.getUpgradeOutput());
        return map;
    }
}
