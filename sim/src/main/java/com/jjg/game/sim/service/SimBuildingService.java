package com.jjg.game.sim.service;

import com.alibaba.fastjson.JSON;
import com.jjg.game.alliance.service.AllianceEventService;
import com.jjg.game.alliance.service.AllianceHelpService;
import com.jjg.game.common.pb.ItemInfo;
import com.jjg.game.common.proto.Pair;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.base.condition.numeric.ActionConditionEvent;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.constant.GameConstant;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.ItemOperationResult;
import com.jjg.game.core.data.NoticeTipBuilder;
import com.jjg.game.core.logger.CoreLogger;
import com.jjg.game.core.pb.KVInfo;
import com.jjg.game.core.service.PlayerPackService;
import com.jjg.game.core.utils.ItemUtils;
import com.jjg.game.core.utils.TipUtils;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BuildingAreaTableCfg;
import com.jjg.game.sampledata.bean.BuildingUpgradeTableCfg;
import com.jjg.game.sampledata.bean.CasinoStatsSheetCfg;
import com.jjg.game.sim.constant.BuildingOutputType;
import com.jjg.game.sim.constant.BuildingType;
import com.jjg.game.sim.constant.SimConstant;
import com.jjg.game.sim.constant.SimStatKey;
import com.jjg.game.sim.data.*;
import com.jjg.game.sim.listener.SimPlayerTickListener;
import com.jjg.game.sim.listener.SimTaskStateReporter;
import com.jjg.game.sim.pb.SimPbConverter;
import com.jjg.game.sim.pb.res.*;
import com.jjg.game.sim.pb.struct.BuildingTips;
import com.jjg.game.sim.pb.struct.OfflineReward;
import com.jjg.game.sim.tools.SimTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Consumer;

/**
 * 建筑服务: 解锁、升级、CD 清除
 *
 * @author 11
 * @date 2026/5/28
 */
@Service
public class SimBuildingService implements SimPlayerTickListener, SimTaskStateReporter {
    private static final Logger log = LoggerFactory.getLogger(SimBuildingService.class);

    //初始等级 (解锁后)
    private static final int INITIAL_LEVEL = 1;

    //Redis 通知是实时主路径；tick 只按此间隔兜底消息丢失，避免每个 tick(2s)逐建筑访问 Redis。
    private static final long SPEEDUP_CHECK_INTERVAL_MS = 30_000L;

    @Autowired
    private SimConfigCacheService configCache;
    @Autowired
    private SimEmployeeService employeeService;
    @Autowired
    private PlayerPackService playerPackService;
    @Autowired
    private CoreLogger coreLogger;
    @Autowired
    private AllianceHelpService allianceHelpService;
    @Autowired
    private AllianceEventService allianceEventService;
    @Autowired
    private SimMedalService medalService;
    //懒加载打破与 SimCasinoService 的循环依赖 (对方持有本服务)
    @Autowired
    @Lazy
    private SimCasinoService simCasinoService;
    //懒加载打破与 SimTaskService 的循环依赖 (对方持有本服务作状态补报口)
    @Autowired
    @Lazy
    private SimTaskService simTaskService;
    @Autowired
    private SimSkillService simSkillService;

    @Override
    public void onTick(SimPlayerContext ctx, long now) {
        output(ctx, now);
        checkBuildingUpgradeComplete(ctx, now);
    }

