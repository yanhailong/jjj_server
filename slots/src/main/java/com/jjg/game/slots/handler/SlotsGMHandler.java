package com.jjg.game.slots.handler;

import com.jjg.game.common.concurrent.BaseHandler;
import com.jjg.game.common.concurrent.PlayerExecutorGroupDisruptor;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.listener.GmListener;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.ResearchSkillsCfg;
import com.jjg.game.sim.service.SimSkillService;
import com.jjg.game.slots.controller.SlotsRoomController;
import com.jjg.game.slots.dao.SlotsPoolDao;
import com.jjg.game.slots.data.SlotsPlayerGameData;
import com.jjg.game.slots.manager.AbstractSlotsGameManager;
import com.jjg.game.slots.manager.SlotsFactoryManager;
import com.jjg.game.slots.manager.SlotsRoomManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


/**
 * @author 11
 * @date 2025/9/12 15:59
 */
@Component
public class SlotsGMHandler implements GmListener {
    protected final Logger log = LoggerFactory.getLogger(getClass());


    @Autowired
    private SlotsFactoryManager slotsFactoryManager;
    @Autowired
    private SlotsPoolDao slotsPoolDao;
    @Autowired
    private SlotsRoomManager slotsRoomManager;
    @Autowired
    private SimSkillService simSkillService;

    @Override
    public CommonResult<String> gm(PlayerController playerController, String[] gmOrders) {
        CommonResult<String> res = new CommonResult<>(Code.SUCCESS);
        try {
            if ("libType".equalsIgnoreCase(gmOrders[0])) {
                log.debug("收到选择libtype 的gm命令 playerId = {},gmOrders = {}", playerController.playerId(), gmOrders);
                slotsFactoryManager.getGameManager(playerController.getPlayer().getGameType(), playerController.getPlayer().getRoomCfgId()).addTestIconDataLibType(playerController, Integer.parseInt(gmOrders[1]));
            } else if ("setIcons".equalsIgnoreCase(gmOrders[0])) {
                log.debug("收到setIcons 的gm命令 playerId = {},gmOrders = {}", playerController.playerId(), gmOrders);
                slotsFactoryManager.getGameManager(playerController.getPlayer().getGameType(), playerController.getPlayer().getRoomCfgId()).addTestIconDataIcons(playerController, gmOrders[1]);
            } else if ("setLib".equalsIgnoreCase(gmOrders[0])) {
                log.debug("收到setLib 的gm命令 playerId = {},gmOrders = {}", playerController.playerId(), gmOrders);
                slotsFactoryManager.getGameManager(playerController.getPlayer().getGameType(), playerController.getPlayer().getRoomCfgId()).addTestLibs(playerController, gmOrders[1]);
            } else if ("pool".equalsIgnoreCase(gmOrders[0])) {
                log.debug("收到pool 的gm命令 playerId = {},gmOrders = {}", playerController.playerId(), gmOrders);
                boolean change = slotsFactoryManager.getGameManager(playerController.getPlayer().getGameType(), playerController.getPlayer().getRoomCfgId()).gmChangePool(playerController, Integer.parseInt(gmOrders[1]), Long.parseLong(gmOrders[2]));
                if (!change) {
                    res.code = Code.FAIL;
                    return res;
                }
            } else if ("contribt".equalsIgnoreCase(gmOrders[0])) {  //玩家贡献金额
                log.debug("收到contribt 的gm命令 playerId = {},gmOrders = {}", playerController.playerId(), gmOrders);
                boolean change = slotsFactoryManager.getGameManager(playerController.getPlayer().getGameType(), playerController.getPlayer().getRoomCfgId()).gmChangeContribtGold(playerController, Long.parseLong(gmOrders[1]));
                if (!change) {
                    res.code = Code.FAIL;
                    return res;
                }
            } else if ("clearPoolCD".equalsIgnoreCase(gmOrders[0])) { //清除奖池冷却时间
                log.debug("收到 clearPoolCD 的gm命令 playerId = {},gmOrders = {}", playerController.playerId(), gmOrders);
                int poolId = Integer.parseInt(gmOrders[1]);
                if (poolId < 1) {
                    res.code = Code.FAIL;
                    return res;
                }
                slotsPoolDao.clearPoolCD(poolId);
            } else if ("setAllBetCount".equalsIgnoreCase(gmOrders[0])) {
                log.debug("收到 setAllBetCount 的gm命令 playerId = {},gmOrders = {}", playerController.playerId(), gmOrders);
                boolean change = slotsFactoryManager.getGameManager(playerController.getPlayer().getGameType(), playerController.getPlayer().getRoomCfgId()).gmChangeAllData(playerController, Integer.parseInt(gmOrders[1]), -1);
                if (!change) {
                    res.code = Code.FAIL;
                    return res;
                }
            } else if ("setPrizelessCount".equalsIgnoreCase(gmOrders[0])) {
                log.debug("收到 setPrizelessCount 的gm命令 playerId = {},gmOrders = {}", playerController.playerId(), gmOrders);
                boolean change = slotsFactoryManager.getGameManager(playerController.getPlayer().getGameType(), playerController.getPlayer().getRoomCfgId()).gmChangeAllData(playerController, -1, Integer.parseInt(gmOrders[1]));
                if (!change) {
                    res.code = Code.FAIL;
                    return res;
                }
            } else if ("changeRoomEndTime".equalsIgnoreCase(gmOrders[0])) {
                long endTime = Long.parseLong(gmOrders[1]);
                PlayerExecutorGroupDisruptor.getDefaultExecutor().tryPublish(playerController.getPlayer().getRoomId(), 0, new BaseHandler<String>() {
                    @Override
                    public void action() throws Exception {
                        if (playerController.getScene() instanceof SlotsRoomController slotsRoomController) {
                            slotsRoomController.getRoom().setOverdueTime(endTime);
                        }
                    }
                });
            } else if ("skillLevelUp".equalsIgnoreCase(gmOrders[0])) {
                int skillId = Integer.parseInt(gmOrders[1]);
                ResearchSkillsCfg cfg = GameDataManager.getResearchSkillsCfg(skillId);
                if (cfg == null) {
                    res.code = Code.FAIL;
                    log.warn("gm修改技能失败，未找到该技能 skillId={}", skillId);
                    return res;
                }
                AbstractSlotsGameManager<?, ?, ?> gameManager = slotsFactoryManager.getGameManager(playerController.getPlayer().getGameType(), playerController.getPlayer().getRoomCfgId());
                SlotsPlayerGameData playerGameData = gameManager == null ? null : gameManager.getPlayerGameData(playerController.playerId());
                if (playerGameData == null) {
                    res.code = Code.FAIL;
                    log.warn("gm修改技能失败，未找到玩家信息 playerId={}", playerController.playerId());
                    return res;
                }
                res.code = simSkillService.gmLevelUpSkill(playerGameData.getSimSkillsData(), cfg);
            } else {
                res.code = Code.NOT_FOUND;
            }
        } catch (Exception e) {
            log.error("", e);
            res.code = Code.EXCEPTION;
        }
        return res;
    }
}
