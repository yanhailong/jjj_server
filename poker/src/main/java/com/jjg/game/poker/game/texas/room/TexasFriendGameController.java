package com.jjg.game.poker.game.texas.room;

import com.jjg.game.core.constant.EGameType;
import com.jjg.game.core.dao.room.FriendRoomBillHistoryDao;
import com.jjg.game.core.data.FriendRoomBillHistoryBean;
import com.jjg.game.core.data.Room;
import com.jjg.game.core.data.RoomType;
import com.jjg.game.room.controller.AbstractRoomController;
import com.jjg.game.room.controller.GameController;
import com.jjg.game.room.data.room.FriendRoomBillHistoryHelper;
import com.jjg.game.room.data.room.SettlementData;
import com.jjg.game.room.friendroom.AbstractFriendRoomController;
import com.jjg.game.sampledata.bean.Room_ChessCfg;

import java.util.HashMap;
import java.util.Map;

/**
 * @author 2CL
 */
@GameController(gameType = EGameType.TEXAS, roomType = RoomType.POKER_TEAM_UP_ROOM)
public class TexasFriendGameController extends TexasGameController {

    public TexasFriendGameController(AbstractRoomController<Room_ChessCfg, ? extends Room> roomController) {
        super(roomController);
    }

    public void dealBankerFlowing(Map<Long, SettlementData> settlementDataMap) {
        dealBankerFlowing(0, settlementDataMap);
    }

    @Override
    public void dealBankerFlowing(long bankerFlowing, Map<Long, SettlementData> settlementDataMap) {
        if (roomController instanceof AbstractFriendRoomController<?, ?>) {
            // 需要记录
            FriendRoomBillHistoryDao dao = roomController.getRoomManager().getFriendRoomBillHistoryDao();
            if (!settlementDataMap.isEmpty()) {
                long roomTotalIncome =
                    settlementDataMap.values().stream()
                        .filter(s -> s.getBetWin() > 0)
                        .mapToLong(SettlementData::getBetWin)
                        .sum();
                if (roomTotalIncome <= 0) {
                    return;
                }
                // 构建基础历史数据bean
                FriendRoomBillHistoryBean historyBean = FriendRoomBillHistoryHelper.buildFriendRoom(getRoom());
                long roomCreatorTotalIncome =
                    settlementDataMap.values().stream()
                        .filter(s -> s.getRoomCreatorIncome() > 0)
                        .mapToLong(SettlementData::getRoomCreatorIncome)
                        .sum();
                historyBean.setTotalFlowing(roomCreatorTotalIncome);
                historyBean.setItemId(getGameTransactionItemId());
                historyBean.setPartInPlayerIncome(
                    settlementDataMap.entrySet().stream()
                        .collect(HashMap::new,
                            (map, e)
                                -> map.put(e.getKey(), e.getValue().getTotalWin()),
                            HashMap::putAll));
                // 添加历史
                dao.addFriendRoomBillHistory(historyBean);
            }
        }
    }
}
