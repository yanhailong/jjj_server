package com.jjg.game.room.friendroom;

import com.jjg.game.common.data.DataSaveCallback;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.*;
import com.jjg.game.room.base.ERoomItemReason;
import com.jjg.game.room.controller.AbstractRoomController;
import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.room.message.FriendRoomMessageBuilder;
import com.jjg.game.room.message.resp.ResBankerApplyListInFriendRoom;
import com.jjg.game.room.message.struct.ApplyBankPlayerInfo;
import com.jjg.game.sampledata.bean.RoomCfg;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author 2CL
 */
public abstract class AbstractFriendRoomController<RC extends RoomCfg> extends AbstractRoomController<RC, FriendRoom> {

    public AbstractFriendRoomController(Class<? extends RoomPlayer> roomPlayerClazz, FriendRoom room) {
        super(roomPlayerClazz, room);
    }

    @Override
    public boolean continueGame() {
        boolean continueGameRes = super.continueGame();
        roomDao.doSave(room.getGameType(), room.getId(), new DataSaveCallback<>() {
            @Override
            public void updateData(FriendRoom dataEntity) {
            }

            @Override
            public Boolean updateDataWithRes(FriendRoom dataEntity) {
                dataEntity.setStatus(0);
                dataEntity.setPauseTime(0);
                return true;
            }
        });
        return continueGameRes;
    }

    @Override
    public void pauseGame() {
        super.pauseGame();
        roomDao.doSave(room.getGameType(), room.getId(), new DataSaveCallback<>() {
            @Override
            public void updateData(FriendRoom dataEntity) {
            }

            @Override
            public Boolean updateDataWithRes(FriendRoom dataEntity) {
                dataEntity.setStatus(1);
                dataEntity.setPauseTime(System.currentTimeMillis());
                return true;
            }
        });
    }

    @Override
    public void stopGame() {
        // 保存房间为解散中
        roomDao.doSave(room.getGameType(), room.getId(), new DataSaveCallback<>() {
            @Override
            public void updateData(FriendRoom dataEntity) {
            }

            @Override
            public Boolean updateDataWithRes(FriendRoom dataEntity) {
                dataEntity.setStatus(2);
                return true;
            }
        });
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
        // 好友房，房间庄家不能为空，否则不能继续，如果庄家准备金不足，且
        return !room.getBankerPredicateMap().isEmpty();
    }

    @Override
    public void onRoomCantContinue() {
        super.onRoomCantContinue();
    }

    /**
     * 申请成为庄家
     */
    public int supplyBeBanker(long playerId, long predictCostGold) {
        if (room.roomBankerId() == playerId) {
            // 重复上庄
            return Code.REPEAT_OP;
        }
        // 房间创建者
        long roomCreator = room.getCreator();
        if (roomCreator == playerId) {
            return Code.FAIL;
        }
        return addBankerPredicateGold(playerId, predictCostGold);
    }

    /**
     * 取消成为庄家
     *
     * @param playerId 玩家ID
     * @return 取消结果
     */
    public int cancelBeBanker(long playerId) {
        // 如果当前玩家不为庄家
        if (room.roomBankerId() != playerId) {
            return Code.PARAM_ERROR;
        }
        // 查询玩家当前的金币
        long bankerResetGold = room.roomBankerResetGold();
        // 将玩家移除
        CommonResult<? extends Room> result = roomDao.doSave(room.getGameType(), room.getId(),
            new DataSaveCallback<>() {
                @Override
                public void updateData(FriendRoom dataEntity) {
                }

                @Override
                public Boolean updateDataWithRes(FriendRoom dataEntity) {
                    return dataEntity.removeBanker() != null;
                }
            });
        // 如果添加不成功
        if (!result.success()) {
            log.error("下庄失败，res：更新房间，code：{} {}", result.code, getRoom().logStr());
            return result.code;
        }
        this.room = (FriendRoom) result.data;
        // 添加未使用完的准备金
        int codeRes = gameController.addGold(playerId, bankerResetGold,
            ERoomItemReason.FRIEND_ROOM_CANCEL_BANKER_ADD_GOLD.withCfgId(room.getRoomCfgId()));
        log.info("玩家：{} 下庄成功, 添加剩余准备金：{}", playerId, bankerResetGold);
        // 取消上庄后，需要重置上庄次数
        gameController.getGameDataVo().setBeBankerTimes(0);
        return codeRes;
    }

