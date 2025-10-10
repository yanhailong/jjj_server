package com.jjg.game.poker.game.texas.gamephase;

import com.jjg.game.core.utils.TipUtils;
import com.jjg.game.poker.game.common.PokerBuilder;
import com.jjg.game.poker.game.common.gamephase.BaseStartGamePhase;
import com.jjg.game.poker.game.texas.autohandler.TexasRobotHandler;
import com.jjg.game.poker.game.texas.data.SeatInfo;
import com.jjg.game.poker.game.texas.message.reps.NotifyTexasSeatStateChange;
import com.jjg.game.poker.game.texas.room.TexasGameController;
import com.jjg.game.poker.game.texas.room.data.TexasGameDataVo;
import com.jjg.game.room.constant.EGamePhase;
import com.jjg.game.room.data.robot.GameRobotPlayer;
import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.room.message.RoomMessageBuilder;
import com.jjg.game.room.robot.RobotUtil;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.ChessRobotCfg;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;

/**
 * @author lm
 * @date 2025/9/19 15:58
 */
public class TexasStartGamePhase extends BaseStartGamePhase<TexasGameDataVo> {

    public TexasStartGamePhase(TexasGameController gameController) {
        super(gameController, gameController.getGameDataVo().getId());
    }

    @Override
    public void phaseDoAction() {
        super.phaseDoAction();
        if (gameController instanceof TexasGameController controller) {
            //获取机器人 并进行准备
            for (SeatInfo seatInfo : gameDataVo.getSeatInfo().values()) {
                if (!seatInfo.isSeatDown()) {
                    continue;
                }
                GamePlayer gamePlayer = gameDataVo.getGamePlayer(seatInfo.getPlayerId());
                if (gamePlayer instanceof GameRobotPlayer robotPlayer) {
                    ChessRobotCfg chessRobotCfg = GameDataManager.getChessRobotCfg(robotPlayer.getActionId());
                    int chessExecutionDelay = RobotUtil.getChessExecutionDelay(robotPlayer.getActionId());
                    int pro;
                    if (robotPlayer.getLastWin() == 0) {
                        //刚刚加入房间
                        pro = 10000;
                    } else {
                        pro = robotPlayer.getLastWin() == 1 ? chessRobotCfg.getContinueAfterVictory().getFirst() : chessRobotCfg.getContinueAfterFail().getFirst();
                    }
                    TexasRobotHandler handler = new TexasRobotHandler(robotPlayer, TexasRobotHandler.GO_READY, controller, pro);
                    RobotUtil.schedule(controller.getRoomController(), handler, chessExecutionDelay);
                }
            }
        }
    }

    @Override
    public void nextPhase() {
        //设置当前游戏阶段为发牌
        if (gameController instanceof TexasGameController controller) {
            //将未准备的提起来
            NotifyTexasSeatStateChange notify = new NotifyTexasSeatStateChange();
            TreeMap<Integer, SeatInfo> seatInfoTreeMap = gameDataVo.getSeatInfo();
            notify.playerChange = new ArrayList<>(seatInfoTreeMap.size());
            Set<Long> leaveSeatIds = new HashSet<>();
            for (SeatInfo info : seatInfoTreeMap.values()) {
                if (info.isSeatDown() && !info.isJoinGame()) {
                    //未准备的站起
                    if (!info.isReady()) {
                        //修改座位状态
                        info.setSeatDown(false);
                        notify.playerChange.addAll(PokerBuilder.buildPlayerInfoList(gameDataVo.getGamePlayer(info.getPlayerId()), info, controller));
                        leaveSeatIds.add(info.getPlayerId());
                    }
                }
            }
            //通知房间的人座位变化
            broadcastBuilderToRoom(RoomMessageBuilder.newBuilder().sendAllPlayer(notify));
            //通知具体的人提示
            for (Long levelSeatPlayerId : leaveSeatIds) {
                TipUtils.sendTip(levelSeatPlayerId, TipUtils.TipType.TOAST, 300200098);
            }
            TexasPlayCardPhase gamePhase = new TexasPlayCardPhase(controller);
            controller.addPokerPhase(gamePhase);
            //通知场上信息
            PokerBuilder.buildNotifyPhaseChange(EGamePhase.PLAY_CART, -1);
        }
    }


    @Override
    public void onPhaseReset() {
        //当倒计时结束时，将强制将没有准备的玩家离开座位，玩家收到飘字提示300200098 由於您長時間沒有準備，已被請離座位
        NotifyTexasSeatStateChange notify = new NotifyTexasSeatStateChange();
        TreeMap<Integer, SeatInfo> seatInfoTreeMap = gameDataVo.getSeatInfo();
        if (seatInfoTreeMap.isEmpty()) {
            return;
        }
        if (gameController instanceof TexasGameController controller) {
            notify.playerChange = new ArrayList<>(seatInfoTreeMap.size());
            Set<Long> leaveSeatIds = new HashSet<>(seatInfoTreeMap.size());
            for (SeatInfo info : seatInfoTreeMap.values()) {
                if (info.isSeatDown() && !info.isJoinGame()) {
                    //未准备的站起
                    if (!info.isReady()) {
                        //修改座位状态
                        info.setSeatDown(false);
                        notify.playerChange.addAll(PokerBuilder.buildPlayerInfoList(gameDataVo.getGamePlayer(info.getPlayerId()), info, controller));
                        leaveSeatIds.add(info.getPlayerId());
                    } else {
                        //准备的设置为未准备
                        info.setReady(false);
                        //更新操作时间
                        controller.updatePlayerLatestOperateTime(info.getPlayerId());
                        notify.playerChange.addAll(PokerBuilder.buildPlayerInfoList(gameDataVo.getGamePlayer(info.getPlayerId()), info, controller));
                    }
                }
            }
            //通知房间的人座位变化
            broadcastBuilderToRoom(RoomMessageBuilder.newBuilder().sendAllPlayer(notify));
            //通知具体的人提示
            for (Long levelSeatPlayerId : leaveSeatIds) {
                TipUtils.sendTip(levelSeatPlayerId, TipUtils.TipType.TOAST, 300200098);
            }
        }
    }
}
