package com.jjg.game.sim.service;

import com.alibaba.fastjson.JSON;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.listener.ConfigExcelChangeListener;
import com.jjg.game.core.pb.KVInfo;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.PropCfg;
import com.jjg.game.sampledata.bean.ResearchSkillsCfg;
import com.jjg.game.sim.constant.SimConstant;
import com.jjg.game.sim.dao.SimSkillsDao;
import com.jjg.game.sim.data.SimCasinoData;
import com.jjg.game.sim.data.SimPlayerContext;
import com.jjg.game.sim.data.SimSkillsData;
import com.jjg.game.sim.pb.SimPbConverter;
import com.jjg.game.sim.pb.res.ResSimGetSkills;
import com.jjg.game.sim.pb.res.ResSimUpgradeSkill;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * slots 技能服务
 *
 * @author 11
 * @date 2026/5/22
 */
@Service
public class SimSkillService extends AbstractSkillService implements ConfigExcelChangeListener {

    @Autowired
    private SimSkillsDao simSkillsDao;
    @Autowired
    private SimConfigCacheService simConfigCacheService;

    /**
     * 初始时解锁技能
     *
     * @param ctx
     * @param casinoId
     */
    public void initUnlock(SimPlayerContext ctx, int casinoId) {
        //获取casinoId解锁的游戏
        Set<Integer> unlockGameSet = simConfigCacheService.getUnlockGameByRegionId(casinoId);
        if (unlockGameSet.isEmpty()) {
            return;
        }

        Map<Integer, SimSkillsData> skillsMap = ctx.getSkillsDataMap();
        for (PropCfg cfg : GameDataManager.getPropCfgList()) {
            if (cfg.getSkillId() != null && !cfg.getSkillId().isEmpty()) {
                continue;
            }
            if (!unlockGameSet.contains(cfg.getGameType())) {
                continue;
            }

            SimSkillsData data = skillsMap.get(cfg.getGameType());
            if (data != null) {
                Integer beforeLevel = data.findSkilLevelByPropId(cfg.getId());
                if (beforeLevel != null) {
                    continue;
                }
            } else {
                data = new SimSkillsData();
                data.setPlayerId(ctx.playerId());
                data.setGameType(cfg.getGameType());
                skillsMap.put(cfg.getGameType(), data);
            }
            data.changeSkillLevel(cfg.getId(), 0);
        }
    }

    /**
     * 加载技能
     */
    public void onLoadSlotsSkills(SimPlayerContext ctx, int gameType) {
        ResSimGetSkills res = new ResSimGetSkills(Code.SUCCESS);
        try {
            //加载技能数据
            SimSkillsData data = ctx.getSkillData(gameType);
            if (data == null) {
                data = simSkillsDao.findByGameType(ctx.playerId(), gameType);
                if (data != null) {
                    ctx.getSkillsDataMap().put(data.getGameType(), data);
                }
            }

            if (data != null) {
                res.skills = new ArrayList<>();
                res.skills.add(SimPbConverter.toGameSkills(data));
            }

            if (ctx.getSimBaseData().getResearchPointMap() != null && !ctx.getSimBaseData().getResearchPointMap().isEmpty()) {
                res.researchPoints = new ArrayList<>();
                for (Map.Entry<Integer, Integer> en : ctx.getSimBaseData().getResearchPointMap().entrySet()) {
                    KVInfo kvInfo = new KVInfo();
                    kvInfo.key = en.getKey();
                    kvInfo.value = en.getValue();
                    res.researchPoints.add(kvInfo);
                }
            }

            log.info("玩家加载技能 playerId={},res={}", ctx.playerId(), JSON.toJSONString(res));
        } catch (Exception e) {
            log.error("", e);
            res.code = Code.EXCEPTION;
        }
        ctx.send(res);
    }

