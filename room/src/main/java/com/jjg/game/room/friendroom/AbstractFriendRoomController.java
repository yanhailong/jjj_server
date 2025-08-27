package com.jjg.game.room.friendroom;

import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.*;
import com.jjg.game.room.controller.AbstractRoomController;
import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.sampledata.bean.RoomCfg;

/**
 * @author 2CL
 */
public abstract class AbstractFriendRoomController<RC extends RoomCfg> extends AbstractRoomController<RC, FriendRoom> {

    public AbstractFriendRoomController(Class<? extends RoomPlayer> roomPlayerClazz, FriendRoom room) {
        super(roomPlayerClazz, room);
    }

    @Override
    public void continueGame() {
        super.continueGame();
        roomDao.doSave(room.getGameType(), room.getId(), (r) -> {
            r.setStatus(0);
            r.setPauseTime(0);
        });
    }

    @Override
    public void pauseGame() {
        super.pauseGame();
        roomDao.doSave(room.getGameType(), room.getId(), (r) -> {
            r.setStatus(1);
            r.setPauseTime(System.currentTimeMillis());
        });
    }

    @Override
    public void stopGame() {
        super.stopGame();
    }

    /**
     * 不能让机器人加入房间
     */
    @Override
    protected void checkRobotJoinRoom() {
    }

    @Override
    public boolean checkRoomCanContinue() {
        // 好友房，房间庄家不能为空，否则不能继续
        return !room.getBankerPredicateMap().isEmpty();
    }

    @Override
    public void onRoomCantContinue() {
        super.onRoomCantContinue();
    }

    /**
     * 申请成为庄家
     */
    public int supplyBeBanker(PlayerController playerController, long predictCostGold) {
        long playerId = playerController.playerId();
        if (room.roomBankerId() == playerId) {
            // 重复上庄
            return Code.REPEAT_OP;
        }
        long roomCreator = room.getCreator();
        if (roomCreator == playerId) {
            return Code.FAIL;
        }
        RoomCfg roomCfg = getGameController().getGameDataVo().getRoomCfg();
        if (roomCfg.getMinBankerAmount() != null && roomCfg.getMinBankerAmount().size() > 1) {
            int minBankerAmount = roomCfg.getMinBankerAmount().get(1);
            // 请求的预付金小于最低可以配置的金额
            if (predictCostGold < minBankerAmount) {
                return Code.PARAM_ERROR;
            }
        }
        // 检查玩家金币是否足够
        GamePlayer gamePlayer = getGameController().getGamePlayer(playerId);
        // 判断金币是否足够
        if (gamePlayer.getGold() < predictCostGold) {
            return Code.NOT_ENOUGH;
        }
        // 保存房间数据
        CommonResult<? extends Room> result = roomDao.doSave(room.getGameType(), room.getId(), (r) -> {
            r.addBankerSupply(playerId, predictCostGold);
        });
        this.room = (FriendRoom) result.data;
        return Code.SUCCESS;
    }
}
