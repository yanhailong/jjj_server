package com.jjg.game.sim.service;

import com.alibaba.fastjson.JSON;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.listener.ConfigExcelChangeListener;
import com.jjg.game.sampledata.bean.ResearchSkillsCfg;
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

            //研究点在当前赌场上
            SimCasinoData casino = ctx.getCurrentCasino();
            if (casino == null) {
                log.warn("升级技能失败，当前赌场不存在 playerId={}", skillData.getPlayerId());
                res.code = Code.NOT_FOUND;
                ctx.send(res);
                return;
            }

            //检查研究点是否足够
            for (Map.Entry<Integer, Integer> en : newLevelCfg.getResearchPoints().entrySet()) {
                int researchPoint = casino.findResearchPoint(en.getKey());
                if (researchPoint < en.getValue()) {
                    log.warn("升级技能失败，研究点不足 playerId={},propId={},newLevelCfgId={},researchPoint={}", skillData.getPlayerId(), skillPropId, newLevelCfg.getId(), researchPoint);
                    res.code = Code.NOT_ENOUGH;
                    ctx.send(res);
                    return;
                }
            }

            //扣除研究点
            for (Map.Entry<Integer, Integer> en : newLevelCfg.getResearchPoints().entrySet()) {
                casino.deductResearchPoint(en.getKey(), en.getValue());
            }

            skillData.changeSkillLevel(skillPropId, newLevelCfg.getGrade());
            log.info("玩家技能升级成功 playerId={},propId={},newLevel={}", skillData.getPlayerId(), skillPropId, newLevelCfg.getGrade());
        } catch (Exception e) {
            log.error("", e);
            res.code = Code.EXCEPTION;
        }
        ctx.send(res);
    }

}