    /**
     * 升级技能
     */
    public void onUpgradeSkill(SimPlayerContext ctx, int gameType, int skillPropId) {
        ResSimUpgradeSkill res = new ResSimUpgradeSkill(Code.SUCCESS);
        try {
            SimSkillsData skillData = ctx.getSkillData(gameType);
            if (skillData == null) {
                log.warn("升级技能失败: simSkillsData 不存在 playerId={}", ctx.playerId());
                res.code = Code.NOT_FOUND;
                ctx.send(res);
                return;
            }
            PropCfg propCfg = GameDataManager.getPropCfg(skillPropId);
            if (propCfg == null) {
                log.warn("升级技能失败，未找到prop配置 playerId={},skillPropId={}", skillData.getPlayerId(), skillPropId);
                res.code = Code.NOT_FOUND;
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

            Integer beforeLevel = skillData.findSkilLevelByPropId(skillPropId);
            if (beforeLevel == null) {
                log.warn("升级技能失败，该技能还未解锁 playerId={},propId={}", skillData.getPlayerId(), skillPropId);
                res.code = Code.PARAM_ERROR;
                ctx.send(res);
                return;
            }

            //新等级的配置
            ResearchSkillsCfg newLevelCfg = levelMap.get(beforeLevel + 1);
            if (newLevelCfg == null) {
                log.warn("升级技能失败，该技能已达到上限 playerId={},propId={}", skillData.getPlayerId(), skillPropId);
                res.code = Code.NOT_FOUND;
                ctx.send(res);
                return;
            }

            //检查新等级所需要的研究点
            if (newLevelCfg.getResearchPoints() == null || newLevelCfg.getResearchPoints().isEmpty()) {
                skillData.changeSkillLevel(skillPropId, newLevelCfg.getGrade());
                log.warn("该技能等级升级无需研究点，升级技能成功 playerId={},propId={},newLevelCfgId={}", skillData.getPlayerId(), skillPropId, newLevelCfg.getId());
                res.code = Code.SUCCESS;
                ctx.send(res);
                return;
            }

            //检查研究点是否足够
            for (Map.Entry<Integer, Integer> en : newLevelCfg.getResearchPoints().entrySet()) {
                int itemId = en.getKey();
                int type = 0;
                if (itemId == SimConstant.Item.ID_RESEARCH_POINT) {
                    type = SimConstant.ResearchPoint.NORMAL_TPYE;
                } else if (itemId == SimConstant.Item.ID_RARE_RESEARCH_POINT) {
                    type = SimConstant.ResearchPoint.RARE_TPYE;
                } else {
                    log.warn("研究点道具id错误 playerId={},propId={},itemId={}", skillData.getPlayerId(), skillPropId, itemId);
                    continue;
                }
                int researchPoint = ctx.getSimBaseData().findResearchPoint(type);
                if (researchPoint < en.getValue()) {
                    log.warn("升级技能失败，研究点不足 playerId={},propId={},newLevelCfgId={},researchPoint={}", skillData.getPlayerId(), skillPropId, newLevelCfg.getId(), researchPoint);
                    res.code = Code.NOT_ENOUGH;
                    ctx.send(res);
                    return;
                }
            }

            //扣除研究点
            for (Map.Entry<Integer, Integer> en : newLevelCfg.getResearchPoints().entrySet()) {
                ctx.getSimBaseData().deductResearchPoint(en.getKey(), en.getValue());
            }
            skillData.changeSkillLevel(skillPropId, newLevelCfg.getGrade());

            res.gameType = gameType;
            res.skillId = skillPropId;
            res.nowLevel = newLevelCfg.getGrade();

            res.researchPoints = new ArrayList<>();
            for (Map.Entry<Integer, Integer> en : ctx.getSimBaseData().getResearchPointMap().entrySet()) {
                KVInfo kvInfo = new KVInfo();
                kvInfo.key = en.getKey();
                kvInfo.value = en.getValue();
                res.researchPoints.add(kvInfo);
            }

            //新解锁的技能
            List<PropCfg> propCfgList = simConfigCacheService.getPropCfgList(gameType);
            if (propCfgList != null && !propCfgList.isEmpty()) {
                for (PropCfg cfg : propCfgList) {
                    //检查该技能是否解锁
                    Integer level = skillData.findSkilLevelByPropId(cfg.getId());
                    if (level != null) {
                        continue;
                    }
                    if (cfg.getSkillId() == null || cfg.getSkillId().isEmpty()) {
                        skillData.changeSkillLevel(cfg.getId(), 0);
                        res.newUnlockSkills.add(cfg.getId());
                    } else {
                        for (Map.Entry<Integer, Integer> en : cfg.getSkillId().entrySet()) {
                            Integer tmpLevel = skillData.findSkilLevelByPropId(en.getKey());
                            if (tmpLevel == null || tmpLevel < en.getValue()) {
                                continue;
                            }
                            skillData.changeSkillLevel(cfg.getId(), 0);
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

            result.code = addSkill(data, skillId);
            if (result.code == Code.SUCCESS) {
                ctx.addSkillData(data);
                result.data = data.getSkillsMap();
            }
        } catch (Exception e) {
            log.error("", e);
            result.code = Code.EXCEPTION;
        }
        return result;
    }
}
