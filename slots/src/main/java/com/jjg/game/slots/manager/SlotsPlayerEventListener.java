package com.jjg.game.slots.manager;

import com.jjg.game.common.concurrent.BaseHandler;
import com.jjg.game.common.concurrent.PlayerExecutorGroupDisruptor;
import com.jjg.game.common.listener.SessionCloseListener;
import com.jjg.game.common.listener.SessionEnterListener;
import com.jjg.game.common.protostuff.PFSession;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.common.utils.WheelTimerUtil;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.dao.PlayerSessionTokenDao;
import com.jjg.game.core.data.*;
import com.jjg.game.core.logger.CoreLogger;
import com.jjg.game.core.recharge.service.RechargeService;
import com.jjg.game.core.service.CorePlayerService;
import com.jjg.game.core.service.PlayerSessionService;
import com.jjg.game.core.task.manager.TaskManager;
import com.jjg.game.slots.controller.SlotsRoomController;
import com.jjg.game.slots.data.SlotsPlayerGameData;
import com.jjg.game.social.constant.SocialConst;
import com.jjg.game.social.service.SocialStatusService;
import com.jjg.game.sim.constant.SimConstant;
import com.jjg.game.sim.pb.res.NotifyGuideTrigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;


/**
 * @author 11
 * @date 2025/7/24 17:04
 */
@Component
public class SlotsPlayerEventListener implements SessionEnterListener, SessionCloseListener {
    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private CorePlayerService playerService;
    @Autowired
    private PlayerSessionService playerSessionService;
    @Autowired
    private CoreLogger logger;
    @Autowired
    private SlotsFactoryManager slotsFactoryManager;
    @Autowired
    private PlayerSessionTokenDao playerSessionTokenDao;
    @Autowired
    private SlotsRoomManager slotsRoomManager;
    @Autowired
    private CoopRoomManager coopRoomManager;
    @Autowired
    private TaskManager taskManager;
    @Autowired
    private RechargeService rechargeService;
    @Autowired
    private SocialStatusService socialStatusService;
    @Autowired
    private SlotsRPCLinkManager slotsRPCLinkManager;

    @Override
    public void sessionClose(PFSession session) {
        exitGame(session, ExitType.DROPPED);
    }

    @Override
    public void sessionEnter(PFSession session, long playerId) {
        try {
            session.setPlayerId(playerId);
            session.setWorkId(playerId);

            PlayerSessionInfo info = playerSessionService.getInfo(playerId);
            if (info == null) {
                log.warn("sessionEnter时 PlayerSessionInfo 为空 playerId = {}", playerId);
                return;
            }

            if (info.getGameType() < 1) {
                log.warn("sessionEnter时 PlayerSessionInfo中gameType小于1 getPlayerId = {}", playerId);
                return;
            }

            //检查slots游戏管理器
            AbstractSlotsGameManager gameManager = slotsFactoryManager.getGameManager(info.getGameType(), info.getRoomCfgId());
            if (gameManager == null) {
                log.debug("sessionEnter时，获取游戏管理器失败 playerId = {},gameType = {}", playerId, info.getGameType());
                return;
            }

            final PlayerSessionInfo tempInfo = info;

            Player player = playerService.doSave(playerId, p -> {
                p.setGameType(tempInfo.getGameType());
                p.setRoomCfgId(tempInfo.getRoomCfgId());
            });

            playerSessionService.enterGameServer(player);

            PlayerController playerController = new PlayerController(session, player);
            session.setReference(playerController);

            boolean entered;
            if (player.getRoomId() < 1) {
                entered = enterSlotsGame(session, player, playerController, info, gameManager);
            } else {
                entered = enterRoomSlotsGame(session, player, playerController, info, gameManager);
            }
            if (entered) {
                notifySlotsGuideDelayed(playerController);
            }
            socialStatusService.broadcastStatus(playerId, SocialConst.FriendOnlineStatus.IN_GAME);
        } catch (Exception e) {
            log.error("", e);
        }
    }

    /**
     * 玩家切换到 SLOT 节点后激活 PathName=2 的等待引导。
     * 延迟执行以保证具体 SLOT 游戏的进场响应先于通用引导通知到达客户端。
     */
    private void notifySlotsGuideDelayed(PlayerController playerController) {
        long playerId = playerController.playerId();
        long workId = playerController.getSession().getWorkId();
        WheelTimerUtil.schedule(() ->
                        PlayerExecutorGroupDisruptor.getDefaultExecutor().publishWithFallback(
                                workId, 0, new BaseHandler<String>() {
                                    @Override
                                    public void action() {
                                        if (playerController.getSession() == null
                                                || playerController.getSession().getReference() != playerController) {
                                            log.info("跳过已离开SLOT节点的引导场景检查 playerId={}", playerId);
                                            return;
                                        }
                                        CommonResult<List<Integer>> result = slotsRPCLinkManager.enterGuidePath(
                                                playerController, SimConstant.GuidePath.SLOTS);
                                        if (!result.success() || result.data == null || result.data.isEmpty()) {
                                            return;
                                        }
                                        NotifyGuideTrigger notify = new NotifyGuideTrigger(Code.SUCCESS);
                                        notify.guideGroupIds = result.data;
                                        playerController.send(notify);
                                        log.info("玩家进入SLOT场景后触发新手引导 playerId={},groups={}",
                                                playerId, result.data);
                                    }
                                }.setHandlerParamWithSelf("slots guide path enter notify")),
                SimConstant.GuideTiming.SLOTS_ENTER_TRIGGER_DELAY_MILLIS,
                TimeUnit.MILLISECONDS);
    }

