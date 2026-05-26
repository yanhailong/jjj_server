package com.jjg.game.sim.service;

import com.jjg.game.core.listener.ConfigExcelChangeListener;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.VisitorLevelCfg;
import com.jjg.game.sampledata.bean.VisitorStarCfg;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 配置缓存
 *
 * @author 11
 * @date 2026/5/26
 */
@Component
public class SimConfigCacheService implements ConfigExcelChangeListener {
    //visitorLevel配置 guestId -> level -> cfg
    private Map<Integer, Map<Integer, VisitorLevelCfg>> visitorLevelCfgMap;
    //visitorStar配置 guestId -> star -> cfg
    private Map<Integer, Map<Integer, VisitorStarCfg>> visitorStarCfgMap;

    /**
     * 加载 VisitorLevel 配置
     */
    private void loadVisitorLevelConfig() {
        Map<Integer, Map<Integer, VisitorLevelCfg>> tmpVisitorLevelCfgMap = new HashMap<>();
        for (VisitorLevelCfg cfg : GameDataManager.getVisitorLevelCfgList()) {
            tmpVisitorLevelCfgMap.computeIfAbsent(cfg.getVisitor(), k -> new HashMap<>()).put(cfg.getLevel(), cfg);
        }
        this.visitorLevelCfgMap = tmpVisitorLevelCfgMap;
    }

    /**
     * 加载 VisitorStar 配置
     */
    private void loadVisitorStarConfig() {
        Map<Integer, Map<Integer, VisitorStarCfg>> tmpVisitorStarCfgMap = new HashMap<>();
        for (VisitorStarCfg cfg : GameDataManager.getVisitorStarCfgList()) {
            tmpVisitorStarCfgMap.computeIfAbsent(cfg.getVisitor(), k -> new HashMap<>()).put(cfg.getStarlevel(), cfg);
        }
        this.visitorStarCfgMap = tmpVisitorStarCfgMap;
    }

    @Override
    public void initSampleCallbackCollector() {
        addInitSampleFileObserveWithCallBack(VisitorLevelCfg.EXCEL_NAME, this::loadVisitorLevelConfig);
        addInitSampleFileObserveWithCallBack(VisitorStarCfg.EXCEL_NAME, this::loadVisitorStarConfig);
    }

    public Map<Integer, Map<Integer, VisitorLevelCfg>> getVisitorLevelCfgMap() {
        return visitorLevelCfgMap;
    }

    public Map<Integer, Map<Integer, VisitorStarCfg>> getVisitorStarCfgMap() {
        return visitorStarCfgMap;
    }
}
