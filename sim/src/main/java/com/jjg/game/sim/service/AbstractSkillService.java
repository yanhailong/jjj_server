package com.jjg.game.sim.service;

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
            tmp.computeIfAbsent(cfg.getGameType(), k -> new HashSet<>()).add(cfg.getId());
        }
        this.skillPropIdsMap = tmp;
    }

    protected void loadResearchSkillConfig() {
        Map<Integer, Map<Integer, Map<Integer, ResearchSkillsCfg>>> tmp = new HashMap<>();
        for (ResearchSkillsCfg cfg : GameDataManager.getResearchSkillsCfgList()) {
            tmp.computeIfAbsent(cfg.getGameType(), k -> new HashMap<>())
                    .computeIfAbsent(cfg.getAttr(), k -> new HashMap<>())
                    .put(cfg.getGrade(), cfg);
        }
        this.skillsCfgMap = tmp;
    }

    public SimSkillsData getSkillDataByGameType(long playerId, int gameType) {
        return simSkillsDao.findById(SimSkillsData.buildKey(playerId, gameType)).orElse(null);
    }

}
