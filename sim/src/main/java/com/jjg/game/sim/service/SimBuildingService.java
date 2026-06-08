package com.jjg.game.sim.service;

import com.alibaba.fastjson.JSON;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.pb.KVInfo;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BuildingAreaTableCfg;
import com.jjg.game.sampledata.bean.BuildingUpgradeTableCfg;
import com.jjg.game.sampledata.bean.CasinoStatsSheetCfg;
import com.jjg.game.sim.constant.BonusType;
import com.jjg.game.sim.constant.BuildingOutputType;
import com.jjg.game.sim.constant.BuildingType;
import com.jjg.game.sim.constant.SimConstant;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private SimItemService simItemService;

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
            //前置建筑必须已解锁
            if (cfg.getUnlockMethod() > 0 && casino.findBuilding(cfg.getUnlockMethod()) == null) {
                log.warn("解锁建筑失败, 前置建筑未解锁 playerId={},buildingId={},prereq={}", ctx.playerId(), buildingId, cfg.getUnlockMethod());
                res.code = Code.PARAM_ERROR;
                ctx.send(res);
                return;
            }
            //资源足够?
            boolean remove = simItemService.removeItems(ctx, cfg.getUnlockCost(), AddType.SIM_BUILDING_UPGRADE, null);
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

            //先检查是不是添加进度条
            if (currentCfg.getCostPerLevel() != null && !currentCfg.getCostPerLevel().isEmpty()) {
                //添加进度条
                if (data.getProgress() < currentCfg.getCostPerLevel().size()) {

                    List<Integer> list = currentCfg.getCostPerLevel().get(data.getProgress());
                    boolean remove = simItemService.removeItem(ctx, list.get(0), list.get(1), AddType.SIM_BUILDING_UPGRADE);
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

            int targetLevel = data.getLevel() + 1;
            BuildingUpgradeTableCfg next = configCache.getBuildingUpgradeCfg(buildingId, targetLevel);
            if (next == null) {
                log.warn("升级建筑失败, 已达上限 playerId={},buildingId={},level={}", ctx.playerId(), buildingId, data.getLevel());
                res.code = Code.PARAM_ERROR;
                ctx.send(res);
                return;
            }
            //建筑等级 <= 经营等级 (简化: 以赌场 stats level 为经营等级)
//            CasinoStatsSheetCfg statsCfg = GameDataManager.getCasinoStatsSheetCfg(casino.getStatsId());
//            int operationLevel = statsCfg == null ? 0 : statsCfg.getLevel();
//            if (targetLevel > operationLevel + 1) {
//                log.warn("升级建筑失败, 经营等级不足 playerId={},buildingId={},targetLevel={},operationLevel={}", ctx.playerId(), buildingId, targetLevel, operationLevel);
//                res.code = Code.NOT_ENOUGH;
//                ctx.send(res);
//                return;
//            }
            boolean remove = simItemService.removeItems(ctx, next.getUpgradeCost(), AddType.SIM_BUILDING_UPGRADE, null);
            if (!remove) {
                log.warn("升级建筑失败, 资源不足 playerId={},buildingId={},cost={}", ctx.playerId(), buildingId, next.getUpgradeCost());
                res.code = Code.NOT_ENOUGH;
                ctx.send(res);
                return;
            }
            long cdMs = (long) next.getUpgradeCD() * 60_000L;
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
                return;
            }
            BuildingData data = casino.findBuilding(buildingId);
            if (data == null) {
                res.code = Code.PARAM_ERROR;
                ctx.send(res);
                return;
            }

            res.code = completeBuildingUpgrade(casino, data, System.currentTimeMillis());
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
            long now = System.currentTimeMillis();
            if (data == null || !data.isUpgrading(now)) {
                res.code = Code.PARAM_ERROR;
                ctx.send(res);
                log.warn("清除建筑升级 CD失败，当前建筑没有处于cd状态 playerId={},buildingId={}", ctx.playerId(), buildingId);
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
            } else {
                boolean remove = simItemService.removeItem(ctx, SimConstant.Item.ID_CLEAR_CD, costCount, AddType.SIM_BUILDING_UPGRADE);
                if (!remove) {
                    res.code = Code.NOT_ENOUGH;
                    ctx.send(res);
                    return;
                }
                //每消耗 1 个道具, CD 减少 1 分钟
                long reduceMs = (long) costCount * TimeHelper.ONE_MINUTE_OF_MILLIS;
                data.setCdEndTime(data.getCdEndTime() - reduceMs);
            }

            //CD 已减满: 立即完成升级
            if (data.getCdEndTime() <= now) {
                data.setCdEndTime(now);
                completeBuildingUpgrade(casino, data, now);
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
     * 立即结算当前赌场已积累的整分钟在线产出 (切换赌场前调用, 避免余量丢失)
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
                simItemService.addItem(ctx, total, AddType.SIM_BUILD_MINUTE_REWARDS, null, false);
            }
            //仅推进已结算的整分钟, 保留余量
            casino.setLastOutputTime(casino.getLastOutputTime() + fullMinutes * TimeHelper.ONE_MINUTE_OF_MILLIS);
        } catch (Exception e) {
            log.error("在线产出结算异常 playerId={}", ctx.playerId(), e);
        }
    }

    /**
     * 计算当前赌场所有游戏区/休息区建筑每分钟产出 (含雇员加成) 之和
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
            //获取建筑的基础产出，不包含加成
            Map<BuildingOutputType, Long> base = getBaseOutput(building.getId(), building.getLevel());
            if (base.isEmpty()) {
                continue;
            }

            BuildingAreaTableCfg areaCfg = GameDataManager.getBuildingAreaTableCfg(building.getId());
            if (areaCfg == null) {
                continue;
            }

            BuildingType buildingType = BuildingType.fromCode(areaCfg.getType());
            if (buildingType == null) {
                continue;
            }
            //获取加成
            BonusType bonusType = BonusType.fromBuildingType(buildingType);
            //检查是不是每分钟产出的建筑类型
            if (bonusType != null && !bonusType.isMin()) {
                continue;
            }

            //主管加成
            Map<BonusType, Integer> manageBonusesMap = employeeService.manageEmployeeBonus(ctx, building.getManagerEmployId());
            //合并
            Map<BonusType, Integer> tmpMap;
            if (manageBonusesMap != null && !manageBonusesMap.isEmpty()) {
                tmpMap = new HashMap<>();
                tmpMap.putAll(bonusesMap);
                tmpMap.putAll(manageBonusesMap);
            } else {
                tmpMap = bonusesMap;
            }

            int bonus = 0;
            if (bonusType != null) {
                bonus = tmpMap.getOrDefault(bonusType, 0);
            }

            Map<BuildingOutputType, Long> actual = applyBonus(base, bonus);
            actual.forEach((buildingOutputType, count) -> total.merge(buildingOutputType, count, Long::sum));
        }
        return total;
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
        BonusType bonusType = resolveBonusType(buildingData.getId());
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
        BonusType bonusType = resolveBonusType(buildingData.getId());
        if (bonusType == null) {
            return Collections.emptyList();
        }

        //普通雇员加成
        Map<BonusType, Integer> bonusesMap = new HashMap<>();
        employeeService.computeTypeBonusFixed(ctx, bonusesMap);
        //主管加成
        Map<BonusType, Integer> withSupervisor = employeeService.manageEmployeeBonus(ctx, buildingData.getManagerEmployId());

        //仅取主管额外贡献的部分 (总加成 - 普通雇员加成), 避免与普通雇员加成重复计算
        int normalBonus = bonusesMap.getOrDefault(bonusType, 0);
        int totalBonus = withSupervisor.getOrDefault(bonusType, 0);
        int managerBonus = totalBonus - normalBonus;
        return buildBonusList(base, managerBonus);
    }

    /**
     * 根据建筑id解析对应的加成类型
     */
    private BonusType resolveBonusType(int buildingId) {
        BuildingAreaTableCfg areaCfg = GameDataManager.getBuildingAreaTableCfg(buildingId);
        if (areaCfg == null) {
            return null;
        }
        BuildingType buildingType = BuildingType.fromCode(areaCfg.getType());
        if (buildingType == null) {
            return null;
        }
        return BonusType.fromBuildingType(buildingType);
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
//        rewards.rewards = ItemUtils.buildItemInfo(reward.getBaseReward());
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
        CasinoStatsSheetCfg statsCfg = GameDataManager.getCasinoStatsSheetCfg(casino.getStatsId());
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
        simItemService.addItem(ctx, finalReward, AddType.SIM_BUILD_OFFLINE_REWARDS, null, false);
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
