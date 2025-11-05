package com.jjg.game.poker.game.blackjack.gamephase;

import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.data.RobotPlayer;
import com.jjg.game.core.data.RoomPlayer;
import com.jjg.game.core.pb.NotifyExitRoom;
import com.jjg.game.poker.game.blackjack.autohandler.BlackJackRobotHandler;
import com.jjg.game.poker.game.blackjack.room.BlackJackGameController;
import com.jjg.game.poker.game.blackjack.room.data.BlackJackGameDataVo;
import com.jjg.game.poker.game.common.PokerBuilder;
import com.jjg.game.poker.game.common.data.PlayerSeatInfo;
import com.jjg.game.poker.game.common.gamephase.BaseBetPhase;
import com.jjg.game.poker.game.common.message.reps.NotifyPokerPhaseChange;
import com.jjg.game.poker.game.texas.data.SeatInfo;
import com.jjg.game.room.constant.EGamePhase;
import com.jjg.game.room.controller.AbstractPhaseGameController;
import com.jjg.game.room.data.robot.GameRobotPlayer;
import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.room.message.RoomMessageBuilder;
import com.jjg.game.room.robot.RobotUtil;
import com.jjg.game.sampledata.bean.Room_ChessCfg;

import java.util.*;

import static com.jjg.game.poker.game.blackjack.constant.BlackJackConstant.Common.BET_FIX_TIME;

/**
 * @author lm
 * @date 2025/7/28 14:16
 */
public class BlackJackBetPhase extends BaseBetPhase<BlackJackGameDataVo> {

    private final long id;

    public BlackJackBetPhase(AbstractPhaseGameController<Room_ChessCfg, BlackJackGameDataVo> gameController, long id) {
        super(gameController);
        this.id = id;
    }


    @Override
    public void nextPhase() {
        //设置当前游戏阶段为发牌
        if (gameController instanceof BlackJackGameController controller) {
            BlackJackPlayCardPhase gamePhase = new BlackJackPlayCardPhase(controller);
            controller.addPokerPhase(gamePhase);
            //通知场上信息
            PokerBuilder.buildNotifyPhaseChange(EGamePhase.PLAY_CART, -1);
        }
    }

    @Override
    public void phaseDoAction() {
        super.phaseDoAction();
        robotAction();
    }

    @Override
    public void robotPhaseScheduleAction(GameRobotPlayer robotPlayer, int chessExecutionDelay, int pro) {
        if (gameController instanceof BlackJackGameController controller) {
            BlackJackRobotHandler handler = new BlackJackRobotHandler(robotPlayer, BlackJackRobotHandler.BET, controller, pro);
            RobotUtil.schedule(controller.getRoomController(), handler, chessExecutionDelay);
        }
    }

    @Override
    public void phaseFinish() {
        if (id != gameDataVo.getId()) {
            log.info("该定时器已经取消或者不需要再执行 id:{}", id);
            return;
        }
        if (gameController instanceof BlackJackGameController controller) {
            if (controller.getCurrentGamePhase() != EGamePhase.BET) {
                log.info("该定时器已经不需要再执行 id:{}", id);
                return;
            }
            //生成执行列表
            TreeMap<Integer, SeatInfo> seatInfoTreeMap = gameDataVo.getSeatInfo();
            List<PlayerSeatInfo> playerSeatInfoList = gameDataVo.getPlayerSeatInfoList();
            controller.genPlayerSeatInfoList(seatInfoTreeMap, playerSeatInfoList);
            //没下注的人直接踢掉
            List<PlayerSeatInfo> playerSeatInfo = gameDataVo.getPlayerSeatInfoList();
            List<PlayerSeatInfo> noBetPlayer = new ArrayList<>(playerSeatInfo.size());
            List<Long> noJoinPlayer = new ArrayList<>(playerSeatInfo.size());
            long timeMillis = System.currentTimeMillis();
            Map<Long, Long> baseBetInfo = gameDataVo.getBaseBetInfo();
            for (PlayerSeatInfo seatInfo : playerSeatInfo) {
                GamePlayer gamePlayer = gameDataVo.getGamePlayer(seatInfo.getPlayerId());
                if (Objects.nonNull(gamePlayer)) {
                    if (!baseBetInfo.containsKey(gamePlayer.getId())) {
                        if (timeMillis - BET_FIX_TIME > gamePlayer.getPokerPlayerGameData().getJoinTime()) {
                            noBetPlayer.add(seatInfo);
                        } else {
                            noJoinPlayer.add(seatInfo.getPlayerId());
                        }
                    }
                }
            }
            List<PlayerController> robot = new ArrayList<>();
            for (PlayerSeatInfo info : noBetPlayer) {
                Map<Long, PlayerController> playerControllers = gameController.getRoomController().getPlayerControllers();
                PlayerController playerController = playerControllers.get(info.getPlayerId());
                if (Objects.nonNull(playerController)) {
                    if (playerController.getPlayer() instanceof RobotPlayer) {
                        robot.add(playerController);
                    } else {
                        RoomPlayer roomPlayer = gameController.getRoom().getRoomPlayers().get(info.getPlayerId());
                        if (Objects.nonNull(roomPlayer) && roomPlayer.isOnline()) {
                            log.info("玩家：{}  未押注直接踢掉", info.getPlayerId());
                            NotifyExitRoom timeNoOperate = new NotifyExitRoom();
                            timeNoOperate.langId = gameDataVo.getRoomCfg().getEscTipText();
                            broadcastBuilderToRoom(RoomMessageBuilder.newBuilder().sendPlayer(playerController.playerId(), timeNoOperate));
                        } else {
                            log.info("玩家：{}  未押注离线直接踢掉退出房间", info.getPlayerId());
                            gameController.getRoomController().getRoomManager().exitRoom(playerController);
                            GamePlayer gamePlayer = getGameDataVo().getGamePlayer(info.getPlayerId());
                            if (gamePlayer != null) {
                                gameController.getGameDataTracker().sendExitGameLog(gamePlayer);
                            } else {
                                log.info("提出玩家：{}  时gamePlayer为null", info.getPlayerId());
                            }
                        }
                    }
                }
                playerSeatInfo.remove(info);
            }
            //机器人直接退出
            gameController.getRoomController().getRoomManager().robotPlayerExitRoom(robot);
            //未下注的移除
            if (!noJoinPlayer.isEmpty()) {
                playerSeatInfo.removeIf(info -> noJoinPlayer.contains(info.getPlayerId()));
            }
            if (gameDataVo.canStartGame() && !gameDataVo.getBaseBetInfo().isEmpty()) {
                //进入下个阶段
                nextPhase();
            } else {
                //重新等待
                gameDataVo.getPlayerSeatInfoList().clear();
                controller.goBackWaitReadyPhase();
                NotifyPokerPhaseChange notifyPokerPhaseChange = PokerBuilder.buildNotifyPhaseChange(EGamePhase.WAIT_READY, -1);
                broadcastMsgToRoom(notifyPokerPhaseChange);
            }
        }
    }
}
