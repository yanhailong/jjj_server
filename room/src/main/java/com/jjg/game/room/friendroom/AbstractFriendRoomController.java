package com.jjg.game.room.friendroom;

import com.jjg.game.common.data.DataSaveCallback;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.constant.GlobalSampleConstantId;
import com.jjg.game.core.data.*;
import com.jjg.game.core.utils.SampleDataUtils;
import com.jjg.game.room.base.EGameState;
import com.jjg.game.room.base.ERoomItemReason;
import com.jjg.game.room.controller.AbstractRoomController;
import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.room.message.FriendRoomMessageBuilder;
import com.jjg.game.room.message.resp.ResBankerApplyListInFriendRoom;
import com.jjg.game.room.message.resp.ResEditBankerPredicateGold;
import com.jjg.game.room.message.struct.ApplyBankPlayerInfo;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.RoomCfg;
import com.jjg.game.sampledata.bean.RoomExpendCfg;

import java.util.ArrayList;
import java.util.LinkedHashMap;
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
                long resetTime = dataEntity.getOverdueTime() - dataEntity.getPauseTime();
                dataEntity.setOverdueTime(dataEntity.getPauseTime() + resetTime);
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

        LinkedHashMap<Long, Long> applyBankers = getRoom().getBankerPredicateMap();
        if (applyBankers != null && !applyBankers.isEmpty()) {
            LinkedHashMap<Long, Long> applyBankersCopy = new LinkedHashMap<>(applyBankers);
            for (Map.Entry<Long, Long> entry : applyBankersCopy.entrySet()) {
                // 添加未使用完的准备金
                gameController.addGold(entry.getKey(), entry.getValue(),
                    ERoomItemReason.FRIEND_ROOM_CANCEL_BANKER_ADD_GOLD.withCfgId(getRoom().getRoomCfgId()));
            }
        }
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
        // 需要检查房间时长
        if (room.getOverdueTime() < System.currentTimeMillis()) {
            // 如果时间到期且没有开启自动续费，先暂停游戏
            if (!room.isAutoRenewal()) {
                return false;
            }
            // 自动续费，检查玩家金币是否足够
            RoomExpendCfg roomExpendCfg = GameDataManager.getRoomExpendCfg(room.getRoomExpendId());
            List<Integer> requiredMoney = roomExpendCfg.getRequiredMoney();
            int gold = requiredMoney.get(1);
            long roomCreator = room.getCreator();
            // 时长，毫秒
            long durationTime = (long) roomExpendCfg.getDurationTime() * TimeHelper.ONE_MINUTE_OF_MILLIS;
            int deductCode;
            // TODO 需要整合扣除道具方法
            if (gameController.getGameDataVo().getGamePlayer(roomCreator) != null) {
                deductCode =
                    gameController.deductGold(
                        roomCreator, gold, ERoomItemReason.FRIEND_ROOM_AUTO_RENEW_TIME.withCfgId(room.getRoomCfgId()));
            } else {
                CommonResult<?> commonResult = roomManager.getPlayerService().deductGold(roomCreator, gold,
                    ERoomItemReason.FRIEND_ROOM_AUTO_RENEW_TIME.name(), room.getRoomCfgId() + "");
                deductCode = commonResult.code;
            }
            if (deductCode != Code.SUCCESS) {
                return false;
            }
            // 续费时长
            roomDao.doSave(room, new DataSaveCallback<FriendRoom>() {
                @Override
                public void updateData(FriendRoom dataEntity) {
                }

                @Override
                public Boolean updateDataWithRes(FriendRoom dataEntity) {
                    dataEntity.setOverdueTime(System.currentTimeMillis() + durationTime);
                    return true;
                }
            });
        }
        int minBankerAmount = FriendRoomSampleUtils.getRoomMinBankerAmount(roomCfg.getId());
        // 好友房，如果房间庄家不为空或者房间房主底注大于最小上庄金币
        return !room.getBankerPredicateMap().isEmpty() || room.getPredictCostGoldNum() > minBankerAmount;
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
        int addRes = addBankerPredicateGold(playerId, predictCostGold);
        // 如果申请成功，且当前游戏处于暂停状态，需要继续游戏
        if (addRes == Code.SUCCESS && gameController.getGameState() == EGameState.PAUSED) {
            // 尝试继续游戏
            continueGame();
        }
        return addRes;
    }

    /**
     * 标记玩家下庄，具体操作需要在进入下一轮开始之前
     */
    public int markBankerCancel(long playerId) {
        // 如果当前玩家是庄家直接标记
        if (room.roomBankerId() == playerId) {
            gameController.getGameDataVo().setApplyCancelBeBankerPlayer(playerId);
            return Code.SUCCESS;
        }
        // 如果当前玩家不是庄家，直接从申请列表中删除，然后再返还金币
        long playerGold = room.getApplyBankerPlayerGold(playerId);
        if (playerGold <= 0) {
            return Code.PARAM_ERROR;
        }
        int code = gameController.addGold(
            playerId, playerGold, ERoomItemReason.FRIEND_ROOM_CANCEL_BANKER_ADD_GOLD.withCfgId(room.getRoomCfgId()));
        log.info("玩家：{} 申请取消成为庄家，添加金币：{}", playerId, playerGold);
        return code;
    }

    /**
     * 取消成为庄家
     *
     * @param playerId 玩家ID
     * @return 取消结果
     */
    public int cancelBeBanker(long playerId, ERoomItemReason eRoomItemReason) {
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
        int codeRes =
            gameController.addGold(playerId, bankerResetGold, eRoomItemReason.withCfgId(room.getRoomCfgId()));
        log.info("玩家：{} 下庄成功, 添加剩余准备金：{}", playerId, bankerResetGold);
        // 取消上庄后，需要重置上庄次数
        gameController.getGameDataVo().setBeBankerTimes(0);
        return codeRes;
    }

    @Override
    public CommonResult<FriendRoom> onPlayerLeaveRoom(PlayerController playerController) {
        long roomBankerId = getRoom().roomBankerId();
        // 如果庄家离开房间，需要下庄
        if (playerController.playerId() == roomBankerId) {
            cancelBeBanker(playerController.playerId(), ERoomItemReason.FRIEND_ROOM_LEAVE_ROOM_ADD_GOLD);
        }
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
        res.beBankerTimes = gameController.getGameDataVo().getBeBankerTimes();
        res.maxBankerTimes = SampleDataUtils.getIntGlobalData(GlobalSampleConstantId.BE_BANKER_MAX_ROUND);
        playerController.send(res);
    }


    /**
     * 申请成为庄家
     */
    public int bankerEditPredicateGold(PlayerController playerController, long predictCostGold) {
        long playerId = playerController.playerId();
        if (room.roomBankerId() != playerId) {
            // 不是庄家
            return Code.PARAM_ERROR;
        }
        int resCode = addBankerPredicateGold(playerId, predictCostGold);
        if (resCode != Code.SUCCESS) {
            return resCode;
        }
        long newlyBankerGold = room.roomBankerResetGold();
        long banker = room.roomBankerId();
        GamePlayer gamePlayer = gameController.getGameDataVo().getGamePlayer(banker);
        ResEditBankerPredicateGold res = new ResEditBankerPredicateGold(Code.SUCCESS);
        res.newlyPredicateGold = newlyBankerGold;
        res.bankerResetGold = gamePlayer.getGold();
        playerController.send(res);
        return resCode;
    }

    /**
     * 添加准备金
     */
    private int addBankerPredicateGold(long playerId, long predictCostGold) {
        RoomCfg roomCfg = getGameController().getGameDataVo().getRoomCfg();
        if (roomCfg.getMinBankerAmount() != null && roomCfg.getMinBankerAmount().size() > 1) {
            int minBankerAmount = roomCfg.getMinBankerAmount().get(1);
            // 请求的预付金币小于最低可以配置的金币
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

    public void deductBankerGold(long bankerFlowing) {
        roomDao.doSave(room, new DataSaveCallback<FriendRoom>() {
            @Override
            public void updateData(FriendRoom dataEntity) {

            }

            @Override
            public Boolean updateDataWithRes(FriendRoom dataEntity) {
                dataEntity.deductBankerGold(Math.abs(bankerFlowing));
                return true;
            }
        });
    }
}
