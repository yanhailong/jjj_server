package com.jjg.game.sim.service;

import com.jjg.game.core.constant.Code;
import com.jjg.game.core.listener.ConfigExcelChangeListener;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.PropCfg;
import com.jjg.game.sampledata.bean.ResearchSkillsCfg;
import com.jjg.game.sim.dao.SimSkillsDao;
import com.jjg.game.sim.data.SimSkillsData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author 11
 * @date 2026/5/25
 */
public abstract class AbstractSkillService implements ConfigExcelChangeListener {
    protected static final int GLOBAL_GAME_TYPE = 0;
    protected static final int GLOBAL_SKILL_TYPE = 1;

    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    protected SimSkillsDao simSkillsDao;

    //技能道具id  gameType -> propId集合
    protected Map<Integer, Set<Integer>> skillPropIdsMap = new HashMap<>();
    //技能配置  gameType -> propId -> grade -> cfg
    protected Map<Integer, Map<Integer, Map<Integer, ResearchSkillsCfg>>> skillsCfgMap = new HashMap<>();

    @Override
    public void initSampleCallbackCollector() {
        addInitSampleFileObserveWithCallBack(PropCfg.EXCEL_NAME, this::loadPropConfig);
        addInitSampleFileObserveWithCallBack(ResearchSkillsCfg.EXCEL_NAME, this::loadResearchSkillConfig);
    }

    protected void loadPropConfig() {
        Map<Integer, Set<Integer>> tmp = new HashMap<>();
        for (PropCfg cfg : GameDataManager.getPropCfgList()) {
            tmp.computeIfAbsent(skillGameType(cfg), k -> new HashSet<>()).add(cfg.getId());
        }
        this.skillPropIdsMap = tmp;
    }

    public void loadResearchSkillConfig() {
        Map<Integer, Map<Integer, Map<Integer, ResearchSkillsCfg>>> tmp = new HashMap<>();
        for (ResearchSkillsCfg cfg : GameDataManager.getResearchSkillsCfgList()) {
            Map<Integer, Map<Integer, ResearchSkillsCfg>> tmpMap1 = tmp.computeIfAbsent(cfg.getGameType(), k -> new HashMap<>());
            Map<Integer, ResearchSkillsCfg> tmpMap2 = tmpMap1.computeIfAbsent(cfg.getAttr(), k -> new HashMap<>());

            tmpMap2.put(cfg.getGrade(),cfg);
        }
        this.skillsCfgMap = tmp;
    }

    protected int skillGameType(PropCfg cfg) {
        return cfg.getType() == GLOBAL_SKILL_TYPE ? GLOBAL_GAME_TYPE : cfg.getGameType();
    }

    public SimSkillsData getSkillDataByGameType(long playerId, int gameType) {
        return simSkillsDao.findById(SimSkillsData.buildKey(playerId, gameType)).orElse(null);
    }

    public ResearchSkillsCfg getResearchSkillsCfg(int gameType,int skillAttrId,int level){
        Map<Integer, Map<Integer, ResearchSkillsCfg>> gameTymeCfgMap = this.skillsCfgMap.get(gameType);
        if(gameTymeCfgMap == null || gameTymeCfgMap.isEmpty()){
            return null;
        }
        Map<Integer, ResearchSkillsCfg> attrMap = gameTymeCfgMap.get(skillAttrId);
        if(attrMap == null || attrMap.isEmpty()){
            return null;
        }
        return attrMap.get(level);
    }

    public int addSkill(SimSkillsData simSkillsData, int skillId) {
        ResearchSkillsCfg cfg = GameDataManager.getResearchSkillsCfg(skillId);
        if (cfg == null) {
            log.warn("添加技能失败，未找到技能配置 playerId={},skillId={}", simSkillsData.getPlayerId(), skillId);
            return Code.NOT_FOUND;
        }
        simSkillsData.changeSkillLevel(cfg.getAttr(), cfg.getGrade());
        return Code.SUCCESS;
    }

    public void save(SimSkillsData simSkillsData) {
        simSkillsDao.save(simSkillsData);
    }
}