    @Override
    public CommonResult<FriendRoom> onPlayerLeaveRoom(PlayerController playerController) {
        // 需要先下庄
        cancelBeBanker(playerController.playerId());
        return super.onPlayerLeaveRoom(playerController);
    }

    /**
     * 请求上庄列表
     */
    public void reqBankerList(PlayerController playerController) {
        ResBankerApplyListInFriendRoom res = new ResBankerApplyListInFriendRoom(Code.SUCCESS);
        Map<Long, Long> bankerPredicateMap = room.getBankerPredicateMap();
        List<ApplyBankPlayerInfo> applyBankPlayerInfos = new ArrayList<>();
        ApplyBankPlayerInfo bankPlayerInfo = null;
        for (Map.Entry<Long, Long> entry : bankerPredicateMap.entrySet()) {
            ApplyBankPlayerInfo playerInfo = new ApplyBankPlayerInfo();
            GamePlayer gamePlayer = gameController.getGamePlayer(entry.getKey());
            playerInfo.basePlayerInfo = FriendRoomMessageBuilder.buildFriendRoomPlayerInfo(gamePlayer);
            playerInfo.predictCostGold = entry.getValue();
            if (bankPlayerInfo == null) {
                bankPlayerInfo = playerInfo;
            } else {
                applyBankPlayerInfos.add(playerInfo);
            }
        }
        res.bankPlayerInfo = bankPlayerInfo;
        res.applyBankPlayerInfos = applyBankPlayerInfos;
        playerController.send(res);
    }


    /**
     * 申请成为庄家
     */
    public int bankerEditPredicateGold(long playerId, long predictCostGold) {
        if (room.roomBankerId() != playerId) {
            // 不是庄家
            return Code.PARAM_ERROR;
        }
        return addBankerPredicateGold(playerId, predictCostGold);
    }

    /**
     * 添加准备金
     */
    private int addBankerPredicateGold(long playerId, long predictCostGold) {
        RoomCfg roomCfg = getGameController().getGameDataVo().getRoomCfg();
        if (roomCfg.getMinBankerAmount() != null && roomCfg.getMinBankerAmount().size() > 1) {
            int minBankerAmount = roomCfg.getMinBankerAmount().get(1);
            // 请求的预付金小于最低可以配置的金额
            if (predictCostGold < minBankerAmount) {
                return Code.PARAM_ERROR;
            }
        }
        // 扣除金币
        int deductedRes = gameController.deductGold(playerId, predictCostGold,
            ERoomItemReason.FRIEND_ROOM_APPLY_BANKER_DEDUCT_PREDICATE.withCfgId(roomCfg.getId()));
        if (deductedRes != Code.SUCCESS) {
            return deductedRes;
        }
        // 保存房间数据
        CommonResult<? extends Room> result = roomDao.doSave(room.getGameType(), room.getId(),
            new DataSaveCallback<>() {
                @Override
                public void updateData(FriendRoom dataEntity) {
                }

                @Override
                public Boolean updateDataWithRes(FriendRoom dataEntity) {
                    dataEntity.addBankerSupply(playerId, predictCostGold);
                    return true;
                }
            });
        if (result.code != Code.SUCCESS) {
            return result.code;
        }
        log.info("玩家：{} 添加准备金：{}", playerId, predictCostGold);
        this.room = (FriendRoom) result.data;
        return Code.SUCCESS;
    }
}
