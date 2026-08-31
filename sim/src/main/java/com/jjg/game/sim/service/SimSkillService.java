package com.jjg.game.sim.service;

import com.alibaba.fastjson.JSON;
import com.jjg.game.alliance.service.AllianceEventService;
import com.jjg.game.common.pb.ItemInfo;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.PlayerPack;
import com.jjg.game.core.service.PlayerPackService;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BuildingAreaTableCfg;
import com.jjg.game.sampledata.bean.ItemCfg;
import com.jjg.game.sampledata.bean.PropCfg;
import com.jjg.game.sampledata.bean.ResearchSkillsCfg;
import com.jjg.game.sim.dao.SimSkillsDao;
import com.jjg.game.sim.data.BuildingData;
import com.jjg.game.sim.data.SimPlayerContext;
import com.jjg.game.sim.data.SimSkillsData;
import com.jjg.game.sim.data.SkillDetailData;
import com.jjg.game.sim.data.TogetherPlaySkillEffectData;
import com.jjg.game.sim.pb.SimPbConverter;
import com.jjg.game.sim.pb.res.ResSimGetSkills;
import com.jjg.game.sim.pb.res.ResSimUpgradeSkill;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * slots 技能服务
 *
 * @author 11
 * @date 2026/5/22
 */
@Service
public class SimSkillService extends AbstractSkillService {

    @Autowired
    private SimSkillsDao simSkillsDao;
    @Autowired
    private SimConfigCacheService simConfigCacheService;
    @Autowired
    private AllianceEventService allianceEventService;
    @Autowired
    private PlayerPackService playerPackService;

    //每个技能的最大等级
    private Map<Integer, Integer> maxLevelMap = new HashMap<>();

    /** 技能红点只读预检，不解锁技能、不扣研究点。返回null表示当前不可升级。 */
    public Map<Integer, Long> redDotUpgradeCost(SimPlayerContext ctx, int gameType, int propId) {
        SimSkillsData data = ctx.getSkillData(gameType);
        SkillDetailData detail = data == null ? null : data.findSkilLevelByPropId(propId);
        PropCfg prop = GameDataManager.getPropCfg(propId);
        if (detail == null || prop == null || skillGameType(prop) != gameType) return null;
        ResearchSkillsCfg next = getResearchSkillsCfg(gameType, propId, detail.getLevel() + 1);
        if (next == null) return null;
        if (gameType != GLOBAL_GAME_TYPE) {
            BuildingAreaTableCfg area = simConfigCacheService.getBuildingAreaTableCfgByGameType(gameType);
            BuildingData building = area == null || ctx.getCurrentCasino() == null ? null : ctx.getCurrentCasino().findBuilding(area.getId());
            if (building == null || building.getLevel() < next.getBuildingLevel()) return null;
        }
        Map<Integer, Long> cost = new HashMap<>();
        if (next.getResearchPoints() != null) {
            for (Map.Entry<Integer, Integer> entry : next.getResearchPoints().entrySet()) {
                if (entry.getKey() == null || !canUseResearchPoint(gameType, entry.getKey())) return null;
                if (entry.getValue() != null && entry.getValue() > 0) cost.put(entry.getKey(), entry.getValue().longValue());
            }
        }
        return cost;
    }

    /**
     * 登录加载技能 (player 全量)。须在加载场景数据之前调用: initUnlock 依据已入内存的技能等级决定是否补解锁。
     */
    public void loadSkillsData(SimPlayerContext ctx) {
        for (SimSkillsData data : simSkillsDao.findByPlayerId(ctx.playerId())) {
            ctx.getSkillsDataMap().putIfAbsent(data.getGameType(), data);
        }
    }

    /**
     * 初始时解锁技能
     *
     * @param ctx
     * @param casinoId
     */
    public void initUnlock(SimPlayerContext ctx, int casinoId) {
        //获取casinoId配置的可研发游戏
        Set<Integer> researchGames = simConfigCacheService.getResearchGamesByRegionId(casinoId);

        for (PropCfg cfg : GameDataManager.getPropCfgList()) {
            if (cfg.getSkillId() != null && !cfg.getSkillId().isEmpty()) {
                continue;
            }

            int gameType = skillGameType(cfg);
            if (gameType == GLOBAL_GAME_TYPE) {
                unlockInitialSkill(ctx, cfg, gameType);
            } else if (researchGames.contains(gameType)) {
                unlockInitialSkill(ctx, cfg, gameType);
            }
        }
    }

