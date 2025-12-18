package com.jjg.game.slots.controller;

import com.jjg.game.core.data.SlotsFriendRoom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * slots 房间控制器
 */
public class SlotsRoomController {
    private static final Logger log = LoggerFactory.getLogger(SlotsRoomController.class);

    //房间对象
    private SlotsFriendRoom room;

    public SlotsRoomController(SlotsFriendRoom room) {
        this.room = room;
    }

    /**
     * 房间暂停
     */
    public void pauseGame(){
        if(this.room.getStatus() == 1){
            this.room.setPauseTime(System.currentTimeMillis());
        }
    }

    /**
     * 解散
     */
    public void destroyOnNextRoundStart(){
        this.room.setStatus(3);
        this.room.setPauseTime(System.currentTimeMillis());
    }


    public SlotsFriendRoom getRoom() {
        return room;
    }

    public void playerBet(long roomId, long playerId, long betValue){
        room.addBet(roomId,playerId,betValue);
    }
}
