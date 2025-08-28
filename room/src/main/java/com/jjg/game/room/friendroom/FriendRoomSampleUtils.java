package com.jjg.game.room.friendroom;

import com.jjg.game.core.constant.Code;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.RoomCfg;

/**
 * 好友房配置工具
 *
 * @author 2CL
 */
public class FriendRoomSampleUtils {

    /**
     * 获取房间最低上庄金额
     */
    public static int getRoomMinBankerAmount(int roomCfgId) {
        RoomCfg roomCfg = GameDataManager.getRoomCfg(roomCfgId);
        if (roomCfg.getMinBankerAmount() != null && roomCfg.getMinBankerAmount().size() > 1) {
            return roomCfg.getMinBankerAmount().get(1);
        }
        return 0;
    }
}