    private void unlockInitialSkill(SimPlayerContext ctx, PropCfg cfg, int gameType) {
        Map<Integer, SimSkillsData> skillsMap = ctx.getSkillsDataMap();
        SimSkillsData data = skillsMap.get(gameType);
        if (data != null) {
            SkillDetailData skillDetailData = data.findSkilLevelByPropId(cfg.getId());
            if (skillDetailData != null) {
                return;
            }
        } else {
            data = new SimSkillsData();
            data.setPlayerId(ctx.playerId());
            data.setGameType(gameType);
            skillsMap.put(gameType, data);
        }
        super.changeSkillLevel(ctx, data, cfg.getId(), 0);
    }

    /**
     * 加载技能
     */
    public void onLoadSlotsSkills(SimPlayerContext ctx, int gameType) {
        ResSimGetSkills res = new ResSimGetSkills(Code.SUCCESS);
        try {
            res.skills = new ArrayList<>();
            SimSkillsData data = loadSkillData(ctx, gameType);
            if (data != null) {
                res.skills.add(SimPbConverter.toGameSkills(data));
            }
            if (gameType != GLOBAL_GAME_TYPE) {
                SimSkillsData globalData = loadSkillData(ctx, GLOBAL_GAME_TYPE);
                if (globalData != null) {
                    res.skills.add(SimPbConverter.toGameSkills(globalData));
                }
            }
            res.researchPoints = getResearchPoints(ctx.playerId(), gameType);
            log.info("玩家加载技能 playerId={},res={}", ctx.playerId(), JSON.toJSONString(res));
        } catch (Exception e) {
            log.error("", e);
            res.code = Code.EXCEPTION;
        }
        ctx.send(res);
    }

    private SimSkillsData loadSkillData(SimPlayerContext ctx, int gameType) {
        SimSkillsData data = ctx.getSkillData(gameType);
        if (data == null) {
            data = simSkillsDao.findByGameType(ctx.playerId(), gameType);
            if (data != null) {
                ctx.getSkillsDataMap().put(data.getGameType(), data);
            }
        }
        return data;
    }

