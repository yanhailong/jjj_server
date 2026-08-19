package com.jjg.game.slots.service;

import com.jjg.game.core.data.PropInfo;
import com.jjg.game.core.utils.PropUtil;
import com.jjg.game.sampledata.bean.ResearchSkillsCfg;
import com.jjg.game.season.data.SeasonSlotsSessionData;
import com.jjg.game.sim.data.SlotsSkillEffectData;
import com.jjg.game.sim.service.AbstractSkillService;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * @author 11
 * @date 2026/5/25
 */
@Service
public class SlotsSkillService extends AbstractSkillService {

    /**
     * 将玩家技能 specialMode (libType -> weightDelta) 累加到 typeProp 权重上，返回修改后的克隆
     */
    protected PropInfo applySpecialModeSkillBonus(PropInfo propInfo, List<ResearchSkillsCfg> skills) {
        //将玩家技能中所有影响libType权重的加成合并
        Map<Integer, Integer> deltaMap = new HashMap<>();
        for (ResearchSkillsCfg cfg : skills) {
            Map<Integer, Integer> specialMode = cfg.getSpecialMode();
            if (specialMode == null || specialMode.isEmpty()) {
                continue;
            }
            specialMode.forEach((key, value) -> deltaMap.merge(key, value, Integer::sum));
        }
        return PropUtil.applyPropInfoDelta(propInfo, deltaMap);
    }

    /**
     * 将玩家技能 winRate + specialModeProbUp 中匹配 libType 的 (sectionIdx -> delta) 累加到 section 权重上，返回修改后的克隆
     */
    protected PropInfo applySectionSkillBonus(PropInfo propInfo, int libType, List<ResearchSkillsCfg> skillCfgList) {
        //将玩家技能中所有匹配libType的 section 权重加成合并
        Map<Integer, Integer> deltaMap = new HashMap<>();
        for (ResearchSkillsCfg cfg : skillCfgList) {
            accumulateSectionDelta(deltaMap, cfg.getWinRate(), libType);
            accumulateSectionDelta(deltaMap, cfg.getSpecialModeProbUp(), libType);
        }
        return PropUtil.applyPropInfoDelta(propInfo, deltaMap);
    }

    /**
     * 合并配置中的区间权重修改
     *
     * @param deltaMap
     * @param cfgPropChangMap
     * @param libType
     */
    private void accumulateSectionDelta(Map<Integer, Integer> deltaMap, Map<Integer, Map<Integer, Integer>> cfgPropChangMap, int libType) {
        if (cfgPropChangMap == null || cfgPropChangMap.isEmpty()) {
            return;
        }
        Map<Integer, Integer> inner = cfgPropChangMap.get(libType);
        if (inner == null || inner.isEmpty()) {
            return;
        }
        inner.forEach((sectionIdx, delta) -> {
            if (sectionIdx == null || delta == null) {
                return;
            }
            deltaMap.merge(sectionIdx, delta, Integer::sum);
        });
    }

    /**
     * 使用技能
     *
     * @param propInfo
     * @return
     */
    public PropInfo useLibTypeSkill(int gameType, Map<Integer, Integer> skillsMap, PropInfo propInfo) {
        List<ResearchSkillsCfg> skillsCfgList = transSkills(gameType, skillsMap);
        if (skillsCfgList.isEmpty()) {
            return propInfo;
        }

        PropInfo newPropInfo = applySpecialModeSkillBonus(propInfo, skillsCfgList);
        if (newPropInfo == null) {
            return propInfo;
        }
        return newPropInfo;
    }

    /**
     * 使用技能
     *
     * @param propInfo
     * @return
     */
    public PropInfo useSectionSkill(int gameType, Map<Integer, Integer> skillsMap, PropInfo propInfo, int libType) {
        List<ResearchSkillsCfg> skillsCfgList = transSkills(gameType, skillsMap);
        if (skillsCfgList.isEmpty()) {
            return propInfo;
        }

        PropInfo newPropInfo = applySectionSkillBonus(propInfo, libType, skillsCfgList);
        if (newPropInfo == null) {
            return propInfo;
        }
        return newPropInfo;
    }

    /** 应用赛季宝石的 specialMode 效果。 */
    public PropInfo useSeasonGemLibTypeBonus(SeasonSlotsSessionData sessionData, PropInfo propInfo) {
        return sessionData == null ? propInfo
                : applyLibTypeWeightBonus(sessionData.getLibTypeWeightDelta(), propInfo);
    }

    /** 应用赛季宝石的 winRate 与 specialModeProbUp 效果。 */
    public PropInfo useSeasonGemSectionBonus(SeasonSlotsSessionData sessionData, PropInfo propInfo, int libType) {
        return sessionData == null ? propInfo
                : applySectionWeightBonus(sessionData.getSectionWeightDelta(), propInfo, libType);
    }

    /** 应用游客羁绊的 specialMode 效果。 */
    public PropInfo useSkillEffectLibTypeBonus(SlotsSkillEffectData effectData, PropInfo propInfo) {
        return effectData == null ? propInfo
                : applyLibTypeWeightBonus(effectData.getLibTypeWeightDelta(), propInfo);
    }

    /** 应用游客羁绊的 winRate 与 specialModeProbUp 效果。 */
    public PropInfo useSkillEffectSectionBonus(SlotsSkillEffectData effectData, PropInfo propInfo, int libType) {
        return effectData == null ? propInfo
                : applySectionWeightBonus(effectData.getSectionWeightDelta(), propInfo, libType);
    }

    private PropInfo applyLibTypeWeightBonus(Map<Integer, Integer> delta, PropInfo propInfo) {
        if (delta == null || delta.isEmpty()) {
            return propInfo;
        }
        return PropUtil.applyPropInfoDelta(propInfo, delta);
    }

    private PropInfo applySectionWeightBonus(Map<Integer, Map<Integer, Integer>> delta,
                                             PropInfo propInfo, int libType) {
        if (delta == null || delta.isEmpty()) {
            return propInfo;
        }
        Map<Integer, Integer> sectionDelta = delta.get(libType);
        if (sectionDelta == null || sectionDelta.isEmpty()) {
            return propInfo;
        }
        return PropUtil.applyPropInfoDelta(propInfo, sectionDelta);
    }

    /**
     * 将玩家身上的 propId->level 转化成 List<ResearchSkillsCfg>
     *
     * @return
     */
    public List<ResearchSkillsCfg> transSkills(int gameType, Map<Integer, Integer> skillsMap) {
        Map<Integer, Map<Integer, ResearchSkillsCfg>> cfgMap = this.skillsCfgMap.get(gameType);
        if (skillsMap == null || skillsMap.isEmpty() || cfgMap == null || cfgMap.isEmpty()) {
            return Collections.emptyList();
        }

        List<ResearchSkillsCfg> list = new ArrayList<>();
        for (Map.Entry<Integer, Integer> en : skillsMap.entrySet()) {
            Map<Integer, ResearchSkillsCfg> tmpMap = cfgMap.get(en.getKey());
            if (tmpMap != null && !tmpMap.isEmpty()) {
                ResearchSkillsCfg cfg = tmpMap.get(en.getValue());
                if (cfg != null) {
                    list.add(cfg);
                }
            }
        }
        return list;
    }
}
