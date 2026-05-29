package com.jjg.game.sim.service;

import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.CasinoListCfg;
import com.jjg.game.sampledata.bean.CasinoStatsSheetCfg;
import com.jjg.game.sim.constant.SimConstant;
import com.jjg.game.sim.dao.SimCasinoDao;
import com.jjg.game.sim.data.SimBaseData;
import com.jjg.game.sim.data.SimCasinoData;
import com.jjg.game.sim.data.SimPlayerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
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

    /**
     * 加载场景数据
     *
     * @param ctx
     * @param baseData
     * @return
     */
    public SimCasinoData loadCasinoData(SimPlayerContext ctx, SimBaseData baseData) {
        //加载赌场数据
        SimCasinoData simCasinoData = null;
        if (baseData.getCurrentCasinoId() > 1) {
            simCasinoData = simCasinoDao.findById(SimCasinoData.buildKey(ctx.playerId(), baseData.getCurrentCasinoId())).orElse(null);
            if (simCasinoData != null) {
                ctx.getCasinoMap().put(simCasinoData.getCasinoId(), simCasinoData);
            } else {
                log.warn("玩家获取当前所在场景数据为空 playerId={},currentCasinoId={}", ctx.playerId(), baseData.getCurrentCasinoId());
            }
        } else {
            List<SimCasinoData> simCasinoDataList = initPlayerCasino(ctx.playerId());
            if (simCasinoDataList.isEmpty()) {
                log.warn("玩家初始化时解锁赌场失败 playerId={}", ctx.playerId());
            } else {
                for (SimCasinoData cd : simCasinoDataList) {
                    ctx.getCasinoMap().put(cd.getCasinoId(), cd);
                }
                simCasinoData = simCasinoDataList.get(0);
                baseData.setCurrentCasinoId(simCasinoData.getCasinoId());
            }
        }

        //检查是否有建筑完成升级
        simBuildingService.completeAllBuildingUpgrade(simCasinoData);
        return simCasinoData;
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
}