    /**
     * 升级技能
     */
    public void onUpgradeSkill(SimPlayerContext ctx, int gameType, int skillPropId) {
        ResSimUpgradeSkill res = new ResSimUpgradeSkill(Code.SUCCESS);
        try {
            PropCfg propCfg = GameDataManager.getPropCfg(skillPropId);
            if (propCfg == null) {
                log.warn("升级技能失败，未找到prop配置 playerId={},skillPropId={}", ctx.playerId(), skillPropId);
                res.code = Code.NOT_FOUND;
                ctx.send(res);
                return;
            }
            SimSkillsData skillData;
            if (propCfg.getSkillTypeId() == 1) {
                skillData = ctx.getSkillData(0);
                gameType = GLOBAL_GAME_TYPE;
            } else {
                skillData = ctx.getSkillData(gameType);
            }
            if (skillData == null) {
                log.warn("升级技能失败: simSkillsData 不存在 playerId={},gameType={},skillPropId={},propType={}", ctx.playerId(), gameType, skillPropId, propCfg.getSkillTypeId());
                res.code = Code.NOT_FOUND;
                ctx.send(res);
                return;
            }

            if (skillGameType(propCfg) != gameType) {
                log.warn("升级技能失败，技能类型与请求不匹配 playerId={},propId={},gameType={}",
                        skillData.getPlayerId(), skillPropId, gameType);
                res.code = Code.PARAM_ERROR;
                ctx.send(res);
                return;
            }

            Map<Integer, Map<Integer, ResearchSkillsCfg>> cfgMap = this.skillsCfgMap.get(skillData.getGameType());
            if (cfgMap == null || cfgMap.isEmpty()) {
                log.warn("升级技能失败，未找到技能配置 playerId={},propId={}", skillData.getPlayerId(), skillPropId);
                res.code = Code.NOT_FOUND;
                ctx.send(res);
                return;
            }

            Map<Integer, ResearchSkillsCfg> levelMap = cfgMap.get(skillPropId);
            if (levelMap == null || levelMap.isEmpty()) {
                log.warn("升级技能失败，未找到技能配置2 playerId={},propId={}", skillData.getPlayerId(), skillPropId);
                res.code = Code.NOT_FOUND;
                ctx.send(res);
                return;
            }

            SkillDetailData skillDetailData = skillData.findSkilLevelByPropId(skillPropId);
            if (skillDetailData == null) {
                log.warn("升级技能失败，该技能还未解锁 playerId={},propId={}", skillData.getPlayerId(), skillPropId);
                res.code = Code.PARAM_ERROR;
                ctx.send(res);
                return;
            }

            //新等级的配置
            ResearchSkillsCfg newLevelCfg = levelMap.get(skillDetailData.getLevel() + 1);
            if (newLevelCfg == null) {
                log.warn("升级技能失败，该技能已达到上限 playerId={},propId={}", skillData.getPlayerId(), skillPropId);
                res.code = Code.NOT_FOUND;
                ctx.send(res);
                return;
            }

            if (gameType != GLOBAL_GAME_TYPE) {
                BuildingAreaTableCfg buildingAreaTableCfg = simConfigCacheService.getBuildingAreaTableCfgByGameType(gameType);
                if (buildingAreaTableCfg == null) {
                    log.warn("升级技能失败，根据游戏未找到配置 playerId={},propId={},gameType={}", skillData.getPlayerId(), skillPropId, gameType);
                    res.code = Code.PARAM_ERROR;
                    ctx.send(res);
                    return;
                }

                BuildingData building = ctx.getCurrentCasino().findBuilding(buildingAreaTableCfg.getId());
                if (building == null) {
                    log.warn("升级技能失败，未找到对应的建筑信息 playerId={},propId={},gameType={},buildingId={}", skillData.getPlayerId(), skillPropId, gameType, buildingAreaTableCfg.getId());
                    res.code = Code.PARAM_ERROR;
                    ctx.send(res);
                    return;
                }

                if (building.getLevel() < newLevelCfg.getBuildingLevel()) {
                    log.warn("升级技能失败，建筑等级不足 playerId={},propId={},gameType={},buildingId={},buildingLevel={},requiredBuildingLevel={}",
                            skillData.getPlayerId(), skillPropId, gameType, buildingAreaTableCfg.getId(), building.getLevel(), newLevelCfg.getBuildingLevel());
                    res.code = Code.BUILDING_LEVEL_LIMIT;
                    ctx.send(res);
                    return;
                }
            }

            //检查新等级所需要的研究点
            if (newLevelCfg.getResearchPoints() != null && !newLevelCfg.getResearchPoints().isEmpty()) {
                //扣除研究点 (研究点已按 itemId 存于背包, 先校验再扣除, 不足整体失败)
                Map<Integer, Long> costMap = new HashMap<>();
                for (Map.Entry<Integer, Integer> en : newLevelCfg.getResearchPoints().entrySet()) {
                    if (en.getValue() != null && en.getValue() > 0) {
                        costMap.put(en.getKey(), en.getValue().longValue());
                    }
                }
                if (!playerPackService.removeItems(ctx.getPlayer(), costMap, AddType.SIM_SKILL_UPGRADE, "skillUpgrade:" + skillPropId).success()) {
                    log.warn("升级技能失败，道具不足 playerId={},propId={},newLevelCfgId={}", skillData.getPlayerId(), skillPropId, newLevelCfg.getId());
                    res.code = Code.NOT_ENOUGH;
                    ctx.send(res);
                    return;
                }
            }
            super.changeSkillLevel(ctx, skillData, skillPropId, newLevelCfg.getGrade());
            refreshBuildOutput(skillData, skillPropId);
            //联盟任务: 技能研究次数 (param=游戏类型, 供 0=任意/指定游戏 过滤)
            allianceEventService.onGameResearch(ctx.playerId(), gameType);

            res.gameType = gameType;
            res.skillId = skillPropId;
            res.nowLevel = newLevelCfg.getGrade();

            res.researchPoints = getResearchPoints(ctx.playerId(), gameType);

            //新解锁的技能
            List<PropCfg> propCfgList = simConfigCacheService.getPropCfgList(gameType);
            if (propCfgList != null && !propCfgList.isEmpty()) {
                res.newUnlockSkills = new ArrayList<>();
                for (PropCfg cfg : propCfgList) {
                    //检查该技能是否解锁
                    SkillDetailData tmpSkillDetailData = skillData.findSkilLevelByPropId(cfg.getId());
                    if (tmpSkillDetailData != null) {
                        continue;
                    }
                    if (cfg.getSkillId() == null || cfg.getSkillId().isEmpty()) {
                        super.changeSkillLevel(ctx, skillData, cfg.getId(), 0);
                        res.newUnlockSkills.add(cfg.getId());
                    } else {
                        for (Map.Entry<Integer, Integer> en : cfg.getSkillId().entrySet()) {
                            tmpSkillDetailData = skillData.findSkilLevelByPropId(en.getKey());
                            if (tmpSkillDetailData == null || tmpSkillDetailData.getLevel() < en.getValue()) {
                                continue;
                            }
                            super.changeSkillLevel(ctx, skillData, cfg.getId(), 0);
                            res.newUnlockSkills.add(cfg.getId());
                        }
                    }
                }
            }

            log.info("玩家技能升级成功 playerId={},propId={},newLevel={}", skillData.getPlayerId(), skillPropId, newLevelCfg.getGrade());
        } catch (Exception e) {
            log.error("", e);
            res.code = Code.EXCEPTION;
        }
        ctx.send(res);
    }

