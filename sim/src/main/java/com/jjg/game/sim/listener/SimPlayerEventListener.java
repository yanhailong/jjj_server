package com.jjg.game.sim.listener;

import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.concurrent.BaseHandler;
import com.jjg.game.common.concurrent.PlayerExecutorGroupDisruptor;
import com.jjg.game.common.listener.SessionCloseListener;
import com.jjg.game.common.listener.SessionEnterListener;
import com.jjg.game.common.protostuff.PFSession;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.ExitType;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.logger.CoreLogger;
import com.jjg.game.core.recharge.service.RechargeService;
import com.jjg.game.core.service.CorePlayerService;
import com.jjg.game.core.service.PlayerSessionService;
import com.jjg.game.core.task.manager.TaskManager;
import com.jjg.game.sim.manager.SimManager;
import com.jjg.game.sim.service.SimNodeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author 11
 * @date 2026/5/25
 */
@Component
public class SimPlayerEventListener implements SessionEnterListener, SessionCloseListener {
    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private CorePlayerService playerService;
    @Autowired
    private PlayerSessionService playerSessionService;
    @Autowired
    private CoreLogger logger;
    @Autowired
    private TaskManager taskManager;
    @Autowired
    private RechargeService rechargeService;
    @Autowired
    private SimNodeService simNodeService;
    @Autowired
    private ClusterSystem clusterSystem;
    @Autowired
    private SimManager simManager;

    @Override
    public void sessionClose(PFSession session) {
        exitGame(session, ExitType.DROPPED);
    }

    @Override
    public void sessionEnter(PFSession session, long playerId) {
        try {
            session.setPlayerId(playerId);
            session.setWorkId(playerId);

            Player player = playerService.get(playerId);

            playerSessionService.updateNodePath(session, player);
            simNodeService.save(playerId, clusterSystem.getNodePath());

            PlayerController playerController = new PlayerController(session, player);
            session.setReference(playerController);

            //放入玩家对应线程中处理避免和回存冲突
            PlayerExecutorGroupDisruptor.getDefaultExecutor().tryPublish(session.getWorkId(), 0, new BaseHandler<String>() {
                @Override
                public void action() throws Exception {
                    taskManager.loadTaskData(player.getId());
                    //创建玩家会话上下文
                    simManager.createContext(playerController);
                    //大厅非重连会检查一次，这里再检查一次
                    rechargeService.loadOfflineRecharge(player.getId());
                }
            });
            logger.enterGame(player, player.getGameType(), player.getRoomCfgId(), player.getDeviceType());
            log.debug("玩家进入sim 游戏 playerId = {}", player.getId());
        } catch (Exception e) {
            log.error("", e);
        }
    }


    /**
     * 退出游戏
     *
     * @param session  PFSession
     * @param exitType 退出类型
     */
    public int exitGame(PFSession session, ExitType exitType) {
        PlayerController playerController = (PlayerController) session.getReference();
        if (playerController == null) {
            log.warn("玩家退出游戏服务器时 playerController 为空,playerId={},sessionId={}", session.getPlayerId(),
                    session.sessionId());
            return Code.SUCCESS;
        }

        simManager.onExitGame(playerController.playerId(), exitType);
        playerSessionService.offline(playerController.getPlayer(), exitType == ExitType.DROPPED);
        session.setReference(null);
        log.debug("玩家退出sim游戏 playerId = {}", playerController.playerId());
        return Code.SUCCESS;
    }
}
