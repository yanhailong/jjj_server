package com.jjg.game.poker.game.texas.room;

import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.EGameType;
import com.jjg.game.core.dao.room.FriendRoomBillHistoryDao;
import com.jjg.game.core.data.FriendRoomBillHistoryBean;
import com.jjg.game.core.data.Room;
import com.jjg.game.core.data.RoomType;
import com.jjg.game.room.controller.AbstractRoomController;
import com.jjg.game.room.controller.GameController;
import com.jjg.game.room.data.room.FriendRoomBillHistoryHelper;
import com.jjg.game.room.data.room.RoomBankerChangeParam;
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

    @Override
    public int addItem(long playerId, long num, AddType addType, String desc, boolean isNotify) {
        addType = AddType.FRIEND_GAME_SETTLEMENT;
        return super.addItem(playerId, num, addType, desc, isNotify);
    }

    @Override
    public int deductItem(long playerId, long num, AddType deductType, String desc, boolean isNotify) {
        deductType = AddType.FRIEND_GAME_BET;
        return super.deductItem(playerId, num, deductType, desc, isNotify);
    }

    @Override
    public void dealBankerFlowing(RoomBankerChangeParam param, Map<Long, SettlementData> settlementDataMap) {
        if (roomController instanceof AbstractFriendRoomController<?, ?>) {
            // 需要记录
            FriendRoomBillHistoryDao dao = roomController.getRoomManager().getFriendRoomBillHistoryDao();
            if (!settlementDataMap.isEmpty()) {
                long roomCreatorTotalIncome = param.getRoomCreatorTotalIncome();
                // 构建基础历史数据bean
                FriendRoomBillHistoryBean historyBean = FriendRoomBillHistoryHelper.buildFriendRoom(getRoom());
                historyBean.setTotalIncome(roomCreatorTotalIncome);
                long totalFlowing =
                    settlementDataMap.values().stream()
                        .filter(s -> s.getBetWin() > 0)
                        .mapToLong(SettlementData::getBetWin)
                        .sum();
                historyBean.setTotalFlowing(totalFlowing);
                historyBean.setItemId(getGameTransactionItemId());
                historyBean.setMonth(TimeHelper.getMonthNumerical());
                historyBean.setPartInPlayerIncome(
                    settlementDataMap.entrySet().stream()
                        .collect(HashMap::new,
                            (map, e)
                                -> map.put(
                                e.getKey(),
                                e.getValue().getBetWin() > 0
                                    ? e.getValue().getBetWin()
                                    : -1 * e.getValue().getBetTotal()),
                            HashMap::putAll));
                // 添加历史
                dao.addFriendRoomBillHistory(historyBean);
            }
        }
    }
}
