package com.jjg.game.sim.rpc;

import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.handler.CoreRPCController;
import com.jjg.game.core.rpc.GmToAllBridge;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.ResearchSkillsCfg;
import com.jjg.game.sim.bridge.ToSimBridge;
import com.jjg.game.sim.controller.SimGameController;
import com.jjg.game.sim.data.SimPlayerGameData;
import com.jjg.game.sim.data.SimSkillsData;
import com.jjg.game.sim.manager.SimManager;
import com.jjg.game.sim.service.SimSkillService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * @author 11
 * @date 2026/5/25
 */
@Component
public class SimRPCController extends CoreRPCController implements ToSimBridge, GmToAllBridge {
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private SimManager simManager;
    @Autowired
    private SimSkillService simSkillService;

    @Override
    public int deductResearchPoint(long playerId, Map<Integer, Integer> deductMap) {
        if (deductMap == null || deductMap.isEmpty()) {
            return Code.SUCCESS;
        }
        SimGameController gameController = simManager.getGameController(playerId);
        if (gameController == null) {
            log.warn("扣除研究点失败，未找到玩家 sim 数据 playerId={}", playerId);
            return Code.NOT_FOUND;
        }
        SimPlayerGameData data = gameController.getPlayerGameData();
        //先校验
        for (Map.Entry<Integer, Integer> en : deductMap.entrySet()) {
            if (data.findResearchPoint(en.getKey()) < en.getValue()) {
                log.warn("扣除研究点失败，研究点不足 playerId={},type={},need={}", playerId, en.getKey(), en.getValue());
                return Code.NOT_ENOUGH;
            }
        }
        //再扣
        for (Map.Entry<Integer, Integer> en : deductMap.entrySet()) {
            data.deductResearchPoint(en.getKey(), en.getValue());
        }
        return Code.SUCCESS;
    }

    @Override
    public CommonResult<SimSkillsData> addSkillById(long playerId, int gameType, int skillId) {
        ResearchSkillsCfg cfg = GameDataManager.getResearchSkillsCfg(skillId);
        if (cfg == null) {
            log.warn("添加技能失败，未找到技能配置 playerId={},skillId={}", playerId, skillId);
            return new CommonResult<>(Code.NOT_FOUND);
        }

        //优先用内存中的数据，避免与 sim 节点内存出现脏读
        SimGameController gameController = simManager.getGameController(playerId);
        SimSkillsData data = gameController == null ? null : gameController.getSkillData(gameType);
        if (data == null) {
            data = simSkillService.getSkillDataByGameType(playerId, gameType);
        }
        if (data == null) {
            data = new SimSkillsData();
            data.setPlayerId(playerId);
            data.setGameType(gameType);
        }
        data.changeSkillLevel(cfg.getAttr(), cfg.getGrade());
//        simSkillService.save(data);

        //回写到 sim 内存
        if (gameController != null) {
            Map<Integer, SimSkillsData> skillsMap = gameController.getSkillsDataMap();
            if (skillsMap == null) {
                skillsMap = new HashMap<>();
                gameController.setSkillsDataMap(skillsMap);
            }
            skillsMap.put(gameType, data);
        }

        log.info("添加技能成功 playerId={},gameType={},skillId={},propId={},grade={}",
                playerId, gameType, skillId, cfg.getAttr(), cfg.getGrade());
        return new CommonResult<>(Code.SUCCESS, data);
    }
}
