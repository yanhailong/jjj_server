package com.jjg.game.poker.game.tosouthblood.room;

import com.jjg.game.common.utils.CommonUtil;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.EGameType;
import com.jjg.game.core.dao.room.FriendRoomBillHistoryDao;
import com.jjg.game.core.data.FriendRoom;
import com.jjg.game.core.data.FriendRoomBillHistoryBean;
import com.jjg.game.core.data.Room;
import com.jjg.game.core.data.RoomType;
import com.jjg.game.room.constant.EGamePhase;
import com.jjg.game.room.controller.AbstractRoomController;
import com.jjg.game.room.controller.GameController;
import com.jjg.game.room.data.room.FriendRoomBillHistoryHelper;
import com.jjg.game.room.data.room.RoomBankerChangeParam;
import com.jjg.game.room.data.room.SettlementData;
import com.jjg.game.room.friendroom.AbstractFriendRoomController;
import com.jjg.game.room.friendroom.FriendRoomSampleUtils;
import com.jjg.game.room.message.RoomMessageBuilder;
import com.jjg.game.room.message.resp.NotifyPauseGameOnNewRound;
import com.jjg.game.sampledata.bean.Room_ChessCfg;

import java.util.HashMap;
import java.util.Map;

/**
 * 南方前进-血战好友房游戏控制器
 */
@GameController(gameType = EGameType.TO_SOUTH_BLOOD, roomType = RoomType.POKER_TEAM_UP_ROOM)
public class ToSouthBloodFriendGameController extends ToSouthBloodGameController {

    public ToSouthBloodFriendGameController(AbstractRoomController<Room_ChessCfg, ? extends Room> roomController) {
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
    public void onDestroyRoomAction() {
        if (getCurrentGamePhase() == EGamePhase.WAIT_READY) {
            broadcastGamePauseInfo();
        }
    }

    @Override
    public void onContinueGameAction() {
        if (getCurrentGamePhase() == EGamePhase.WAIT_READY) {
            broadcastGamePauseInfo();
        }
    }

    @Override
    public void pauseGame() {
        super.pauseGame();
        if (getCurrentGamePhase() == EGamePhase.WAIT_READY) {
            broadcastGamePauseInfo();
        }
    }

    @Override
    public void broadcastGamePauseInfo() {
        NotifyPauseGameOnNewRound notifyPauseGameOnNewRound = new NotifyPauseGameOnNewRound();
        FriendRoom friendRoom = getRoom();
        if (friendRoom.getStatus() == 2) {
            notifyPauseGameOnNewRound.pauseType = 1;
        }
        if (friendRoom.getOverdueTime() < System.currentTimeMillis()) {
            notifyPauseGameOnNewRound.pauseType = 3;
        }
        int minBankerAmount = FriendRoomSampleUtils.getRoomMinBankerAmount(gameDataVo.getRoomCfg().getId());
        if (friendRoom.getPredictCostGoldNum() < minBankerAmount) {
            notifyPauseGameOnNewRound.pauseType = 2;
        }
        if (friendRoom.getStatus() == 3) {
            notifyPauseGameOnNewRound.pauseType = 4;
        }
        broadcastToPlayers(RoomMessageBuilder.newBuilder().setData(notifyPauseGameOnNewRound).toAllPlayer());
    }

    @Override
    public void dealBankerFlowing(RoomBankerChangeParam param, Map<Long, SettlementData> settlementDataMap) {
        if (roomController instanceof AbstractFriendRoomController<?, ?>) {
            FriendRoomBillHistoryDao dao = roomController.getRoomManager().getFriendRoomBillHistoryDao();
            if (!settlementDataMap.isEmpty()) {
                long roomCreatorTotalIncome = param.getRoomCreatorTotalIncome();
                FriendRoomBillHistoryBean historyBean = FriendRoomBillHistoryHelper.buildFriendRoom(getRoom());
                historyBean.setTotalIncome(roomCreatorTotalIncome);
                long totalFlowing =
                        settlementDataMap.values().stream()
                                .filter(s -> s.getBetWin() > 0)
                                .mapToLong(SettlementData::getBetWin)
                                .sum();
                historyBean.setTotalFlowing(totalFlowing);
                historyBean.setItemId(getGameTransactionItemId());
                historyBean.setGameMajorType(CommonUtil.getMajorTypeByGameType(roomController.getRoom().getGameType()));
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
                dao.addFriendRoomBillHistory(historyBean);
            }
        }
    }
}
