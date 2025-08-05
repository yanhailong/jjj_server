package com.jjg.game.poker.game.texas.message.handler;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.data.RoomPlayer;
import com.jjg.game.poker.game.common.PokerBuilder;
import com.jjg.game.poker.game.texas.constant.TexasConstant;
import com.jjg.game.poker.game.texas.data.SeatInfo;
import com.jjg.game.poker.game.texas.data.TexasDataHelper;
import com.jjg.game.poker.game.texas.message.reps.NotifySeatStateChange;
import com.jjg.game.poker.game.texas.message.req.ReqChangeSeatState;
import com.jjg.game.poker.game.texas.message.req.ReqChangeTable;
import com.jjg.game.poker.game.texas.message.req.ReqPokerBet;
import com.jjg.game.poker.game.texas.message.req.ReqShowCard;
import com.jjg.game.poker.game.texas.room.TexasGameController;
import com.jjg.game.poker.game.texas.room.data.TexasGameDataVo;
import com.jjg.game.room.constant.EGamePhase;
import com.jjg.game.room.controller.AbstractGameController;
import com.jjg.game.room.data.room.GameDataVo;
import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.room.manager.RoomManager;
import com.jjg.game.room.message.RoomMessageBuilder;
import com.jjg.game.room.sample.bean.RoomCfg;
import com.jjg.game.room.sample.bean.Room_ChessCfg;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * @author lm
 * @date 2025/7/30 14:16
 */
@Component
@MessageType(MessageConst.MessageTypeDef.TEXAS_TYPE)
public class TexasMessageHandler {
    @Autowired
    private RoomManager roomManager;

    @Command(value = TexasConstant.MsgBean.REQ_BET)
    public void reqBet(PlayerController playerController, ReqPokerBet reqPokerBet) {
        AbstractGameController<? extends RoomCfg, ? extends GameDataVo<? extends RoomCfg>> gameController =
                roomManager.getGameControllerByPlayerId(playerController.playerId());
        if (gameController instanceof TexasGameController controller) {
            controller.dealBet(playerController.playerId(), reqPokerBet);
        }
    }

    @Command(value = TexasConstant.MsgBean.REQ_SHOW_CARD)
    public void reqShowCard(PlayerController playerController, ReqShowCard reqShowCard) {
        AbstractGameController<? extends RoomCfg, ? extends GameDataVo<? extends RoomCfg>> gameController =
                roomManager.getGameControllerByPlayerId(playerController.playerId());
        if (gameController instanceof TexasGameController controller) {
            controller.reqShowCard(playerController.playerId(), controller);
        }
    }

