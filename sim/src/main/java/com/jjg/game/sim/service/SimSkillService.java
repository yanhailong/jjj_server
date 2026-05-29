package com.jjg.game.sim.service;

import com.jjg.game.core.constant.Code;
import com.jjg.game.core.listener.ConfigExcelChangeListener;
import com.jjg.game.sampledata.bean.ResearchSkillsCfg;
import com.jjg.game.sim.dao.SimSkillsDao;
import com.jjg.game.sim.data.SimCasinoData;
import com.jjg.game.sim.data.SimPlayerContext;
import com.jjg.game.sim.data.SimSkillsData;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private SimSkillsDao simSkillsDao;

    public void loadSkillsData(SimPlayerContext ctx) {
        List<SimSkillsData> list = simSkillsDao.findByPlayerId(ctx.playerId());
        if (list != null && !list.isEmpty()) {
            list.forEach(skillsData -> {
                ctx.getSkillsDataMap().put(skillsData.getGameType(), skillsData);
            });
        }
    }

    /**
     * 升级技能
     */
    public int upgradeSkill(SimPlayerContext ctx, SimSkillsData simSkillsData, int propId) {
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

        //研究点在当前赌场上
        SimCasinoData casino = ctx.getCurrentCasino();
        if (casino == null) {
            log.warn("升级技能失败，当前赌场不存在 playerId={}", simSkillsData.getPlayerId());
            return Code.NOT_FOUND;
        }

        //检查研究点是否足够
        for (Map.Entry<Integer, Integer> en : newLevelCfg.getResearchPoints().entrySet()) {
            int researchPoint = casino.findResearchPoint(en.getKey());
            if (researchPoint < en.getValue()) {
                log.warn("升级技能失败，研究点不足 playerId={},propId={},newLevelCfgId={},researchPoint={}", simSkillsData.getPlayerId(), propId, newLevelCfg.getId(), researchPoint);
                return Code.NOT_ENOUGH;
            }
        }

        //扣除研究点
        for (Map.Entry<Integer, Integer> en : newLevelCfg.getResearchPoints().entrySet()) {
            casino.deductResearchPoint(en.getKey(), en.getValue());
        }

        simSkillsData.changeSkillLevel(propId, newLevelCfg.getGrade());
        log.info("玩家技能升级成功 playerId={},propId={},newLevel={}", simSkillsData.getPlayerId(), propId, newLevelCfg.getGrade());
        return Code.SUCCESS;
    }

}
