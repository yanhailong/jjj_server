package com.jjg.game.slots.controller;

import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.ExitType;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.data.RoomPlayer;
import com.jjg.game.core.data.SlotsFriendRoom;
import com.jjg.game.core.pb.NotifyExitRoom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;


/**
 * slots 房间控制器
 */
public class SlotsRoomController {
    private static final Logger log = LoggerFactory.getLogger(SlotsRoomController.class);

    //房间对象
    private SlotsFriendRoom room;
    private Map<Long, PlayerController> playerControllers = new ConcurrentHashMap<>();

    public SlotsRoomController(SlotsFriendRoom room) {
        this.room = room;
    }

    /**
     * 房间暂停
     */
    public void pauseGame() {
        if (this.room.getStatus() == 1) {
            this.room.setStatus(2);
            this.room.setPauseTime(System.currentTimeMillis());
        }
    }

    /**
     * 解散
     */
    public void destroyOnNextRoundStart() {
        this.room.setStatus(3);
        this.room.setPauseTime(System.currentTimeMillis());
    }


    public SlotsFriendRoom getRoom() {
        return room;
    }

    public void playerBet(long playerId, long betValue, long roomInCome) {
        room.addBet(playerId, betValue, roomInCome);
    }

    /**
     * 房间中加入玩家数据
     *
     * @param playerController
     */
    public void addPlayer(PlayerController playerController) {
        RoomPlayer roomPlayer = new RoomPlayer();
        roomPlayer.setPlayerId(playerController.playerId());
        roomPlayer.setOnline(true);
        room.addPlayer(roomPlayer);
        playerControllers.put(playerController.playerId(), playerController);
    }

    /**
     * 房间中移除玩家数据
     *
     * @param playerId
     * @return
     */
    public boolean exitRoom(long playerId) {
        RoomPlayer roomPlayer = this.room.exit(playerId);
        PlayerController playerController = this.playerControllers.remove(playerId);
        return roomPlayer != null || playerController != null;
    }

    /**
     * 给房间中所有人发送消息
     *
     * @param msg
     */
    public void notifyAllPlayers(Object msg) {
        CompletableFuture.runAsync(() -> {
            this.playerControllers.forEach((playerId, playerController) -> {
                playerController.send(msg);
            });
        });
    }

    public PlayerController getPlayerController(long playerId) {
        return this.playerControllers.get(playerId);
    }

    /**
     * 通知玩家在房间长时间未操作
     * @param playerId
     */
    public void playerRoomIdle(long playerId) {
        PlayerController playerController = this.playerControllers.get(playerId);
        if (playerController == null) {
            return;
        }
        NotifyExitRoom notify = new NotifyExitRoom();
        if (this.room.getStatus() == 3) {
            notify.langId = Code.SLOTS_ROOM_PLAYER_KICK_OUT;
        } else {
            notify.langId = Code.ROOM_PLAYER_IDLE;
        }
        playerController.send(notify);
    }

}
