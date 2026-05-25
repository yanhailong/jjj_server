package com.jjg.game.sim.service;

import com.jjg.game.core.constant.Code;
import com.jjg.game.core.listener.ConfigExcelChangeListener;
import com.jjg.game.sampledata.bean.ResearchSkillsCfg;
import com.jjg.game.sim.data.SimPlayerGameData;
import com.jjg.game.sim.data.SimSkillsData;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * slots 技能服务
 *
 * @author 11
 * @date 2026/5/22
 */
@Service
public class SimSkillService extends AbstractSkillService implements ConfigExcelChangeListener {

    public List<SimSkillsData> getAllSlostsSkills(long playerId) {
        return simSkillsDao.findByPlayerId(playerId);
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
            data.buildKey();
            simSkillsDao.save(data);
        } catch (Exception e) {
            log.error("保存 SimSkillsData 失败 id={}", data.getId(), e);
        }
    }

    /**
     * 升级技能
     *
     * @param simSkillsData
     * @param propId
     * @return
     */
    public int upgradeSkill(SimPlayerGameData simPlayerGameData, SimSkillsData simSkillsData, int propId) {
        Map<Integer, Map<Integer, ResearchSkillsCfg>> cfgMap = this.skillsCfgMap.get(simSkillsData.getGameType());
        if (cfgMap == null || cfgMap.isEmpty()) {
            log.warn("升级技能失败，未找到技能配置 playerId={},propId={}", simSkillsData.getPlayerId(), propId);
            return Code.NOT_FOUND;
        }

        Map<Integer, ResearchSkillsCfg> levelMap = cfgMap.get(propId);
        if (levelMap == null || levelMap.isEmpty()) {
            log.warn("升级技能失败，未找到技能配置2 playerId={},propId={}", simSkillsData.getPlayerId(), propId);
            return Code.NOT_FOUND;
        }

        Integer beforeLevel = simSkillsData.findSkilLevelByPropId(propId);
        if (beforeLevel == null) {
            log.warn("升级技能失败，该技能还未解锁 playerId={},propId={}", simSkillsData.getPlayerId(), propId);
            return Code.PARAM_ERROR;
        }

        //新等级的配置
        ResearchSkillsCfg newLevelCfg = levelMap.get(beforeLevel + 1);
        if (newLevelCfg == null) {
            log.warn("升级技能失败，该技能已达到上限 playerId={},propId={}", simSkillsData.getPlayerId(), propId);
            return Code.NOT_FOUND;
        }

        //检查新等级所需要的研究点
        if (newLevelCfg.getResearchPoints() == null || newLevelCfg.getResearchPoints().isEmpty()) {
            simSkillsData.changeSkillLevel(propId, newLevelCfg.getGrade());
            log.warn("该技能等级升级无需研究点，升级技能成功 playerId={},propId={},newLevelCfgId={}", simSkillsData.getPlayerId(), propId, newLevelCfg.getId());
            return Code.SUCCESS;
        }

        //检查研究点是否足够
        for (Map.Entry<Integer, Integer> en : newLevelCfg.getResearchPoints().entrySet()) {
            int researchPoint = simPlayerGameData.findResearchPoint(en.getKey());
            if (researchPoint < en.getValue()) {
                log.warn("升级技能失败，研究点不足 playerId={},propId={},newLevelCfgId={},researchPoint={}", simSkillsData.getPlayerId(), propId, newLevelCfg.getId(), researchPoint);
                return Code.NOT_ENOUGH;
            }
        }

        //扣除研究点
        for (Map.Entry<Integer, Integer> en : newLevelCfg.getResearchPoints().entrySet()) {
            simPlayerGameData.deductResearchPoint(en.getKey(), en.getValue());
        }

        simSkillsData.changeSkillLevel(propId, newLevelCfg.getGrade());
        log.info("玩家技能升级成功 playerId={},propId={},newLevel={}", simSkillsData.getPlayerId(), propId, newLevelCfg.getGrade());
        return Code.SUCCESS;
    }

}