    private boolean canUseResearchPoint(int gameType, int itemId) {
        ItemCfg normalItemCfg = simConfigCacheService.getResearchPointItemCfg(0);
        if (normalItemCfg != null && normalItemCfg.getId() == itemId) {
            return true;
        }
        ItemCfg exclusiveItemCfg = simConfigCacheService.getResearchPointItemCfg(gameType);
        return exclusiveItemCfg != null && exclusiveItemCfg.getId() == itemId;
    }

    /**
     * 获取普通研究点和专属研究点
     *
     * @param playerId
     * @param gameType
     * @return
     */
    private List<ItemInfo> getResearchPoints(long playerId, int gameType) {
        PlayerPack playerPack = playerPackService.getFromAllDB(playerId);
        if (playerPack == null) {
            return null;
        }

        List<ItemInfo> list = new ArrayList<>();
        //普通研究点
        ItemCfg normalItemCfg = simConfigCacheService.getResearchPointItemCfg(0);
        if (normalItemCfg != null) {
            ItemInfo normalItemInfo = new ItemInfo();
            normalItemInfo.itemId = normalItemCfg.getId();
            normalItemInfo.count = playerPack.getItemCount(normalItemCfg.getId());
            list.add(normalItemInfo);
        }

        //专属研究点
        ItemCfg exclusiveItemCfg = gameType == GLOBAL_GAME_TYPE
                ? null : simConfigCacheService.getResearchPointItemCfg(gameType);
        if (exclusiveItemCfg != null) {
            ItemInfo exclusiveItemInfo = new ItemInfo();
            exclusiveItemInfo.itemId = exclusiveItemCfg.getId();
            exclusiveItemInfo.count = playerPack.getItemCount(exclusiveItemCfg.getId());
            list.add(exclusiveItemInfo);
        }
        return list;
    }

    /**
     * 计算玩家战力: 遍历所有游戏的技能 (propId -> level), 累加对应 ResearchSkillsCfg 的战力值。
     */
    public int computeCombatPower(SimPlayerContext ctx) {
        Map<Integer, SimSkillsData> skillsDataMap = ctx.getSkillsDataMap();
        if (skillsDataMap == null || skillsDataMap.isEmpty()) {
            return 0;
        }
        int total = 0;
        for (SimSkillsData skillsData : skillsDataMap.values()) {
            total += oneGameCombatPower(skillsData);
        }
        return total;
    }

    /**
     * 单个游戏计算出来的战力
     *
     * @param skillsData
     * @return
     */
    public int oneGameCombatPower(SimSkillsData skillsData) {
        if (skillsData.getGameType() == GLOBAL_GAME_TYPE) {
            return 0;
        }
        Map<Integer, SkillDetailData> skillsMap = skillsData.getSkillsMap();
        if (skillsMap == null || skillsMap.isEmpty()) {
            return 0;
        }
        int total = 0;
        for (Map.Entry<Integer, SkillDetailData> en : skillsMap.entrySet()) {
            ResearchSkillsCfg cfg = getResearchSkillsCfg(skillsData.getGameType(), en.getKey(), en.getValue().getLevel());
            if (cfg != null) {
                total += cfg.getCombatPower();
            }
        }
        return total;
    }

    public CommonResult<Map<Integer, Integer>> skillLevelUp(SimPlayerContext ctx, int gameType, int skillPropId, int level) {
        ResearchSkillsCfg cfg = getResearchSkillsCfg(gameType, skillPropId, level);
        if (cfg == null) {
            return new CommonResult<>(Code.NOT_FOUND);
        }
        return skillLevelUp(ctx, gameType, cfg.getId());
    }