    /**
     * 检测建筑升级 CD 是否到时: 到时由服务器完成升级并主动下发 ResCompleteBuildingUpgrade。
     * 取代客户端 CD 结束后主动请求完成的旧逻辑 (请求路径保留为幂等兜底)。
     */
    private void checkBuildingUpgradeComplete(SimPlayerContext ctx, long now) {
        try {
            SimCasinoData casino = ctx.getCurrentCasino();
            if (casino == null || casino.getBuildingData() == null || casino.getBuildingData().isEmpty()) {
                return;
            }
            boolean consumeSpeedup = now - ctx.getLastSpeedupCheckTime() >= SPEEDUP_CHECK_INTERVAL_MS;
            if (consumeSpeedup) {
                ctx.setLastSpeedupCheckTime(now);
            }
            for (BuildingData data : casino.getBuildingData().values()) {
                //未处于升级 CD 的跳过
                if (data.getCdEndTime() <= 0) {
                    continue;
                }
                //应用联盟加速 (可能使 CD 提前到时; 最多延迟一个检查间隔生效)
                if (consumeSpeedup) {
                    applyAllianceSpeedup(ctx.playerId(), data, now);
                }
                if (data.isUpgradeReady(now)) {
                    onCompleteBuildingUpgrade(ctx, data.getId());
                }
            }
        } catch (Exception e) {
            log.error("检测建筑升级完成异常 playerId={}", ctx.playerId(), e);
        }
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
        ResCompleteBuildingUpgrade completeRes = null;
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

            long now = System.currentTimeMillis();
            applyAllianceSpeedup(ctx.playerId(), buildingData, now);
            if (buildingData.isUpgradeReady(now) && completeBuildingUpgradeAndReport(ctx, ctx.getCurrentCasino(), buildingData, now) == Code.SUCCESS) {
                completeRes = completeBuildingUpgradeResponse(buildingData);
            }
            res.buildingInfo = SimPbConverter.toBuildingInfo(buildingData, now);
            //建筑的基础产出，不包含加成
            Map<BuildingOutputType, Long> base = getBaseOutput(buildingData.getId(), buildingData.getLevel());
            //普通雇员加成
            res.employeeBonus = normalEmployeeBonus(ctx, base);
            //主管加成 (与任命主管返回一致: 配置值按产出类型过滤)
            BuildingAreaTableCfg areaCfg = GameDataManager.getBuildingAreaTableCfg(buildingData.getId());
            SimEmployeeService.ManageBonus manage = employeeService.managerBonusFiltered(ctx, areaCfg == null ? 0 : areaCfg.getEmployeeProfile());
            res.manageEmployeeBonus = employeeService.toKVList(manage.modifier());
            res.manageEmployeeFixBonus = employeeService.toKVList(manage.buff());
            //观看广告次数限制
            res.watchAdLimit = GameDataManager.getGlobalConfigCfg(SimConstant.Global.ID_WATCH_AD_LIMIT).getIntValue();
            //主管id
            res.managerId = managerId(ctx, buildingData);
            if (res.managerId > 0) {
                SimEmployeeData employee = ctx.getEmployee(res.managerId);
                if (employee != null) {
                    res.managerLevel = employee.getLevel();
                }
            } else {
                //找到雇员中是否有可以为该建筑设置为主管的
                int employeeProfile = areaCfg == null ? 0 : areaCfg.getEmployeeProfile();
                res.canSetManager = employeeProfile > 0 && ctx.getEmployeeMap().keySet().stream().map(GameDataManager::getEmployeeProfileCfg).filter(Objects::nonNull).anyMatch(cfg -> cfg.getProfessionID() == employeeProfile);
            }

            log.info("返回建筑信息 playerId={},res={}", ctx.playerId(), JSON.toJSONString(res));
        } catch (Exception e) {
            log.error("", e);
        }
        ctx.send(res);
        if (completeRes != null) {
            ctx.send(completeRes);
        }
    }

    /**
     * 获取当前场景的所有建筑信息
     *
     * @param ctx 玩家上下文
     */
    public void onAllBuildingInfo(SimPlayerContext ctx) {
        ResAllBuildingInfo res = new ResAllBuildingInfo(Code.SUCCESS);
        try {
            SimCasinoData casino = ctx.getCurrentCasino();
            if (casino == null) {
                log.warn("获取所有建筑信息失败, 当前场景为空 playerId={}", ctx.playerId());
                res.code = Code.NOT_FOUND;
            } else {
                applyPendingSpeedup(ctx, casino, System.currentTimeMillis());
                res.buildings = SimPbConverter.toBuildingInfos(casino);
            }
        } catch (Exception e) {
            log.error("获取所有建筑信息异常 playerId={}", ctx.playerId(), e);
            res.code = Code.EXCEPTION;
        }
        ctx.send(res);
    }

    /**
     * 获取主管id
     *
     * @param ctx
     * @param buildingData
     * @return
     */
    private int managerId(SimPlayerContext ctx, BuildingData buildingData) {
        BuildingAreaTableCfg areaCfg = GameDataManager.getBuildingAreaTableCfg(buildingData.getId());
        if (areaCfg == null || areaCfg.getEmployeeProfile() < 1) {
            return 0;
        }
        return ctx.getCurrentCasino().manageEmploy(areaCfg.getEmployeeProfile());
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
                Map<BuildingOutputType, Long> finalReward = computeFinalReward(ctx, reward, watchAd);
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
    public ResUnlockBuilding onUnlockBuilding(SimPlayerContext ctx, int buildingId) {
        ResUnlockBuilding res = new ResUnlockBuilding(Code.SUCCESS);
        res.id = buildingId;
        try {
            SimCasinoData casino = ctx.getCurrentCasino();
            if (casino == null) {
                log.warn("解锁建筑失败, 当前场景不存在 playerId={}", ctx.playerId());
                res.code = Code.NOT_FOUND;
                return res;
            }
            BuildingAreaTableCfg cfg = GameDataManager.getBuildingAreaTableCfg(buildingId);
            if (cfg == null) {
                log.warn("解锁建筑失败, 配置不存在 playerId={},buildingId={}", ctx.playerId(), buildingId);
                res.code = Code.NOT_FOUND;
                return res;
            }
            if (cfg.getRegionID() != casino.getCasinoId()) {
                log.warn("解锁建筑失败, 建筑不属于当前场景 playerId={},buildingId={},casinoId={}", ctx.playerId(), buildingId, casino.getCasinoId());
                res.code = Code.PARAM_ERROR;
                return res;
            }

            if (casino.findBuilding(buildingId) != null) {
                log.warn("解锁建筑失败, 已解锁 playerId={},buildingId={}", ctx.playerId(), buildingId);
                res.code = Code.PARAM_ERROR;
                return res;
            }

            List<BuildingTips> tips = new ArrayList<>();
            //条件1
            if (cfg.getCasinoLevel() > 0 && ctx.getSimBaseData().getAllLevel() < cfg.getCasinoLevel()) {
                log.warn("解锁建筑失败, 场景等级不足 playerId={},buildingId={},casinoId={},cfgCasinoLevel={},ctxAllLevel={}", ctx.playerId(), buildingId, casino.getCasinoId(), cfg.getCasinoLevel(), ctx.getSimBaseData().getAllLevel());

                int languageId = getBuildingUnlockLangId(cfg.getLanguageID(), 0);
                if (languageId < 1) {
                    res.code = Code.LEVEL_NOT_ENOUGH;
                    return res;
                }
                tips.add(SimTool.buildTips(languageId, String.valueOf(cfg.getCasinoLevel())));
            }

            //条件2
            if (cfg.getUnlockMethod() != null) {
                int languageId = getBuildingUnlockLangId(cfg.getLanguageID(), 1);

                for (Map.Entry<Integer, Integer> en : cfg.getUnlockMethod().entrySet()) {
                    BuildingData building = casino.findBuilding(en.getKey());
                    if (building == null) {
                        if (languageId < 1) {
                            log.warn("解锁建筑失败, 解锁方式未通过,建筑未解锁 playerId={},buildingId={},unLockBuildingId={}", ctx.playerId(), buildingId, en.getKey());
                            res.code = Code.PARAM_ERROR;
                            return res;
                        }
                        BuildingAreaTableCfg tmpCfg = GameDataManager.getBuildingAreaTableCfg(en.getKey());
                        tips.add(SimTool.buildTips(languageId, tmpCfg.getBuildingNameId(), String.valueOf(en.getValue())));
                        continue;
                    }
                    if (building.getLevel() < en.getValue()) {
                        if (languageId < 1) {
                            log.warn("解锁建筑失败, 解锁方式未通过,等级不足 playerId={},buildingId={},level={},unLockBuildingId={},cfgLevel={}", ctx.playerId(), buildingId, building.getLevel(), en.getKey(), en.getValue());
                            res.code = Code.PARAM_ERROR;
                            return res;
                        }
                        BuildingAreaTableCfg tmpCfg = GameDataManager.getBuildingAreaTableCfg(en.getKey());
                        tips.add(SimTool.buildTips(languageId, tmpCfg.getBuildingNameId(), String.valueOf(en.getValue())));
                    }
                }
            }

            //条件3
            int costLanguageId = getBuildingUnlockLangId(cfg.getLanguageID(), 2);
            if (tips.isEmpty()) {
                //资源足够?
                boolean remove = playerPackService.removeItems(ctx.getPlayer(), cfg.getUnlockCost(), AddType.SIM_BUILDING_UPGRADE, null).success();
                if (!remove) {
                    if (costLanguageId < 1) {
                        log.warn("解锁建筑失败, 资源不足 playerId={},buildingId={},cost={}", ctx.playerId(), buildingId, cfg.getUnlockCost());
                        res.code = Code.NOT_ENOUGH;
                        return res;
                    }
                    Long l = cfg.getUnlockCost().get(ItemUtils.getGoldItemId());
                    if (l == null) {
                        log.warn("解锁建筑失败, 解析金币时获取错误 playerId={},buildingId={},cost={}", ctx.playerId(), buildingId, cfg.getUnlockCost());
                        res.code = Code.NOT_ENOUGH;
                        return res;
                    }
                    tips.add(SimTool.buildTips(costLanguageId, String.valueOf(l)));
                }
            } else {
                boolean has = playerPackService.checkHasItems(ctx.getPlayer(), cfg.getUnlockCost());
                if (!has) {
                    if (costLanguageId < 1) {
                        log.warn("解锁建筑失败, 资源不足 playerId={},buildingId={},cost={}", ctx.playerId(), buildingId, cfg.getUnlockCost());
                        res.code = Code.NOT_ENOUGH;
                        return res;
                    }
                    Long l = cfg.getUnlockCost().get(ItemUtils.getGoldItemId());
                    if (l == null) {
                        log.warn("解锁建筑失败, 解析金币时获取错误1 playerId={},buildingId={},cost={}", ctx.playerId(), buildingId, cfg.getUnlockCost());
                        res.code = Code.NOT_ENOUGH;
                        return res;
                    }
                    tips.add(SimTool.buildTips(costLanguageId, String.valueOf(l)));
                }
            }

            if (!tips.isEmpty()) {
                NotifyBuildingTips notify = new NotifyBuildingTips();
                notify.tips = tips;
                ctx.send(notify);
                return null;
            }

            unlockAndUpdateBuildData(ctx, buildingId, cfg);
            log.info("解锁建筑成功 playerId={},buildingId={}", ctx.playerId(), buildingId);
        } catch (Exception e) {
            log.error("", e);
            res.code = Code.EXCEPTION;
        }
        return res;
    }


    /**
     * 升级建筑 (启动 CD)
     */
    public boolean canUpgradeBuilding(SimPlayerContext ctx, int buildingId) {
        long now = System.currentTimeMillis();
        SimCasinoData casino = ctx.getCurrentCasino();
        if (casino == null) {
            return false;
        }
        BuildingData data = casino.findBuilding(buildingId);
        if (data == null || data.isUpgrading(now)) {
            return false;
        }
        BuildingUpgradeTableCfg currentCfg = configCache.getBuildingUpgradeCfg(buildingId, data.getLevel());
        return currentCfg != null && checkBuildingUpgrade(ctx, data, currentCfg).getFirst() == BuildingUpgradeCheck.CAN_UPGRADE && playerPackService.checkHasItems(ctx.getPlayer(), currentCfg.getUpgradeCost());
    }

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
            if (data.isUpgrading(now)) {
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

            Pair<BuildingUpgradeCheck, Integer> check = checkBuildingUpgrade(ctx, data, currentCfg);
            if (check.getFirst() == BuildingUpgradeCheck.ADD_PROGRESS) {
                //添加进度条
                List<Integer> list = currentCfg.getCostPerLevel().get(data.getProgress());
                boolean remove = playerPackService.removeItem(ctx.getPlayer(), list.get(0), list.get(1), AddType.SIM_BUILDING_UPGRADE).success();
                if (!remove) {
                    log.warn("建筑添加进度条失败, 扣除资源失败 playerId={},buildingId={},level={}，itemId={},count={}", ctx.playerId(), buildingId, data.getLevel(), list.get(0), list.get(1));
                    res.code = Code.NOT_ENOUGH_ITEM;
                    ctx.send(res);
                    return;
                }

                data.setProgress(data.getProgress() + 1);
                res.buildingInfo = SimPbConverter.toBuildingInfo(data, now);
                ctx.send(res);
                log.info("建筑增加进度条 playerId={},buildingInfo={}", ctx.playerId(), JSON.toJSONString(res.buildingInfo));
                return;
            }

            if (check.getFirst() != BuildingUpgradeCheck.CAN_UPGRADE) {
                if (check.getFirst() == BuildingUpgradeCheck.SKILL_LEVEL_LOW) {
                    NoticeTipBuilder builder = NoticeTipBuilder.builder().tipType(TipUtils.TipType.TOAST).languageId(check.getFirst().code);
                    builder.addArg(2, check.getSecond() + "");
                    TipUtils.sendTip(ctx.getPlayerController(), TipUtils.TipType.TOAST, () -> builder.build());
                } else {
                    res.code = check.getFirst().code;
                    ctx.send(res);
                }
                log.warn("升级建筑失败, 检查升级条件未通过 playerId={},buildingId={},level={},check={}", ctx.playerId(), buildingId, data.getLevel(), check);
                return;
            }

            boolean remove = playerPackService.removeItems(ctx.getPlayer(), currentCfg.getUpgradeCost(), AddType.SIM_BUILDING_UPGRADE, null).success();
            if (!remove) {
                log.warn("升级建筑失败, 资源不足 playerId={},buildingId={},cost={}", ctx.playerId(), buildingId, currentCfg.getUpgradeCost());
                res.code = Code.NOT_ENOUGH;
                ctx.send(res);
                return;
            }
            long cdMs = (long) currentCfg.getUpgradeCD() * 60_000L;
            allianceHelpService.consumeSpeedupSeconds(ctx.playerId(), buildingId);
            data.setCdEndTime(now + cdMs);
            res.buildingInfo = SimPbConverter.toBuildingInfo(data, now);
            log.info("升级建筑启动 playerId={},buildingInfo={}", ctx.playerId(), JSON.toJSONString(res.buildingInfo));
        } catch (Exception e) {
            log.error("", e);
            res.code = Code.EXCEPTION;
        }
        ctx.send(res);
    }

    private Pair<BuildingUpgradeCheck, Integer> checkBuildingUpgrade(SimPlayerContext ctx, BuildingData data, BuildingUpgradeTableCfg currentCfg) {
        if (currentCfg.getCostPerLevel() != null && !currentCfg.getCostPerLevel().isEmpty() && data.getProgress() < currentCfg.getCostPerLevel().size()) {
            return new Pair<>(BuildingUpgradeCheck.ADD_PROGRESS, 0);
        }
        if (currentCfg.getUpgradeCost() == null || currentCfg.getUpgradeCost().isEmpty()) {
            return new Pair<>(BuildingUpgradeCheck.UPGRADE_COST_NOT_CONFIGURED, 0);
        }
        if (currentCfg.getNeedLevel() > ctx.getSimBaseData().getAllLevel()) {
            return new Pair<>(BuildingUpgradeCheck.CASINO_LEVEL_LOW, 0);
        }
        if (configCache.getBuildingUpgradeCfg(data.getId(), data.getLevel() + 1) == null) {
            return new Pair<>(BuildingUpgradeCheck.MAX_LEVEL, 0);
        }

        BuildingAreaTableCfg buildingAreaTableCfg = GameDataManager.getBuildingAreaTableCfg(data.getId());
        if (buildingAreaTableCfg != null && buildingAreaTableCfg.getUnlockGameId() > 0) {
            BuildingUpgradeTableCfg nextCfg = configCache.getBuildingUpgradeCfg(data.getId(), data.getLevel() + 1);
            if (nextCfg != null) {
                //检查技能等级
                SimSkillsData skillData = ctx.getSkillData(buildingAreaTableCfg.getUnlockGameId());
                if (skillData == null || skillData.allLevel() < nextCfg.getSkillLevel()) {
                    return new Pair<>(BuildingUpgradeCheck.SKILL_LEVEL_LOW, nextCfg.getSkillLevel());
                }
            }
        }
        return new Pair<>(BuildingUpgradeCheck.CAN_UPGRADE, 0);
    }

    private enum BuildingUpgradeCheck {
        CAN_UPGRADE(Code.SUCCESS),
        ADD_PROGRESS(Code.PARAM_ERROR),
        UPGRADE_COST_NOT_CONFIGURED(Code.PARAM_ERROR),
        CASINO_LEVEL_LOW(Code.SIM_CASINO_LEVEL_LOW),
        MAX_LEVEL(Code.PARAM_ERROR),
        SKILL_LEVEL_LOW(Code.SKILL_LEVEL_NOT_ENOUGHT),
        ;

        private int code;

        BuildingUpgradeCheck(int code) {
            this.code = code;
        }
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
            res.code = completeBuildingUpgradeAndReport(ctx, casino, data, now);
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
    public void onClearBuildingCD(SimPlayerContext ctx, int buildingId, int costCount, boolean watchAd, int costItemId) {
        ResClearBuildingCD res = new ResClearBuildingCD(Code.SUCCESS);
        ResCompleteBuildingUpgrade completeRes = null;
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
            boolean upgrading = data.isUpgrading(now);
            if (!upgrading && !data.isUpgradeReady(now)) {
                res.code = Code.PARAM_ERROR;
                ctx.send(res);
                log.warn("clear building cd failed, building is not upgrading playerId={},buildingId={}", ctx.playerId(), buildingId);
                return;
            }

            if (upgrading && watchAd) {
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
                ctx.getSimBaseData().incWatchAdCount();
                //主线任务: 观看广告一次 -> 推进 12209
                allianceEventService.onAdWatch(ctx.playerId());
            } else if (upgrading) {
                if (costItemId != SimConstant.Item.ID_CLEAR_CD && costItemId != ItemUtils.getDiamondItemId()) {
                    res.code = Code.PARAM_ERROR;
                    ctx.send(res);
                    log.warn("清除建筑升级 CD失败，道具id错误 playerId={},costItemId={}", ctx.playerId(), costItemId);
                    return;
                }
                if (costCount < 1) {
                    res.code = Code.NOT_ENOUGH;
                    ctx.send(res);
                    log.warn("道具清除建筑升级 CD失败，costCount不能小于1 playerId={},buildingId={},costCount={}", ctx.playerId(), buildingId, costCount);
                    return;
                }
                long reduceMs = (long) costCount * TimeHelper.ONE_MINUTE_OF_MILLIS;
                if (costItemId == ItemUtils.getDiamondItemId()) {
                    int diamondCostPerMinute = GameDataManager.getGlobalConfigCfg(SimConstant.Global.ID_DIAMOND_CLEAR_CD_COST).getIntValue();
                    reduceMs /= diamondCostPerMinute;
                }
                boolean remove = playerPackService.removeItem(ctx.getPlayer(), costItemId, costCount, AddType.SIM_BUILDING_UPGRADE).success();
                if (!remove) {
                    res.code = Code.NOT_ENOUGH;
                    ctx.send(res);
                    log.warn("道具清除建筑升级 CD失败，道具不足 playerId={},buildingId={},costItemId={},costCount={}", ctx.playerId(), buildingId, costItemId, costCount);
                    return;
                }
                //costCount 表示实际消耗数量：加速卡每张清 1 分钟，钻石按全局配置换算时长
                data.setCdEndTime(data.getCdEndTime() - reduceMs);
            }

            now = System.currentTimeMillis();
            if (data.isUpgradeReady(now)) {
                res.code = completeBuildingUpgradeAndReport(ctx, casino, data, now);
                if (res.code == Code.SUCCESS) {
                    completeRes = completeBuildingUpgradeResponse(data);
                }
            }
            res.buildingInfo = SimPbConverter.toBuildingInfo(data, now);
            log.info("清除建筑升级CD playerId={},buildingId={},watchAd={},costCount={},level={},cdEndTime={}", ctx.playerId(), buildingId, watchAd, costCount, data.getLevel(), data.getCdEndTime());
        } catch (Exception e) {
            log.error("", e);
            res.code = Code.EXCEPTION;
        }
        ctx.send(res);
        if (completeRes != null) {
            ctx.send(completeRes);
        }
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
            long settledOutputTime = casino.getLastOutputTime() + fullMinutes * TimeHelper.ONE_MINUTE_OF_MILLIS;
            Map<BuildingOutputType, Long> perMinute = computePerMinuteOutput(ctx, casino);
            if (!perMinute.isEmpty()) {
                Map<BuildingOutputType, Long> total = multiply(perMinute, fullMinutes);
                Map<Integer, Long> items = toItemMap(total);
                if (!items.isEmpty()) {
                    CommonResult<ItemOperationResult> addResult = playerPackService.addItems(ctx.playerId(), items, AddType.SIM_BUILD_MINUTE_REWARDS, null, false);
                    if (addResult == null || !addResult.success()) {
                        log.warn("在线产出入账失败 playerId={},code={}", ctx.playerId(), addResult == null ? Code.FAIL : addResult.code);
                        return;
                    }
                }
                //先提交结算时间，避免后续统计或通知异常导致同一时间段重复发奖
                casino.setLastOutputTime(settledOutputTime);
                simCasinoService.addCasinoExp(ctx, total.getOrDefault(BuildingOutputType.CASINO_LEVEL_EXP, 0L));
                //经营信息: 累加每分钟自产金币收益
                long minuteGold = total.getOrDefault(BuildingOutputType.GOLD, 0L);
                if (minuteGold > 0) {
                    ctx.getSimBaseData().addBusinessIncome(minuteGold);
                }

                if (!items.isEmpty()) {
                    allianceEventService.onBusinessIncome(ctx.playerId(), items);
                }

                if (!total.isEmpty()) {
                    NotifyBuildingOutput notify = new NotifyBuildingOutput();
                    notify.rewards = new ArrayList<>();

                    for (Map.Entry<BuildingOutputType, Long> en : total.entrySet()) {
                        ItemInfo itemInfo = new ItemInfo();
                        itemInfo.itemId = en.getKey().getCode();
                        itemInfo.count = en.getValue();
                        notify.rewards.add(itemInfo);
                    }
                    if (!notify.rewards.isEmpty()) {
                        ctx.send(notify);
                    }
                }
                return;
            }
            //仅推进已结算的整分钟, 保留余量
            casino.setLastOutputTime(settledOutputTime);
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
        Map<BuildingOutputType, Integer> bonusesMap = new HashMap<>();
        employeeService.computeEmployeeLevelBonus(ctx, bonusesMap);

        Map<BuildingOutputType, Long> total = new HashMap<>();
        for (BuildingData building : casino.getBuildingData().values()) {
            Map<BuildingOutputType, Long> actual = buildingActualPerMinute(ctx, building, bonusesMap);
            actual.forEach((buildingOutputType, count) -> total.merge(buildingOutputType, count, Long::sum));
        }
        return total;
    }

    /**
     * 计算单个建筑每分钟实际产出 (含普通雇员 + 主管 + 技能加成); 非每分钟产出类型返回空。
     *
     * @param bonusesMap 已汇总的普通雇员加成 (按类型)
     */
    private Map<BuildingOutputType, Long> buildingActualPerMinute(SimPlayerContext ctx, BuildingData building, Map<BuildingOutputType, Integer> bonusesMap) {
        BuildingAreaTableCfg areaCfg = GameDataManager.getBuildingAreaTableCfg(building.getId());
        if (areaCfg == null) {
            return Collections.emptyMap();
        }
        //只有游戏区和休息区才有每分钟产出
        BuildingType buildingType = BuildingType.fromCode(areaCfg.getType());
        if (buildingType != BuildingType.GAME && buildingType != BuildingType.REST) {
            return Collections.emptyMap();
        }

        //获取建筑的基础产出，不包含加成
        Map<BuildingOutputType, Long> base = getBaseOutput(building.getId(), building.getLevel());
        if (base.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<BuildingOutputType, Long> actual = applyBuildingBonus(ctx, base, areaCfg.getEmployeeProfile(), bonusesMap);
        long skillBonus = skillOutputBonus(ctx, areaCfg.getUnlockGameId());
        if (skillBonus == 0) {
            return actual;
        }
        for (Map.Entry<BuildingOutputType, Long> en : base.entrySet()) {
            if (en.getKey() != BuildingOutputType.CASINO_LEVEL_EXP) {
                actual.merge(en.getKey(), en.getValue() * skillBonus / GameConstant.TEN_THOUSAND, Long::sum);
            }
        }
        return actual;
    }

    /**
     * 计算细分运营看板中单栋建筑的实际属性。
     * 游戏区和休息区返回每分钟产出（含雇员、主管、技能加成），管理区返回当前部门属性。
     */
    public Map<BuildingOutputType, Long> computeDashboardBuildingValues(SimPlayerContext ctx, BuildingData building) {
        if (ctx == null || building == null) {
            return Collections.emptyMap();
        }
        BuildingAreaTableCfg areaCfg = GameDataManager.getBuildingAreaTableCfg(building.getId());
        if (areaCfg == null) {
            return Collections.emptyMap();
        }
        Map<BuildingOutputType, Long> base = getBaseOutput(building.getId(), building.getLevel());
        if (base.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<BuildingOutputType, Integer> bonusesMap = new HashMap<>();
        employeeService.computeEmployeeLevelBonus(ctx, bonusesMap);
        Map<BuildingOutputType, Long> actual = applyBuildingBonus(
                ctx, base, areaCfg.getEmployeeProfile(), bonusesMap);

        BuildingType buildingType = BuildingType.fromCode(areaCfg.getType());
        if (buildingType != BuildingType.GAME && buildingType != BuildingType.REST) {
            return actual;
        }
        long skillBonus = skillOutputBonus(ctx, areaCfg.getUnlockGameId());
        if (skillBonus == 0) {
            return actual;
        }
        for (Map.Entry<BuildingOutputType, Long> entry : base.entrySet()) {
            if (entry.getKey() != BuildingOutputType.CASINO_LEVEL_EXP) {
                actual.merge(entry.getKey(),
                        entry.getValue() * skillBonus / GameConstant.TEN_THOUSAND, Long::sum);
            }
        }
        return actual;
    }

    private long skillOutputBonus(SimPlayerContext ctx, int gameType) {
        long bonus = skillOutputBonus(ctx.getSkillData(0));
        if (gameType > 0) {
            bonus += skillOutputBonus(ctx.getSkillData(gameType));
        }
        return bonus;
    }

    private long skillOutputBonus(SimSkillsData skillsData) {
        Map<Integer, SkillDetailData> skillsMap = skillsData == null ? null : skillsData.getSkillsMap();
        if (skillsMap == null || skillsMap.isEmpty()) {
            return 0;
        }
        long bonus = 0;
        for (SkillDetailData detail : skillsMap.values()) {
            if (detail != null) {
                bonus += detail.getAddOutPut();
            }
        }
        return bonus;
    }

    /**
     * 为单个建筑的基础产出叠加 (普通雇员 + 主管) 加成; 按产出类型分别计算:
     * 实际产出 = 基础 + 基础 * 百分比 / 1000 + 固定值。
     * 百分比 = 雇员等级加成 + 主管技能 Modifier；固定值 = 主管技能 Buff + 成就徽章 BuffId。
     */
    private Map<BuildingOutputType, Long> applyBuildingBonus(SimPlayerContext ctx, Map<BuildingOutputType, Long> base, int employeeProfile, Map<BuildingOutputType, Integer> bonusesMap) {
        if (base == null || base.isEmpty()) {
            return base;
        }
        //主管加成 (百分比 + 固定值)
        SimEmployeeService.ManageBonus manage = employeeService.manageEmployeeBonus(ctx, employeeProfile);
        Map<BuildingOutputType, Long> result = new HashMap<>(base.size());
        for (Map.Entry<BuildingOutputType, Long> en : base.entrySet()) {
            BuildingOutputType group = en.getKey().bonusGroup();
            long baseVal = en.getValue();
            int percent = bonusesMap.getOrDefault(group, 0) + manage.modifier().getOrDefault(group, 0);
            int fixed = manage.buff().getOrDefault(group, 0)
                    + medalService.getFixedBonus(ctx, en.getKey());
            result.put(en.getKey(), baseVal + baseVal * percent / SimConstant.Common.EMPLOYEE_BONUS_DIVISOR + fixed);
        }
        return result;
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
        Map<BuildingOutputType, Integer> bonusesMap = new HashMap<>();
        employeeService.computeEmployeeLevelBonus(ctx, bonusesMap);

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
            Map<BuildingOutputType, Long> actual = applyBuildingBonus(ctx, base, areaCfg.getEmployeeProfile(), bonusesMap);
            if (buildingType == BuildingType.REST) {
                //能量房间: 休息区 POWER 产量
                result.merge(SimStatKey.Operation.ENERGY_ROOM, actual.getOrDefault(BuildingOutputType.POWER, 0L), Long::sum);
            } else {
                //按建筑ID末两位区分: 1~6 SLOT(金币收益), 7~9 扑克, 10~12 捕鱼, 各房间产量互不重叠
                long gold = actual.getOrDefault(BuildingOutputType.GOLD, 0L);
                int statKey = resolveGameRoomStatKey(areaCfg);
                if (statKey > 0) {
                    result.merge(statKey, gold, Long::sum);
                }
            }
        }
        return result;
    }

    static int resolveGameRoomStatKey(BuildingAreaTableCfg areaCfg) {
        if (areaCfg == null) {
            return 0;
        }
        //游戏区建筑ID末两位即原 SequenceID: 1~6 SLOT, 7~9 扑克, 10~12 捕鱼
        int sequenceId = areaCfg.getId() % 100;
        if (sequenceId >= 1 && sequenceId <= 6) {
            return SimStatKey.Operation.GOLD_INCOME;
        }
        if (sequenceId >= 7 && sequenceId <= 9) {
            return SimStatKey.Operation.POKER_ROOM;
        }
        if (sequenceId >= 10 && sequenceId <= 12) {
            return SimStatKey.Operation.FISHING_ROOM;
        }
        return 0;
    }

    /**
     * 经营信息-当前可容纳游客人数: 各已解锁建筑当前等级的最大交互数量之和。
     */
    public int computeCurrentCapacity(SimCasinoData casino) {
        if (casino.getBuildingData() == null || casino.getBuildingData().isEmpty()) {
            return 0;
        }
        int sum = 0;
        for (BuildingData building : casino.getBuildingData().values()) {
            BuildingUpgradeTableCfg cfg = configCache.getBuildingUpgradeCfg(building.getId(), building.getLevel());
            if (cfg == null) {
                continue;
            }
            sum += cfg.getMaxInteractionCount();
        }
        return sum;
    }

    /**
     * 经营信息-升满级最大容纳游客人数: 该场景全部建筑满级的最大交互数量之和。
     */
    public int computeMaxCapacity(int casinoId) {
        int sum = 0;
        for (BuildingAreaTableCfg areaCfg : GameDataManager.getBuildingAreaTableCfgList()) {
            if (areaCfg.getRegionID() != casinoId) {
                continue;
            }
            BuildingUpgradeTableCfg cfg = configCache.getBuildingUpgradeCfg(areaCfg.getId(), configCache.getBuildingMaxLevel(areaCfg.getId()));
            if (cfg == null) {
                continue;
            }
            sum += cfg.getMaxInteractionCount();
        }
        return sum;
    }

    /**
     * 经营信息-职能部门 (管理区) 当前等级的属性值 (含管理区普通雇员 + 主管加成, 与建筑详情页口径一致):
     * 接待区(服务能力) / 营销部(曝光度) / 运营部(知名度)。
     *
     * @param outputType 部门对应的产出类型 (按建筑 typeValue 匹配)
     * @return 部门属性值; 未解锁对应建筑返回 0
     */
    public long computeDeptValue(SimPlayerContext ctx, SimCasinoData casino, BuildingOutputType outputType) {
        BuildingData dept = findDeptBuilding(casino, outputType);
        if (dept == null) {
            return 0;
        }
        Map<BuildingOutputType, Long> base = getBaseOutput(dept.getId(), dept.getLevel());
        if (base.isEmpty()) {
            return 0;
        }
        BuildingAreaTableCfg areaCfg = GameDataManager.getBuildingAreaTableCfg(dept.getId());
        Map<BuildingOutputType, Integer> bonusesMap = new HashMap<>();
        employeeService.computeEmployeeLevelBonus(ctx, bonusesMap);
        Map<BuildingOutputType, Long> actual = applyBuildingBonus(ctx, base, areaCfg.getEmployeeProfile(), bonusesMap);
        return actual.getOrDefault(outputType, 0L);
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
            List<Integer> typeValues = areaCfg.getTypeValue();
            if (typeValues != null && typeValues.contains(outputType.getCode())) {
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
     * @return KVInfo.key=itemId  KVInfo.value=bouns
     */
    public List<KVInfo> normalEmployeeBonus(SimPlayerContext ctx, Map<BuildingOutputType, Long> base) {
        if (base == null || base.isEmpty()) {
            return Collections.emptyList();
        }
        //所有已解锁雇员的等级加成 (按类型汇总)
        Map<BuildingOutputType, Integer> bonusesMap = new HashMap<>();
        employeeService.computeEmployeeLevelBonus(ctx, bonusesMap);
        List<KVInfo> list = new ArrayList<>(base.size());
        for (Map.Entry<BuildingOutputType, Long> en : base.entrySet()) {
            int bonus = bonusesMap.getOrDefault(en.getKey().bonusGroup(), 0);
            list.add(new KVInfo(en.getKey().getCode(), bonus));
        }
        return list;
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

        Map<BuildingOutputType, Long> finalReward = computeFinalReward(ctx, reward, watchAd);
        Map<Integer, Long> items = toItemMap(finalReward);
        if (!items.isEmpty()) {
            CommonResult<ItemOperationResult> addResult = playerPackService.addItems(ctx.playerId(), items, AddType.SIM_BUILD_OFFLINE_REWARDS, null, false);
            if (addResult == null || !addResult.success()) {
                int code = addResult == null ? Code.FAIL : addResult.code;
                log.warn("离线收益入账失败 playerId={},code={}", ctx.playerId(), code);
                return code;
            }
        }
        long casinoExp = finalReward.getOrDefault(BuildingOutputType.CASINO_LEVEL_EXP, 0L);
        long beforeCasinoExp = casino.getExp();
        simCasinoService.addCasinoExp(ctx, casinoExp);
        //仅补充 Kafka 道具流水，不改变娱乐城经验原有的发放路径。
        if (casinoExp > 0) {
            coreLogger.addItems(
                    ctx.playerId(),
                    Map.of(SimConstant.Item.CASINO_EXP, beforeCasinoExp),
                    Map.of(SimConstant.Item.CASINO_EXP, casinoExp),
                    Map.of(SimConstant.Item.CASINO_EXP, (long) casino.getExp()),
                    AddType.SIM_BUILD_OFFLINE_REWARDS,
                    "离线收益娱乐城经验");
        }
        //经营信息: 离线产出金币计入经营收益; 看广告领取计入观看广告数
        long offlineGold = finalReward.getOrDefault(BuildingOutputType.GOLD, 0L);
        ctx.getSimBaseData().addBusinessIncome(offlineGold);
        if (!items.isEmpty()) {
            allianceEventService.onBusinessIncome(ctx.playerId(), items);
        }
        if (watchAd) {
            ctx.getSimBaseData().incWatchAdCount();
            //主线任务: 观看广告一次 -> 推进 12209
            allianceEventService.onAdWatch(ctx.playerId());
        }
        //领取后重置
        ctx.setPendingOffline(null);
        log.info("领取离线收益 playerId={},watchAd={},reward={}", ctx.playerId(), watchAd, finalReward);
        return Code.SUCCESS;
    }

    /**
     * 上报当前场景各等级建筑的持有量, 推进 12208 "拥有 N 个 ≥X 级建筑"。
     * 内存仅驻留当前场景数据, 主线新手阶段玩家通常仅一座娱乐城, 故按当前场景统计。
     * 直接用 ctx 投递: 登录期补报时 ctx 尚未入 registry, 走 playerId 查找会被丢弃。
     */
    @Override
    public void reportTaskState(SimPlayerContext ctx, Consumer<ActionConditionEvent> sink) {
        reportBuildingCounts(ctx, sink);
    }

    private void reportBuildingCounts(SimPlayerContext ctx) {
        reportBuildingCounts(ctx, e -> simTaskService.onConditionEvent(ctx, e));
    }

    private void reportBuildingCounts(SimPlayerContext ctx, Consumer<ActionConditionEvent> sink) {
        SimCasinoData casino = ctx.getCurrentCasino();
        if (casino == null || casino.getBuildingData() == null || casino.getBuildingData().isEmpty()) {
            return;
        }
        SortedMap<Integer, Long> countByLevel = new TreeMap<>();
        for (BuildingData building : casino.getBuildingData().values()) {
            countByLevel.merge(building.getLevel(), 1L, Long::sum);
        }
        SimConditionEventFactory.emitOwnershipCounts(sink, ActionConditionEvent.Type.BUILDING_COUNT, 0, countByLevel);
    }

    private Map<BuildingOutputType, Long> multiply(Map<BuildingOutputType, Long> src, long factor) {
        Map<BuildingOutputType, Long> result = new HashMap<>(src.size());
        src.forEach((k, v) -> result.put(k, v * factor));
        return result;
    }

    /**
     * 建筑产出映射为 itemId (无对应道具的产出类型不入账)
     */
    private Map<Integer, Long> toItemMap(Map<BuildingOutputType, Long> resources) {
        Map<Integer, Long> items = new HashMap<>(resources.size());
        for (Map.Entry<BuildingOutputType, Long> en : resources.entrySet()) {
            Integer itemId = toItemId(en.getKey());
            if (itemId == null) {
                continue;
            }
            items.merge(itemId, en.getValue(), Long::sum);
        }
        return items;
    }

    private Integer toItemId(BuildingOutputType type) {
        return switch (type) {
            case GOLD -> ItemUtils.getGoldItemId();
            case POWER -> SimConstant.Item.ID_POWER;
            case AWARENESS -> SimConstant.Item.ID_AWARENESS;
            default -> null;
        };
    }

    /**
     * 计算离线收益最终产出 (入账/展示统一口径): 看广告则按广告倍数放大并叠加广告金币固定值, 否则原样。
     */
    private Map<BuildingOutputType, Long> computeFinalReward(SimPlayerContext ctx, SimOfflineReward reward, boolean watchAd) {
        double multiplier = 1.0;
        if (watchAd && reward.getAdMultiplier() != null && !reward.getAdMultiplier().isEmpty()) {
            multiplier = Double.parseDouble(reward.getAdMultiplier());
        }
        Map<BuildingOutputType, Long> finalReward = scale(reward.getBaseReward(), multiplier);
        if (watchAd) {
            applyAdGoldBonus(ctx, finalReward);
        }
        return finalReward;
    }

    /**
     * 看广告领取离线收益时，对金币部分叠加徽章配置的 WATCH_ADS_ADD_GOLD 固定值。
     */
    private void applyAdGoldBonus(SimPlayerContext ctx, Map<BuildingOutputType, Long> reward) {
        int fixed = medalService.getAdGoldBonusFixed(ctx);
        if (fixed == 0) {
            return;
        }
        Long gold = reward.get(BuildingOutputType.GOLD);
        if (gold == null || gold <= 0) {
            return;
        }
        reward.put(BuildingOutputType.GOLD, gold + fixed);
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
    public int completeBuildingUpgrade(SimPlayerContext ctx, SimCasinoData casino, BuildingData data, long now) {
        if (!data.isUpgradeReady(now)) {
            return Code.PARAM_ERROR;
        }
        int afterLevel = data.getLevel() + 1;
        data.setLevel(afterLevel);
        data.setCdEndTime(0);
        data.setAdClearCount(0);
        data.setProgress(0);
        simCasinoService.addCasinoExp(ctx, 0);
        allianceEventService.onBuildingUpgrade(ctx.playerId(), data.getId(), data.getLevel());

        BuildingUpgradeTableCfg cfg = configCache.getBuildingUpgradeCfg(data.getId(), afterLevel);
        if (cfg != null && cfg.getUpgradeReward() != null && !cfg.getUpgradeReward().isEmpty()) {
            CommonResult<ItemOperationResult> result = playerPackService.addItems(ctx.playerId(), cfg.getUpgradeReward(), AddType.SIM_BUILDING_UPGRADE);
            if (!result.success()) {
                log.warn("建筑升级后添加道具失败 playerId={},buildingId={},afterLevel={}", ctx.playerId(), data.getId(), afterLevel);
            }
        }
        unlockBetSkills(ctx, data, GameDataManager.getBuildingAreaTableCfg(data.getId()));
        log.info("完成建筑升级 playerId={},buildingId={},newLevel={}", casino.getPlayerId(), data.getId(), data.getLevel());
        return Code.SUCCESS;
    }

    /**
     * 完成建筑升级
     */
    private int completeBuildingUpgradeAndReport(SimPlayerContext ctx, SimCasinoData casino, BuildingData data, long now) {
        int code = completeBuildingUpgrade(ctx, casino, data, now);
        if (code == Code.SUCCESS) {
            //主线任务: 升级改变各等级持有量 -> 上报 12208 "拥有 N 个 ≥X 级建筑"
            reportBuildingCounts(ctx);
        }
        return code;
    }

    private ResCompleteBuildingUpgrade completeBuildingUpgradeResponse(BuildingData data) {
        ResCompleteBuildingUpgrade res = new ResCompleteBuildingUpgrade(Code.SUCCESS);
        res.id = data.getId();
        res.level = data.getLevel();
        return res;
    }

    private long applyAllianceSpeedup(long playerId, BuildingData data, long now) {
        if (data == null || data.getCdEndTime() <= 0) {
            return 0;
        }
        long seconds = allianceHelpService.consumeSpeedupSeconds(playerId, data.getId());
        if (!data.isUpgrading(now)) {
            return 0;
        }
        long reduced = data.applySpeedupSeconds(seconds, now);
        if (reduced > 0) {
            log.info("apply alliance building speedup playerId={},buildingId={},seconds={},cdEndTime={}", playerId, data.getId(), reduced, data.getCdEndTime());
        }
        return reduced;
    }

    /**
     * Redis 通知仅负责唤醒；累计秒数仍以 GETDEL 的结果为准。
     */
    public void onAllianceSpeedupPending(SimPlayerContext ctx, int buildingId) {
        SimCasinoData casino = ctx.getCurrentCasino();
        if (casino == null) {
            return;
        }
        BuildingData data = casino.findBuilding(buildingId);
        if (data == null) {
            // 建筑不属于当前场景时保留 Redis 累计值，切换到对应场景后再消费。
            return;
        }
        long now = System.currentTimeMillis();
        applyAllianceSpeedup(ctx.playerId(), data, now);
        if (data.isUpgradeReady(now) && completeBuildingUpgradeAndReport(ctx, casino, data, now) == Code.SUCCESS) {
            ctx.send(completeBuildingUpgradeResponse(data));
        }
    }

    /**
     * 下发场景建筑列表前消费联盟助力抵扣: 帮助者只把秒数累计到 Redis, 求助者所在节点是唯一消费方,
     * 取出为 GETDEL 原子操作, 因此任何下发路径调用都不会重复应用。
     * <p>
     * Redis 通知丢失或玩家不在目标场景时，tick 与建筑列表下发路径负责兜底消费。
     */
    public void applyPendingSpeedup(SimPlayerContext ctx, SimCasinoData casino, long now) {
        if (casino == null || casino.getBuildingData() == null || casino.getBuildingData().isEmpty()) {
            return;
        }
        boolean completed = false;
        for (BuildingData data : casino.getBuildingData().values()) {
            applyAllianceSpeedup(ctx.playerId(), data, now);
            if (data.isUpgradeReady(now) && completeBuildingUpgrade(ctx, casino, data, now) == Code.SUCCESS) {
                completed = true;
            }
        }
        if (completed) {
            reportBuildingCounts(ctx);
        }
    }

    public void completeAllBuildingUpgrade(SimPlayerContext ctx, SimCasinoData casino) {
        if (casino == null || casino.getBuildingData() == null || casino.getBuildingData().isEmpty()) {
            return;
        }

        long now = System.currentTimeMillis();
        casino.getBuildingData().forEach((k, v) -> completeBuildingUpgrade(ctx, casino, v, now));
    }

    /**
     * 获取该建筑指定等级的基础值 (不含主管/雇员加成):
     * 游戏区/休息区为每分钟产出, 管理区为部门属性值。
     */
    public Map<BuildingOutputType, Long> getBaseOutput(int buildingId, int level) {
        BuildingAreaTableCfg buildingAreaTableCfg = GameDataManager.getBuildingAreaTableCfg(buildingId);
        if (buildingAreaTableCfg == null) {
            return Collections.emptyMap();
        }
        List<Integer> typeValues = buildingAreaTableCfg.getTypeValue();
        if (typeValues == null || typeValues.isEmpty()) {
            return Collections.emptyMap();
        }
        BuildingUpgradeTableCfg cfg = configCache.getBuildingUpgradeCfg(buildingId, level);
        if (cfg == null) {
            return Collections.emptyMap();
        }
        Map<BuildingOutputType, Long> map = new HashMap<>(typeValues.size());
        for (Integer typeValue : typeValues) {
            BuildingOutputType outputType = BuildingOutputType.fromCode(typeValue);
            if (outputType == null) {
                continue;
            }
            long output = outputType == BuildingOutputType.CASINO_LEVEL_EXP ? cfg.getUpgradeExp() : cfg.getUpgradeOutput();
            map.put(outputType, output);
        }
        return map;
    }

    private int getBuildingUnlockLangId(List<Integer> list, int index) {
        if (list == null || list.isEmpty()) {
            return 0;
        }

        if (index >= list.size()) {
            return 0;
        }
        return list.get(index);
    }

    public void gmUnlockAllBuilds(SimPlayerContext ctx) {
        for (BuildingAreaTableCfg cfg : GameDataManager.getBuildingAreaTableCfgList()) {
            BuildingData building = ctx.getCurrentCasino().findBuilding(cfg.getId());
            if (building != null) {
                continue;
            }
            unlockAndUpdateBuildData(ctx, cfg.getId(), cfg);

            ResUnlockBuilding res = new ResUnlockBuilding(Code.SUCCESS);
            res.id = cfg.getId();
            ctx.send(res);
            log.info("gm 无视条件解锁建筑 playerId={},buildId={}", ctx.playerId(), cfg.getId());
        }
    }

    public void unlockAndUpdateBuildData(SimPlayerContext ctx, int buildingId, BuildingAreaTableCfg cfg) {
        BuildingData data = new BuildingData();
        data.setId(buildingId);
        data.setLevel(INITIAL_LEVEL);
        ctx.getCurrentCasino().putBuilding(data);
        if (cfg.getUnlockGameId() > 0) {
            simCasinoService.updateCasinoUnlock(ctx, ctx.getCurrentCasino().getCasinoId(), Set.of(cfg.getUnlockGameId()));
        }
        simCasinoService.addCasinoExp(ctx, 0);
        allianceEventService.onBuildingLevel(ctx.playerId(), buildingId, data.getLevel());
        //主线任务: 新建筑改变各等级持有量 -> 上报 12208 "拥有 N 个 ≥X 级建筑"
        reportBuildingCounts(ctx);
        unlockBetSkills(ctx, data, cfg);
    }

    private void unlockBetSkills(SimPlayerContext ctx, BuildingData data, BuildingAreaTableCfg buildingAreaTableCfg) {
        if (buildingAreaTableCfg == null || buildingAreaTableCfg.getUnlockGameId() < 1) {
            return;
        }

        BuildingUpgradeTableCfg buildingUpgradeTableCfg = configCache.getBuildingUpgradeCfg(data.getId(), data.getLevel());
        if (buildingUpgradeTableCfg == null || buildingUpgradeTableCfg.getBet() == null || buildingUpgradeTableCfg.getBet().isEmpty()) {
            return;
        }
        Map.Entry<Integer, Integer> en = buildingUpgradeTableCfg.getBet().entrySet().stream().findFirst().get();
        simSkillService.skillLevelUp(ctx, buildingAreaTableCfg.getUnlockGameId(), en.getKey(), en.getValue());
    }
}
