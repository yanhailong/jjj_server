package com.jjg.game.sim.service;

import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BuildingUpgradeTableCfg;
import com.jjg.game.sampledata.bean.CasinoListCfg;
import com.jjg.game.sampledata.bean.CasinoStatsSheetCfg;
import com.jjg.game.sim.constant.BonusType;
import com.jjg.game.sim.constant.SimConstant;
import com.jjg.game.sim.dao.SimCasinoDao;
import com.jjg.game.sim.data.BuildingData;
import com.jjg.game.sim.data.SimBaseData;
import com.jjg.game.sim.data.SimCasinoData;
import com.jjg.game.sim.data.SimPlayerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author 11
 * @date 2026/5/28
 */
@Service
public class SimCasinoService {

    private final Logger log = LoggerFactory.getLogger(SimCasinoService.class);

    @Autowired
    private SimCasinoDao simCasinoDao;
    @Autowired
    private SimBuildingService simBuildingService;
    @Autowired
    private SimConfigCacheService configCacheService;
    @Autowired
    private SimEmployeeService employeeService;

    /**
     * 加载场景数据
     *
     * @param ctx
     * @param baseData
     * @return
     */
    public SimCasinoData loadCasinoData(SimPlayerContext ctx, SimBaseData baseData) {
        SimCasinoData currentCasino = null;
        //按 playerId 加载玩家已有的全部赌场, 以"DB 中是否有数据"区分新老玩家, 避免误判把存档冲掉
        List<SimCasinoData> existList = simCasinoDao.findByPlayerId(ctx.playerId());
        if (!existList.isEmpty()) {
            for (SimCasinoData cd : existList) {
                ctx.getCasinoMap().put(cd.getCasinoId(), cd);
            }
            currentCasino = ctx.getCasino(baseData.getCurrentCasinoId());
            if (currentCasino == null) {
                //当前赌场 id 失效时回退到任一已有赌场
                currentCasino = existList.get(0);
                baseData.setCurrentCasinoId(currentCasino.getCasinoId());
                log.warn("玩家当前赌场 id 无对应数据, 回退 playerId={},currentCasinoId={}", ctx.playerId(), baseData.getCurrentCasinoId());
            }
        } else {
            //新玩家: 初始化默认赌场
            List<SimCasinoData> initList = initPlayerCasino(ctx.playerId());
            if (initList.isEmpty()) {
                log.warn("玩家初始化时解锁赌场失败 playerId={}", ctx.playerId());
            } else {
                for (SimCasinoData cd : initList) {
                    ctx.getCasinoMap().put(cd.getCasinoId(), cd);
                }
                currentCasino = initList.get(0);
                baseData.setCurrentCasinoId(currentCasino.getCasinoId());
            }
        }

        //检查是否有建筑完成升级
        simBuildingService.completeAllBuildingUpgrade(currentCasino);
        return currentCasino;
    }


    /**
     * 玩家初始化时的解锁条件
     *
     * @param playerId
     * @return
     */
    public List<SimCasinoData> initPlayerCasino(long playerId) {
        List<SimCasinoData> list = new ArrayList<>();
        //按 CasinoList 配置补齐解锁但未创建的赌场
        CasinoStatsSheetCfg defaultCasinoCfg = GameDataManager.getCasinoStatsSheetCfg(SimConstant.Common.DEFAULT_CASINO_STATS_ID);
        for (Map.Entry<Integer, CasinoListCfg> en : GameDataManager.getCasinoListCfgMap().entrySet()) {
            CasinoListCfg cfg = en.getValue();
            int casinoId = cfg.getId();
            Map<Integer, Integer> condition = cfg.getCondition();
            if (condition != null && !condition.isEmpty()) {
                continue;
            }
            SimCasinoData simCasinoData = new SimCasinoData();
            simCasinoData.setPlayerId(playerId);
            simCasinoData.setCasinoId(casinoId);
            simCasinoData.setStatsId(SimConstant.Common.DEFAULT_CASINO_STATS_ID);
            simCasinoData.setProsperity(defaultCasinoCfg.getProsperity());
            list.add(simCasinoData);
        }
        return list;
    }

    /**
     * 获取知名度
     *
     * @param ctx
     * @return
     */
    public long awareness(SimPlayerContext ctx) {
        SimCasinoData casinoData = ctx.getCurrentCasino();
        if (casinoData == null) {
            return 0;
        }

        if (casinoData.getBuildingData() == null || casinoData.getBuildingData().isEmpty()) {
            return 0;
        }

        //获取运营部的建筑
        BuildingData buildingData = casinoData.getBuildingData().get(SimConstant.Building.ID_OPERATIONS_DEPART);
        if (buildingData == null) {
            return 0;
        }

        //获取配置
        BuildingUpgradeTableCfg cfg = configCacheService.getBuildingUpgradeCfg(buildingData.getId(), buildingData.getLevel());
        if (cfg == null || cfg.getUpgradeOutput() == null || cfg.getUpgradeOutput().isEmpty()) {
            return 0;
        }

        Long awareness = cfg.getUpgradeOutput().get(SimConstant.Item.ID_AWARENESS);
        if (awareness == null) {
            return 0;
        }

        //获取雇员加成
        Map<BonusType, Integer> bonusesMap = new HashMap<>();
        employeeService.computeTypeBonusFixed(ctx, bonusesMap);
        Integer bouns = bonusesMap.get(BonusType.AWARENESS);
        if (bouns == null) {
            return awareness;
        }
        long extra = awareness * bouns / SimConstant.Common.EMPLOYEE_BONUS_DIVISOR;
        return awareness + extra;
    }
}