    /**
     * 进入slots游戏
     *
     * @param session
     * @param player
     * @param playerSessionInfo
     * @param gameManager
     */
    private boolean enterSlotsGame(PFSession session, Player player, PlayerController playerController, PlayerSessionInfo playerSessionInfo, AbstractSlotsGameManager gameManager) {
        //放入玩家对应线程中处理避免和回存冲突
        boolean published = PlayerExecutorGroupDisruptor.getDefaultExecutor().tryPublish(session.getWorkId(), 0, new BaseHandler<String>() {
            @Override
            public void action() throws Exception {
                //删除之前全部的playerGameData
                slotsFactoryManager.onEnterGame(playerController.playerId(), playerController.getPlayer().getRoomCfgId(), 0);
                taskManager.loadTaskData(player.getId());
                //创建 PlayerGameData
                gameManager.createPlayerGameData(playerController, playerSessionInfo.getEnterType(), playerSessionInfo.getTargetValue());
                //大厅非重连会检查一次，这里再检查一次
                rechargeService.loadOfflineRecharge(player.getId());
            }
        });
        PlayerSessionToken playerSessionToken = playerSessionTokenDao.getByPlayerId(player.getId());
        logger.enterGame(player, player.getGameType(), player.getRoomCfgId(), playerSessionToken.getDevice());
        log.debug("玩家进入slots 游戏 playerId = {},gameType = {},enterType={}", player.getId(), player.getGameType(), playerSessionInfo.getEnterType());
        return published;
    }

    /**
     * 进入好友房slots游戏
     *
     * @param session
     * @param player
     * @param playerSessionInfo
     * @param gameManager
     */
    private boolean enterRoomSlotsGame(PFSession session, Player player, PlayerController playerController, PlayerSessionInfo playerSessionInfo, AbstractSlotsGameManager gameManager) {
        SlotsRoomController slotsRoomController = slotsRoomManager.enterRoom(playerController);
        if (slotsRoomController == null) {
            log.warn("进入好友房slots时失败 playerId = {},gameType = {},roomId = {}", player.getId(), player.getGameType(), player.getRoomId());
            playerService.doSave(player.getId(), p -> {
                p.setRoomId(0);
            });
            return false;
        }

        //设置workId
        session.setWorkId(slotsRoomController.getRoom().getId());

        //放入玩家对应线程中处理避免和回存冲突
        boolean published = PlayerExecutorGroupDisruptor.getDefaultExecutor().tryPublish(session.getWorkId(), 0, new BaseHandler<String>() {
            @Override
            public void action() throws Exception {
                //删除之前全部的playerGameData
                slotsFactoryManager.onEnterGame(playerController.playerId(), playerController.getPlayer().getRoomCfgId(), player.getRoomId());
                playerController.setScene(slotsRoomController);
                //创建 PlayerGameData
                taskManager.loadTaskData(player.getId());
                gameManager.createPlayerGameData(playerController, playerSessionInfo.getEnterType(), playerSessionInfo.getTargetValue());
                //大厅非重连会检查一次，这里再检查一次
                rechargeService.loadOfflineRecharge(player.getId());
            }
        });
        logger.enterGame(player, player.getGameType(), player.getRoomCfgId(), player.getDeviceType());
        log.debug("玩家进入好友房slots 游戏 playerId = {},gameType = {},roomId = {},enterType={}", player.getId(), player.getGameType(), player.getRoomId(), playerSessionInfo.getEnterType());
        return published;
    }

    /**
     * 退出游戏
     *
     * @param session  PFSession
     * @param exitType 退出类型
     */
    public CommonResult<SlotsPlayerGameData> exitGame(PFSession session, ExitType exitType) {
        CommonResult<SlotsPlayerGameData> result = new CommonResult<>(Code.SUCCESS);
        PlayerController playerController = (PlayerController) session.getReference();
        if (playerController == null) {
            log.warn("玩家退出游戏服务器时 playerController 为空,playerId={},sessionId={}", session.getPlayerId(),
                    session.sessionId());
            return result;
        }

        AbstractSlotsGameManager<?, ?, ?> gameManager = slotsFactoryManager.getGameManager(playerController.getPlayer().getGameType(), playerController.getPlayer().getRoomCfgId());
        SlotsPlayerGameData playerGameData = gameManager == null ? null : gameManager.getPlayerGameData(playerController);
        boolean canExit = playerGameData == null || gameManager.canExit(playerGameData);
        //特殊状态下，玩家无法主动退出
        if (exitType == ExitType.INITIATIVE && !canExit) {
            result.code = Code.FAIL;
            return result;
        }
        int coopExitCode = coopRoomManager.onPlayerExit(playerController.playerId(), exitType);
        if (coopExitCode != Code.SUCCESS) {
            result.code = coopExitCode;
            return result;
        }
        if (gameManager == null) {
            log.debug("退出游戏时，获取游戏管理器失败 playerId = {},gameType = {}", playerController.playerId(), playerController.getPlayer().getGameType());
            return result;
        }
        if (playerGameData == null) {
            return result;
        }
        playerGameData = gameManager.exit(playerController, exitType);
        playerSessionService.offline(playerController.getPlayer(), exitType == ExitType.DROPPED);
        //计算玩游戏的时长
        int onlineTimeLen = 0;
        if (playerGameData != null && playerGameData.getCreateTime() != 0) {
            onlineTimeLen = TimeHelper.nowInt() - playerGameData.getCreateTime();
        }
        session.setReference(null);
        logger.exitGame(playerController.getPlayer(), onlineTimeLen, playerController.getPlayer().getDeviceType());
        result.data = playerGameData;
        log.debug("玩家退出slots游戏 playerId = {}", playerController.playerId());
        return result;
    }

}