    public CommonResult<Map<Integer, Integer>> skillLevelUp(SimPlayerContext ctx, int gameType, int skillId) {
        CommonResult<Map<Integer, Integer>> result = new CommonResult<>(Code.SUCCESS);
        try {
            //加载技能数据
            SimSkillsData data = ctx.getSkillData(gameType);
            if (data == null) {
                data = simSkillsDao.findByGameType(ctx.playerId(), gameType);
                if (data != null) {
                    ctx.getSkillsDataMap().put(data.getGameType(), data);
                } else {
                    data = new SimSkillsData();
                    data.setPlayerId(ctx.playerId());
                    data.setGameType(gameType);
                }
            }

            result.code = addSkill(ctx, data, skillId);
            if (result.code == Code.SUCCESS) {
                ctx.addSkillData(data);

                result.data = new HashMap<>();
                for (Map.Entry<Integer, SkillDetailData> en : data.getSkillsMap().entrySet()) {
                    result.data.put(en.getKey(), en.getValue().getLevel());
                }
            }
        } catch (Exception e) {
            log.error("", e);
            result.code = Code.EXCEPTION;
        }
        return result;
    }

    @Override
    public int addSkill(SimPlayerContext ctx, SimSkillsData simSkillsData, int skillId) {
        ResearchSkillsCfg cfg = GameDataManager.getResearchSkillsCfg(skillId);
        int code = super.addSkill(ctx, simSkillsData, skillId);
        if (code == Code.SUCCESS) {
            refreshBuildOutput(simSkillsData, cfg.getAttr());
        }
        return code;
    }

    /**
     * 添加建筑产出
     *
     * @param skillPropId
     */
    private void refreshBuildOutput(SimSkillsData skillData, int skillPropId) {
        SkillDetailData skillDetailData = skillData.getSkillsMap().get(skillPropId);
        if (skillDetailData == null) {
            return;
        }

        Integer maxLevel = this.maxLevelMap.get(skillPropId);
        PropCfg propCfg = GameDataManager.getPropCfg(skillPropId);
        int output = maxLevel != null && skillDetailData.getLevel() >= maxLevel && propCfg != null
                ? propCfg.getUpgradeOutput() : 0;
        skillDetailData.setAddOutPut(output);
    }

    public TogetherPlaySkillEffectData togetherPlaySkillEffect(SimSkillsData skillData) {
        TogetherPlaySkillEffectData effect = new TogetherPlaySkillEffectData();
        if (skillData == null || skillData.getGameType() != GLOBAL_GAME_TYPE
                || skillData.getSkillsMap() == null) {
            return effect;
        }
        int commissionUsers = 0;
        int winCommission = 0;
        for (Map.Entry<Integer, SkillDetailData> en : skillData.getSkillsMap().entrySet()) {
            SkillDetailData detail = en.getValue();
            if (detail == null) {
                continue;
            }
            ResearchSkillsCfg cfg = getResearchSkillsCfg(
                    GLOBAL_GAME_TYPE, en.getKey(), detail.getLevel());
            if (cfg != null) {
                commissionUsers += cfg.getCommissionUsers();
                winCommission += cfg.getWinCommission();
            }
        }
        effect.setCommissionUsers(commissionUsers);
        effect.setWinCommission(winCommission);
        return effect;
    }

    @Override
    public void loadResearchSkillConfig() {
        Map<Integer, Map<Integer, Map<Integer, ResearchSkillsCfg>>> tmp = new HashMap<>();

        Map<Integer, Integer> tmpMaxLevelMap = new HashMap<>();
        for (ResearchSkillsCfg cfg : GameDataManager.getResearchSkillsCfgList()) {
            Map<Integer, Map<Integer, ResearchSkillsCfg>> tmpMap1 = tmp.computeIfAbsent(cfg.getGameType(), k -> new HashMap<>());
            Map<Integer, ResearchSkillsCfg> tmpMap2 = tmpMap1.computeIfAbsent(cfg.getAttr(), k -> new HashMap<>());

            tmpMap2.put(cfg.getGrade(), cfg);

            Integer before = tmpMaxLevelMap.get(cfg.getAttr());
            if (before == null || before < cfg.getGrade()) {
                tmpMaxLevelMap.put(cfg.getAttr(), cfg.getGrade());
            }

        }
        this.skillsCfgMap = tmp;
        this.maxLevelMap = tmpMaxLevelMap;
    }
}
