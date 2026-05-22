package com.jjg.game.sim.service;

import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.PropInfo;
import com.jjg.game.core.listener.ConfigExcelChangeListener;
import com.jjg.game.core.utils.PropUtil;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.PropCfg;
import com.jjg.game.sampledata.bean.ResearchSkillsCfg;
import com.jjg.game.sim.SimManager;
import com.jjg.game.sim.constant.SimConstant;
import com.jjg.game.sim.dao.SimSkillsDao;
import com.jjg.game.sim.data.SimPlayerGameData;
import com.jjg.game.sim.data.SimSkillsData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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
public class SimSkillService implements ConfigExcelChangeListener {
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private SimSkillsDao simSkillsDao;
    @Autowired
    private SimManager simManager;

    //技能道具id  gameType -> propId集合
    private Map<Integer, Set<Integer>> skillPropIdsMap = new HashMap<>();
    //技能配置  gameType -> propId -> grade -> cfg
    private Map<Integer, Map<Integer, Map<Integer, ResearchSkillsCfg>>> skillsCfgMap = new HashMap<>();

    @Override
    public void initSampleCallbackCollector() {
        addInitSampleFileObserveWithCallBack(PropCfg.EXCEL_NAME, this::loadPropConfig);
        addInitSampleFileObserveWithCallBack(ResearchSkillsCfg.EXCEL_NAME, this::loadResearchSkillConfig);
    }

    @Override
    public void changeSampleCallbackCollector() {
        addChangeSampleFileObserveWithCallBack(PropCfg.EXCEL_NAME, this::loadPropConfig);
        addChangeSampleFileObserveWithCallBack(ResearchSkillsCfg.EXCEL_NAME, this::loadResearchSkillConfig);
    }

    private void loadPropConfig() {
        Map<Integer, Set<Integer>> tmp = new HashMap<>();
        for (PropCfg cfg : GameDataManager.getPropCfgList()) {
            tmp.computeIfAbsent(cfg.getGameType(), k -> new HashSet<>()).add(cfg.getId());
        }
        this.skillPropIdsMap = tmp;
    }

    private void loadResearchSkillConfig() {
        Map<Integer, Map<Integer, Map<Integer, ResearchSkillsCfg>>> tmp = new HashMap<>();
        for (ResearchSkillsCfg cfg : GameDataManager.getResearchSkillsCfgList()) {
            tmp.computeIfAbsent(cfg.getGameType(), k -> new HashMap<>())
                    .computeIfAbsent(cfg.getAttr(), k -> new HashMap<>())
                    .put(cfg.getGrade(), cfg);
        }
        this.skillsCfgMap = tmp;
    }

    /**
     * 获取玩家在指定slots游戏的技能数据
     *
     * @param playerId
     * @param gameType
     * @return
     */
    public SimSkillsData getSimSkillsData(long playerId, int gameType) {
        return simSkillsDao.findById(SimSkillsData.buildKey(playerId, gameType)).orElse(null);
    }

    /**
     * 保存技能数据
     *
     * @param data
     */
    public void save(SimSkillsData data) {
        if (data == null) {
            return;
        }
        try {
            simSkillsDao.save(data);
        } catch (Exception e) {
            log.error("保存 SimSkillsData 失败 id={}", data.getId(), e);
        }
    }

    /**
     * 解锁技能
     *
     * @param data
     */
    public void unlockSkills(SimSkillsData data) {
        Set<Integer> propIds = this.skillPropIdsMap.get(data.getGameType());
        if (propIds == null || propIds.isEmpty()) {
            return;
        }

        for (int skillPropId : propIds) {
            PropCfg cfg = GameDataManager.getPropCfg(skillPropId);
            if (cfg == null || cfg.getSkillId() == null || cfg.getSkillId().isEmpty()) {
                return;
            }

            if (cfg.getType() == SimConstant.PropConfig.TYPE_STAKE) {

            } else {
                data.addNewSkill(cfg.getId());
            }
        }
    }

    /**
     * 获取玩家的技能列表(技能配置id)
     *
     * @param data
     * @return
     */
    public List<Integer> querySkills(SimSkillsData data) {
        List<Integer> list = new ArrayList<>();
        for (ResearchSkillsCfg cfg : transSkillId(data)) {
            list.add(cfg.getId());
        }
        return list;
    }