    @Command(value = TexasConstant.MsgBean.REQ_CHANGE_SEAT_STATE)
    public void reqChangeSeatState(PlayerController playerController, ReqChangeSeatState reqChangeSeatState) {
        long playerId = playerController.playerId();
        AbstractGameController<? extends RoomCfg, ? extends GameDataVo<? extends RoomCfg>> gameController =
                roomManager.getGameControllerByPlayerId(playerId);
        if (gameController instanceof TexasGameController controller) {
            TexasGameDataVo gameDataVo = controller.getGameDataVo();
            NotifySeatStateChange change = new NotifySeatStateChange();
            SeatInfo seatInfo = gameDataVo.getSeatInfo().get(reqChangeSeatState.seatId);
            if (Objects.isNull(seatInfo) || seatInfo.getPlayerId() != playerId) {
                change.code = Code.PARAM_ERROR;
                controller.broadcastToPlayers(RoomMessageBuilder.newBuilder().sendPlayer(playerId, change));
                return;
            }
            if (seatInfo.getPlayerId() == playerId) {
                GamePlayer gamePlayer = gameDataVo.getGamePlayer(seatInfo.getPlayerId());
                if (Objects.isNull(gamePlayer)) {
                    change.code = Code.PARAM_ERROR;
                    controller.broadcastToPlayers(RoomMessageBuilder.newBuilder().sendPlayer(playerId, change));
                    return;
                }
                //改变座位状态
                if (reqChangeSeatState.changeType == 1) {
                    boolean state = reqChangeSeatState.param == 1;
                    if (state) {
                        if (controller.inRunPhase()) {
                            change.code = Code.FORBID;
                            controller.broadcastToPlayers(RoomMessageBuilder.newBuilder().sendPlayer(playerId, change));
                            return;
                        }
                        boolean added = controller.addTempGoldOrOutTable(seatInfo, gamePlayer);
                        if (!added) {
                            change.code = Code.FORBID;
                            controller.broadcastToPlayers(RoomMessageBuilder.newBuilder().sendPlayer(playerId, change));
                            return;
                        }
                        seatInfo.setSeatDown(true);
                        change.playerChange = PokerBuilder.buildPlayerInfo(gamePlayer, gameDataVo, false);
                        controller.broadcastToPlayers(RoomMessageBuilder.newBuilder().sendAllPlayer(change));
                    } else {
                        //非等待 需要移除信息 判断是否需要开启下一轮和结算
                        if (controller.inRunPhase() && seatInfo.isJoinGame()) {
                            boolean isPlaying = seatInfo.isSeatDown() && seatInfo.isJoinGame();
                            seatInfo.setSeatDown(false);
                            seatInfo.setJoinGame(false);
                            change.playerChange = PokerBuilder.buildPlayerInfo(gamePlayer, gameDataVo, false);
                            controller.broadcastToPlayers(RoomMessageBuilder.newBuilder().sendAllPlayer(change));
                            controller.runPlayerSeatChange(seatInfo, isPlaying);
                            return;
                        }
                        seatInfo.setSeatDown(false);
                        change.playerChange = PokerBuilder.buildPlayerInfo(gamePlayer, gameDataVo, false);
                        controller.broadcastToPlayers(RoomMessageBuilder.newBuilder().sendAllPlayer(change));
                    }
                    // 通知场上玩家加入 准备进入开始阶段
                    boolean canStartGame = gameDataVo.canStartGame();
                    if (canStartGame && controller.getCurrentGamePhase() == EGamePhase.WAIT_READY) {
                        //尝试开启游戏
                        controller.tryStartGame();
                    }
                    if (!canStartGame && controller.getCurrentGamePhase() == EGamePhase.START_GAME) {
                        controller.goBackWaitReadyPhase();
                    }
                    return;
                } else if (reqChangeSeatState.changeType == 2) {
                    //改变座位id 当前局还未结束
                    if (controller.inRunPhase()) {
                        //加入游戏 禁止换座位
                        if (seatInfo.isJoinGame()) {
                            change.code = Code.FORBID;
                            gameController.broadcastToPlayers(RoomMessageBuilder.newBuilder().sendPlayer(playerId, change));
                        } else {
                            NotifySeatStateChange notifySeatStateChange = swapSeat(controller, seatInfo, gamePlayer, reqChangeSeatState.param);
                            controller.broadcastToPlayers(RoomMessageBuilder.newBuilder().sendPlayer(playerId, notifySeatStateChange));
                        }
                        return;
                    } else {
                        //等待阶段换座位需要站起
                        if (seatInfo.isSeatDown()) {
                            change.code = Code.FORBID;
                            gameController.broadcastToPlayers(RoomMessageBuilder.newBuilder().sendPlayer(playerId, change));
                            return;
                        }
                        NotifySeatStateChange notifySeatStateChange = swapSeat(controller, seatInfo, gamePlayer, reqChangeSeatState.param);
                        controller.broadcastToPlayers(RoomMessageBuilder.newBuilder().sendPlayer(playerId, notifySeatStateChange));
                        return;
                    }
                }
            }
        }
        NotifySeatStateChange change = new NotifySeatStateChange();
        change.code = Code.FAIL;
        gameController.broadcastToPlayers(RoomMessageBuilder.newBuilder().sendPlayer(playerId, change));
    }

    public NotifySeatStateChange swapSeat(TexasGameController controller, SeatInfo seatInfo, GamePlayer gamePlayer, int srcSeatId) {
        TexasGameDataVo gameDataVo = controller.getGameDataVo();
        NotifySeatStateChange change = new NotifySeatStateChange();
        //判断目标座位是否有人
        if (gameDataVo.getSeatInfo().containsKey(srcSeatId)) {
            change.code = Code.PARAM_ERROR;
            return change;
        }
        //换座位
        RoomPlayer roomPlayer = controller.getRoom().getRoomPlayers().get(seatInfo.getPlayerId());
        roomPlayer.setSit(srcSeatId);
        SeatInfo remove = gameDataVo.getSeatInfo().remove(seatInfo.getSeatId());
        gameDataVo.getSeatInfo().put(srcSeatId, remove);
        if (seatInfo.isSeatDown()) {
            controller.addTempGoldOrOutTable(remove, gamePlayer);
        }
        change.playerChange = PokerBuilder.buildPlayerInfo(gamePlayer, gameDataVo, false);
        return change;
    }

    @Command(value = TexasConstant.MsgBean.REQ_CHANGE_TABLE)
    public void reqChangeTable(PlayerController playerController, ReqChangeTable changeTable) {
        AbstractGameController<? extends RoomCfg, ? extends GameDataVo<? extends RoomCfg>> gameController =
                roomManager.getGameControllerByPlayerId(playerController.playerId());
        if (gameController instanceof TexasGameController controller) {
            controller.reqChangeTable(playerController, controller);
        }
    }
}
