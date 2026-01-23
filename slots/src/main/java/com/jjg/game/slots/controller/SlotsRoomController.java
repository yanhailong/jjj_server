package com.jjg.game.slots.controller;

import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.data.RoomPlayer;
import com.jjg.game.core.data.SlotsFriendRoom;
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
     * @param playerController
     */
    public void exitRoom(PlayerController playerController) {
        this.room.exit(playerController.playerId());
        this.playerControllers.remove(playerController.playerId());
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
}