    /**
     * 升级技能
     *
     * @param data
     * @param propId
     * @return
     */
    public int upgradeSkill(SimSkillsData data, int propId) {
        Map<Integer, Map<Integer, ResearchSkillsCfg>> cfgMap = this.skillsCfgMap.get(data.getGameType());
        if (cfgMap == null || cfgMap.isEmpty()) {
            log.warn("升级技能失败，未找到技能配置 playerId={},propId={}", data.getPlayerId(), propId);
            return Code.NOT_FOUND;
        }

        Map<Integer, ResearchSkillsCfg> levelMap = cfgMap.get(propId);
        if (levelMap == null || levelMap.isEmpty()) {
            log.warn("升级技能失败，未找到技能配置2 playerId={},propId={}", data.getPlayerId(), propId);
            return Code.NOT_FOUND;
        }

        Integer beforeLevel = data.findSkilLevelByPropId(propId);
        if (beforeLevel == null) {
            log.warn("升级技能失败，该技能还未解锁 playerId={},propId={}", data.getPlayerId(), propId);
            return Code.PARAM_ERROR;
        }

        //新等级的配置
        ResearchSkillsCfg newLevelCfg = levelMap.get(beforeLevel + 1);
        if (newLevelCfg == null) {
            log.warn("升级技能失败，该技能已达到上限 playerId={},propId={}", data.getPlayerId(), propId);
            return Code.NOT_FOUND;
        }

        //检查新等级所需要的研究点
        if (newLevelCfg.getResearchPoints() == null || newLevelCfg.getResearchPoints().isEmpty()) {
            data.changeSkillLevel(propId, newLevelCfg.getGrade());
            log.warn("该技能等级升级无需研究点，升级技能成功 playerId={},propId={},newLevelCfgId={}", data.getPlayerId(), propId, newLevelCfg.getId());
            return Code.SUCCESS;
        }

        SimPlayerGameData simPlayerGameData = simManager.getSimPlayerGameDataFromDB(data.getPlayerId());
        if (simPlayerGameData == null) {
            log.warn("升级技能失败，未找到simPlayerGameData playerId={},propId={}", data.getPlayerId(), propId);
            return Code.NOT_FOUND;
        }

        //检查研究点是否足够
        for (Map.Entry<Integer, Integer> en : newLevelCfg.getResearchPoints().entrySet()) {
            int researchPoint = simPlayerGameData.findResearchPoint(en.getKey());
            if (researchPoint < en.getValue()) {
                log.warn("升级技能失败，研究点不足 playerId={},propId={},newLevelCfgId={},researchPoint={}", data.getPlayerId(), propId, newLevelCfg.getId(), researchPoint);
                return Code.NOT_ENOUGH;
            }
        }

        //扣除研究点
        for (Map.Entry<Integer, Integer> en : newLevelCfg.getResearchPoints().entrySet()) {
            simPlayerGameData.deductResearchPoint(en.getKey(), en.getValue());
        }
        data.changeSkillLevel(propId, newLevelCfg.getGrade());
        log.info("玩家技能升级成功 playerId={},propId={},newLevel={}", data.getPlayerId(), propId, newLevelCfg.getGrade());
        return Code.SUCCESS;
    }

    /**
     * gm升级技能
     *
     * @param data
     * @param cfg
     * @return
     */
    public int gmLevelUpSkill(SimSkillsData data, ResearchSkillsCfg cfg) {
        data.changeSkillLevel(cfg.getAttr(), cfg.getGrade());
        return Code.SUCCESS;
    }

    /**
     * 获取玩家通过技能解锁的下注额列表
     *
     * @param data
     * @return
     */
    public List<Long> skillStakeList(SimSkillsData data) {
        List<Long> list = new ArrayList<>();
        for (ResearchSkillsCfg cfg : transSkillId(data)) {
            if (cfg.getBet() > 0) {
                long bet = cfg.getBet();
                list.add(bet);
            }
        }
        return list;
    }

    /**
     * 判断 betValue 是否为玩家技能解锁的下注额
     *
     * @param data
     * @param betValue
     * @return
     */
    public boolean isSkillBet(SimSkillsData data, long betValue) {
        for (ResearchSkillsCfg cfg : transSkillId(data)) {
            if (cfg.getBet() == betValue) {
                return true;
            }
        }
        return false;
    }

    /**
     * 将玩家身上的 propId->level 转化成 List<ResearchSkillsCfg>
     *
     * @param data
     * @return
     */
    public List<ResearchSkillsCfg> transSkillId(SimSkillsData data) {
        Map<Integer, Map<Integer, ResearchSkillsCfg>> cfgMap = this.skillsCfgMap.get(data.getGameType());
        if (data.getSkillsMap() == null || data.getSkillsMap().isEmpty() || cfgMap == null || cfgMap.isEmpty()) {
            return Collections.emptyList();
        }

        List<ResearchSkillsCfg> list = new ArrayList<>();
        for (Map.Entry<Integer, Integer> en : data.getSkillsMap().entrySet()) {
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

    /**
     * 将玩家技能 specialMode (libType -> weightDelta) 累加到 typeProp 权重上，返回修改后的克隆
     */
    protected PropInfo applySpecialModeSkillBonus(PropInfo propInfo, List<ResearchSkillsCfg> skillCfgList) {
        //将玩家技能中所有影响libType权重的加成合并
        Map<Integer, Integer> deltaMap = new HashMap<>();
        for (ResearchSkillsCfg cfg : skillCfgList) {
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
     * @param simSkillsData
     * @param propInfo
     * @return
     */
    public PropInfo useLibTypeSkill(SimSkillsData simSkillsData, PropInfo propInfo) {
        if (simSkillsData == null) {
            return propInfo;
        }

        //应用玩家技能的 specialMode 加成
        List<ResearchSkillsCfg> skillCfgList = transSkillId(simSkillsData);
        if (skillCfgList.isEmpty()) {
            return propInfo;

        }

        PropInfo newPropInfo = applySpecialModeSkillBonus(propInfo, skillCfgList);
        if (propInfo == null) {
            return propInfo;
        }
        return newPropInfo;
    }

    /**
     * 使用技能
     * @param simSkillsData
     * @param propInfo
     * @return
     */
    public PropInfo useSectionSkill(SimSkillsData simSkillsData, PropInfo propInfo,int libType) {
        if (simSkillsData == null) {
            return propInfo;
        }

        //应用玩家技能的 winRate / specialModeProbUp 加成
        List<ResearchSkillsCfg> skillCfgList = transSkillId(simSkillsData);
        if (!skillCfgList.isEmpty()) {
            return applySectionSkillBonus(propInfo, libType, skillCfgList);
        }
        return propInfo;
    }
}
