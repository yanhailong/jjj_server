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
import com.jjg.game.poker.game.texas.message.reps.NotifyTexasSeatStateChange;
import com.jjg.game.poker.game.texas.message.req.ReqTexasChangeSeatState;
import com.jjg.game.poker.game.texas.message.req.ReqTexasChangeTable;
import com.jjg.game.poker.game.texas.message.req.ReqTexasHistory;
import com.jjg.game.poker.game.texas.message.req.ReqTexasShowCard;
import com.jjg.game.poker.game.texas.room.TexasGameController;
import com.jjg.game.poker.game.texas.room.data.TexasGameDataVo;
import com.jjg.game.room.constant.EGamePhase;
import com.jjg.game.room.controller.AbstractGameController;
import com.jjg.game.room.data.room.GameDataVo;
import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.room.manager.RoomManager;
import com.jjg.game.room.message.RoomMessageBuilder;
import com.jjg.game.sampledata.bean.RoomCfg;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;

/**
 * @author lm
 * @date 2025/7/30 14:16
 */
@Component
@MessageType(value = MessageConst.MessageTypeDef.TEXAS_TYPE)
public class TexasMessageHandler {
    @Autowired
    private RoomManager roomManager;


    @Command(value = TexasConstant.MsgBean.REQ_SHOW_CARD)
    public void reqTexasShowCard(PlayerController playerController, ReqTexasShowCard reqTexasShowCard) {
        AbstractGameController<? extends RoomCfg, ? extends GameDataVo<? extends RoomCfg>> gameController =
                roomManager.getGameControllerByPlayerId(playerController.playerId());
        if (gameController instanceof TexasGameController controller) {
            controller.reqShowCard(playerController.playerId(), controller);
        }
    }

