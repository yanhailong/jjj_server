package com.jjg.game.room.data.room;

import com.jjg.game.core.data.FriendRoom;
import com.jjg.game.core.data.FriendRoomBillHistoryBean;

/**
 * 好友房账单历史
 *
 * @author 2CL
 */
public class FriendRoomBillHistoryHelper {

    /**
     * 好友房历史账单数据
     */
    public static FriendRoomBillHistoryBean buildFriendRoom(FriendRoom friendRoom) {
        FriendRoomBillHistoryBean historyBean = new FriendRoomBillHistoryBean();
        historyBean.setRoomId(friendRoom.getId());
        historyBean.setRoomCreator(friendRoom.getCreator());
        historyBean.setCreatedAt(System.currentTimeMillis());
        historyBean.setGameType(friendRoom.getGameType());
        historyBean.setGameCfgId(friendRoom.getRoomCfgId());
        historyBean.setPartInPlayerIncome(friendRoom.getPlayerIncomeRec());
        historyBean.setTotalIncome(friendRoom.getCreatorIncome());
        historyBean.setTotalFlowing(friendRoom.getTotalFlowing());
        return historyBean;
    }
}
