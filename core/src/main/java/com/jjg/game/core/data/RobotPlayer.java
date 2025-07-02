package com.jjg.game.core.data;

/**
 * 机器人玩家，机器人的数据放置在这
 *
 * @author 2CL
 */
public class RobotPlayer extends Player {

    /**
     * 机器人当前的房间配置ID
     */
    private int roomCfgId;

    public int getRoomCfgId() {
        return roomCfgId;
    }

    public void setRoomCfgId(int roomCfgId) {
        this.roomCfgId = roomCfgId;
    }
}