    @Command(value = TexasConstant.MsgBean.REQ_CHANGE_SEAT_STATE)
    public void reqTexasChangeSeatState(PlayerController playerController, ReqTexasChangeSeatState reqTexasChangeSeatState) {
        long playerId = playerController.playerId();
        AbstractGameController<? extends RoomCfg, ? extends GameDataVo<? extends RoomCfg>> gameController =
                roomManager.getGameControllerByPlayerId(playerId);
        if (gameController instanceof TexasGameController controller) {
            TexasGameDataVo gameDataVo = controller.getGameDataVo();
            NotifyTexasSeatStateChange change = new NotifyTexasSeatStateChange();
            SeatInfo seatInfo = null;
            for (SeatInfo tempSeatInfo : gameDataVo.getSeatInfo().values()) {
                if (tempSeatInfo.getPlayerId() == playerId) {
                    seatInfo = tempSeatInfo;
                }
            }
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
                if (reqTexasChangeSeatState.changeType == 1) {
                    boolean state = reqTexasChangeSeatState.param == 1;
                    if (state) {
                        if (controller.inRunPhase()) {
                            change.code = Code.FORBID;
                            controller.broadcastToPlayers(RoomMessageBuilder.newBuilder().sendPlayer(playerId, change));
                            return;
                        }
                        boolean added = controller.addTempGoldOrOutTable(seatInfo, gamePlayer);
                        if (!added) {
                            change.code = Code.NOT_ENOUGH;
                            controller.broadcastToPlayers(RoomMessageBuilder.newBuilder().sendPlayer(playerId, change));
                            return;
                        }
                        seatInfo.setSeatDown(true);
                        change.playerChange = PokerBuilder.buildPlayerInfo(gamePlayer, seatInfo, gameDataVo);
                        controller.broadcastToPlayers(RoomMessageBuilder.newBuilder().sendAllPlayer(change));
                        // 通知场上玩家加入 准备进入开始阶段
                        boolean canStartGame = gameDataVo.canStartGame();
                        if (canStartGame && controller.getCurrentGamePhase() == EGamePhase.WAIT_READY) {
                            //尝试开启游戏
                            controller.tryStartNextGame();
                        }
                    } else {
                        //非等待 需要移除信息 判断是否需要开启下一轮和结算
                        if (controller.inRunPhase() && seatInfo.isJoinGame()) {
                            boolean isPlaying = seatInfo.isSeatDown() && seatInfo.isJoinGame();
                            seatInfo.setSeatDown(false);
                            seatInfo.setJoinGame(false);
                            change.playerChange = PokerBuilder.buildPlayerInfo(gamePlayer, seatInfo, gameDataVo);
                            controller.broadcastToPlayers(RoomMessageBuilder.newBuilder().sendAllPlayer(change));
                            controller.runPlayerSeatChange(seatInfo, isPlaying);
                            return;
                        }
                        seatInfo.setSeatDown(false);
                        change.playerChange = PokerBuilder.buildPlayerInfo(gamePlayer, seatInfo, gameDataVo);
                        controller.broadcastToPlayers(RoomMessageBuilder.newBuilder().sendAllPlayer(change));
                        return;
                    }

                    return;
                } else if (reqTexasChangeSeatState.changeType == 2) {
                    //改变座位id 当前局还未结束
                    if (controller.inRunPhase() && seatInfo.isJoinGame() || !controller.inRunPhase() && seatInfo.isSeatDown()) {
                        //加入游戏 禁止换座位
                        change.code = Code.FORBID;
                        gameController.broadcastToPlayers(RoomMessageBuilder.newBuilder().sendPlayer(playerId, change));
                        return;
                    }
                    if (!controller.addTempGoldOrOutTable(seatInfo, gamePlayer)) {
                        change.code = Code.NOT_ENOUGH;
                        controller.broadcastToPlayers(RoomMessageBuilder.newBuilder().sendAllPlayer(change));
                        return;
                    }
                    NotifyTexasSeatStateChange notifyTexasSeatStateChange = swapSeat(controller, seatInfo, gamePlayer, reqTexasChangeSeatState.param);
                    controller.broadcastToPlayers(RoomMessageBuilder.newBuilder().sendAllPlayer(notifyTexasSeatStateChange));
                    // 通知场上玩家加入 准备进入开始阶段
                    boolean canStartGame = gameDataVo.canStartGame();
                    if (canStartGame && controller.getCurrentGamePhase() == EGamePhase.WAIT_READY) {
                        //尝试开启游戏
                        controller.tryStartNextGame();
                    }
                    return;
                }
            }
        }
        NotifyTexasSeatStateChange change = new NotifyTexasSeatStateChange();
        change.code = Code.FAIL;
        gameController.broadcastToPlayers(RoomMessageBuilder.newBuilder().sendPlayer(playerId, change));
    }

    public NotifyTexasSeatStateChange swapSeat(TexasGameController controller, SeatInfo seatInfo, GamePlayer gamePlayer, int srcSeatId) {
        TexasGameDataVo gameDataVo = controller.getGameDataVo();
        NotifyTexasSeatStateChange change = new NotifyTexasSeatStateChange();
        Map<Long, RoomPlayer> roomPlayers = controller.getRoom().getRoomPlayers();
        //如果有人交换位置
        //判断目标座位是否有人
        SeatInfo srcSeatInfo = gameDataVo.getSeatInfo().get(srcSeatId);
        if (Objects.nonNull(srcSeatInfo)) {
            if (srcSeatInfo.isSeatDown()) {
                change.code = Code.FORBID;
                return change;
            }
            RoomPlayer roomPlayer = roomPlayers.get(srcSeatInfo.getPlayerId());
            addNewSeatInfo(roomPlayer, srcSeatInfo, seatInfo.getSeatId(), gameDataVo);
        }
        //换座位
        RoomPlayer roomPlayer = roomPlayers.get(seatInfo.getPlayerId());
        SeatInfo newSeatInfo = addNewSeatInfo(roomPlayer, seatInfo, srcSeatId, gameDataVo);
        change.playerChange = PokerBuilder.buildPlayerInfo(gamePlayer, newSeatInfo, gameDataVo);
        return change;
    }

    private static SeatInfo addNewSeatInfo(RoomPlayer roomPlayer, SeatInfo seatInfo, int srcSeatId, TexasGameDataVo gameDataVo) {
        roomPlayer.setSit(srcSeatId);
        gameDataVo.getSeatInfo().remove(seatInfo.getSeatId());
        SeatInfo newSeatInfo = new SeatInfo();
        newSeatInfo.setPlayerId(seatInfo.getPlayerId());
        newSeatInfo.setJoinGame(seatInfo.isJoinGame());
        newSeatInfo.setSeatId(srcSeatId);
        newSeatInfo.setSeatDown(true);
        gameDataVo.getSeatInfo().put(srcSeatId, newSeatInfo);
        return newSeatInfo;
    }

    @Command(value = TexasConstant.MsgBean.REQ_CHANGE_TABLE)
    public void reqTexasChangeTable(PlayerController playerController, ReqTexasChangeTable changeTable) {
        AbstractGameController<? extends RoomCfg, ? extends GameDataVo<? extends RoomCfg>> gameController =
                roomManager.getGameControllerByPlayerId(playerController.playerId());
        if (gameController instanceof TexasGameController controller) {
            controller.reqChangeTable(playerController, controller);
        }
    }

    @Command(value = TexasConstant.MsgBean.REQ_TEXAS_HISTORY)
    public void reqTexasHistory(PlayerController playerController, ReqTexasHistory req) {
        AbstractGameController<? extends RoomCfg, ? extends GameDataVo<? extends RoomCfg>> gameController =
                roomManager.getGameControllerByPlayerId(playerController.playerId());
        if (gameController instanceof TexasGameController controller) {
            controller.reqTexasHistory(playerController.playerId(), req);
        }
    }
}
