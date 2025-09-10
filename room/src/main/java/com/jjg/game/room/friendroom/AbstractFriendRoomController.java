package com.jjg.game.room.friendroom;

import com.jjg.game.common.data.DataSaveCallback;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.constant.GlobalSampleConstantId;
import com.jjg.game.core.data.*;
import com.jjg.game.core.data.FriendRoom;
import com.jjg.game.core.data.RoomPlayer;
import com.jjg.game.core.utils.ItemUtils;
import com.jjg.game.core.utils.SampleDataUtils;
import com.jjg.game.room.base.EGameState;
import com.jjg.game.room.base.ERoomItemReason;
import com.jjg.game.room.controller.AbstractRoomController;
import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.room.message.FriendRoomMessageBuilder;
import com.jjg.game.room.message.RoomMessageBuilder;
import com.jjg.game.room.message.resp.NotifyFriendRoomDataChange;
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
public abstract class AbstractFriendRoomController<RC extends RoomCfg, R extends FriendRoom>
    extends AbstractRoomController<RC, R> {

    public AbstractFriendRoomController(Class<? extends RoomPlayer> roomPlayerClazz, R room) {
        super(roomPlayerClazz, room);
    }

    @Override
    public <G extends Room> void initial(G room) {
        super.initial(room);
        // 在初始化完成后，保存房间的游戏运行状态
        CommonResult<R> result = roomDao.doSave(room.getGameType(), room.getId(), new DataSaveCallback<R>() {
            @Override
            public void updateData(R dataEntity) {

            }

            @Override
            public Boolean updateDataWithRes(FriendRoom dataEntity) {
                dataEntity.setInGaming(true);
                return true;
            }
        });
        if (result.success()) {
            this.room = result.data;
        }
    }

    @Override
    public boolean tryContinueGame() {
        boolean continueGameRes = super.tryContinueGame();
        if (continueGameRes) {
            CommonResult<R> result = roomDao.doSave(room.getGameType(), room.getId(), new DataSaveCallback<>() {
                @Override
                public void updateData(R dataEntity) {
                }

                @Override
                public Boolean updateDataWithRes(FriendRoom dataEntity) {
                    if (dataEntity.getPauseTime() == 0) {
                        return false;
                    }
                    // 动态加上时间
                    long resetTime = dataEntity.getOverdueTime() - dataEntity.getPauseTime();
                    long curTime = System.currentTimeMillis();
                    dataEntity.setOverdueTime(curTime + resetTime);
                    dataEntity.setStatus(1);
                    dataEntity.setPauseTime(0);
                    return true;
                }
            });
            if (result.success()) {
                this.room = result.data;
            } else {
                return false;
            }
            // 如果能开始需要更新房间最新消息
            broadFriendRoomChange();
        } else {
            // 如果房间刚开始，则需要尝试启动游戏
            if (checkRoomCanContinue() && gameController.checkRoomCanStart()) {
                log.debug("尝试继续游戏，处于游戏开始阶段，准备开始游戏");
                startGame();
                // 房间刚开始，启动成功后需要广播房间数据
                broadFriendRoomChange();
            }
        }
        return continueGameRes;
    }

    @Override
    public void pauseGame() {
        super.pauseGame();
        CommonResult<R> result = roomDao.doSave(room.getGameType(), room.getId(), new DataSaveCallback<>() {
            @Override
            public void updateData(R dataEntity) {
            }

            @Override
            public Boolean updateDataWithRes(FriendRoom dataEntity) {
                dataEntity.setStatus(2);
                dataEntity.setPauseTime(System.currentTimeMillis());
                return true;
            }
        });
        if (result.success()) {
            this.room = result.data;
        }
    }

    @Override
    protected CommonResult<? extends Room> checkRoomCanJoin(PlayerController playerController) {
        // 房间不为运行状态不能加入
        if (room.getStatus() != 1) {
            return new CommonResult<>(Code.FORBID);
        }
        return super.checkRoomCanJoin(playerController);
    }

    @Override
    public void stopGame() {
        LinkedHashMap<Long, Long> applyBankers = getRoom().getBankerPredicateMap();
        if (applyBankers != null && !applyBankers.isEmpty()) {
            LinkedHashMap<Long, Long> applyBankersCopy = new LinkedHashMap<>(applyBankers);
            for (Map.Entry<Long, Long> entry : applyBankersCopy.entrySet()) {
                // 添加未使用完的准备金
                gameController.addItem(entry.getKey(), entry.getValue(),
                    ERoomItemReason.FRIEND_ROOM_CANCEL_BANKER_ADD_GOLD.withCfgId(getRoom().getRoomCfgId()));
            }
        }
        super.stopGame();
    }

    /**
     * 在下一轮开始时，进行销毁
     */
    public void destroyOnNextRoundStart() {
        // 重新刷新room数据
        room = roomDao.getRoom(room.getGameType(), room.getId());
        // 如果房间处于未开启游戏,直接走销毁逻辑
        if (gameController.getGameState() == EGameState.INIT_DONE) {
            log.info("房间处于未开启状态，直接解散");
            gameDestroy(true);
        }
    }

    @Override
    public void gameDestroy(boolean closeByPlayer) {
        // 标记游戏为销毁中，
        gameController.gameDestroy(closeByPlayer);
    }

    @Override
    public void disbandRoom(Boolean closeByPlayer) {
        super.disbandRoom(closeByPlayer);
        // 解散完成后需要将剩余的准备金返给玩家
        if (room.getStatus() == 3) {
            int gameTransactionItemId = gameController.getGameTransactionItemId();
            roomManager.getPlayerPackService().addItem(
                room.getCreator(),
                gameTransactionItemId,
                room.getPredictCostGoldNum(),
                ERoomItemReason.FRIEND_ROOM_DISBAND_REBACK_GOLD.name());
        }
    }

    /**
     * 不能让机器人加入房间
     */
    @Override
    protected void checkRobotJoinRoom() {
    }

    @Override
    public boolean checkRoomCanContinue() {
        // 如果房间状态为解散中.直接暂停游戏
        if (room.getStatus() == 3) {
            return false;
        }
        // 需要检查房间时长
        if (room.getOverdueTime() < System.currentTimeMillis()) {
            // 如果时间到期且没有开启自动续费，先暂停游戏
            if (!room.isAutoRenewal()) {
                return false;
            }
            // 自动续费，检查玩家金币是否足够
            RoomExpendCfg roomExpendCfg = GameDataManager.getRoomExpendCfg(room.getRoomExpendId());
            if (roomExpendCfg == null) {
                return false;
            }
            List<Integer> requiredMoney = roomExpendCfg.getRequiredMoney();
            int gold = requiredMoney.get(1);
            // 时长，毫秒
            long durationTime = (long) roomExpendCfg.getDurationTime() * TimeHelper.ONE_MINUTE_OF_MILLIS;
            // 从房间底庄中扣除金币，如果不足直接暂停游戏
            if (gold > room.getPredictCostGoldNum()) {
                // 自动续费失败，房间准备金不足
                log.info("自动续费失败，房间准备金不足: need: {} rest: {}", gold, room.getPredictCostGoldNum());
                return false;
            }
            // 续费时长
            CommonResult<R> result = roomDao.doSave(room, new DataSaveCallback<R>() {
                @Override
                public void updateData(R dataEntity) {
                }

                @Override
                public Boolean updateDataWithRes(FriendRoom dataEntity) {
                    dataEntity.setOverdueTime(System.currentTimeMillis() + durationTime);
                    // TODO日志
                    dataEntity.setPredictCostGoldNum(dataEntity.getPredictCostGoldNum() - gold);
                    return true;
                }
            });
            if (result.success()) {
                this.room = result.data;
            }
        }
        int minBankerAmount = FriendRoomSampleUtils.getRoomMinBankerAmount(roomCfg.getId());
        // 好友房，如果房间庄家不为空或者房间房主底注大于最小上庄金币
        return !room.getBankerPredicateMap().isEmpty() || room.getPredictCostGoldNum() > minBankerAmount;
    }

    @Override
    public void onRoomCantContinue() {
        super.onRoomCantContinue();
        // 当房间处于销毁状态时，直接销毁房间
        if (room.getStatus() == 3) {
            gameDestroy(true);
        }
    }

    /**
     * 申请成为庄家
     */
    public int supplyBeBanker(long playerId, long predictCostGold) {
        if (room.roomBankerId() == playerId) {
            // 重复上庄
            return Code.REPEAT_OP;
        }
        // 房主不能成为庄家
        long roomCreator = room.getCreator();
        if (roomCreator == playerId) {
            return Code.ROOM_CREATOR_CANT_BE_BANKER;
        }
        int addRes = addBankerPredicateGold(playerId, predictCostGold);
        // 如果申请成功，且当前游戏处于暂停状态，需要继续游戏
        if (addRes == Code.SUCCESS) {
            // 尝试继续游戏
            tryContinueGame();
        }
        return addRes;
    }

    /**
     * 广播庄家改变，直接广播庄家上庄
     */
    public void broadFriendRoomChange() {
        NotifyFriendRoomDataChange notify = new NotifyFriendRoomDataChange();
        notify.bankerPlayerId = room.roomBankerId();
        notify.bankerPredicateCostGold = room.getPredictCostGoldNum();
        notify.roomCreatorPredicateCostGold = room.getPredictCostGoldNum();
        broadcastToPlayers(RoomMessageBuilder.newBuilder().setData(notify).toAllPlayer());
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
            return Code.SUCCESS;
        }
        int code = gameController.addItem(
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
        CommonResult<R> result = roomDao.doSave(room.getGameType(), room.getId(),
            new DataSaveCallback<R>() {
                @Override
                public void updateData(R dataEntity) {
                }

                @Override
                public Boolean updateDataWithRes(FriendRoom dataEntity) {
                    return dataEntity.removeBanker() != null;
                }
            });
        // 如果下庄不成功
        if (!result.success()) {
            log.error("下庄失败，res：更新房间，code：{} {}", result.code, getRoom().logStr());
            return result.code;
        }
        this.room = result.data;
        // 通知房间改变
        broadFriendRoomChange();
        // 添加未使用完的准备金
        int codeRes =
            gameController.addItem(playerId, bankerResetGold, eRoomItemReason.withCfgId(room.getRoomCfgId()));
        log.info("玩家：{} 下庄成功, 添加剩余准备金：{}", playerId, bankerResetGold);
        // 取消上庄后，需要重置上庄次数
        gameController.getGameDataVo().setBeBankerTimes(0);
        return codeRes;
    }

    @Override
    public CommonResult<R> onPlayerLeaveRoom(PlayerController playerController) {
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
            return Code.CANT_EDIT_BANKER_GOLD;
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
        int transactionItemId = gameController.getGameTransactionItemId();
        res.bankerResetGold =
            transactionItemId == ItemUtils.getDiamondItemId() ? gamePlayer.getDiamond() : gamePlayer.getGold();
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
            // 扣除道具
            int removeItemResult =
                gameController.deductItem(
                    playerId,
                    predictCostGold,
                    ERoomItemReason.FRIEND_ROOM_APPLY_BANKER_DEDUCT_PREDICATE.name());
            log.debug("扣除道具：{} {}", roomCfg.getMinBankerAmount().get(0), predictCostGold);
            if (removeItemResult != Code.SUCCESS) {
                return removeItemResult;
            }
            // 保存房间数据
            CommonResult<R> result = roomDao.doSave(room.getGameType(), room.getId(),
                new DataSaveCallback<>() {
                    @Override
                    public void updateData(R dataEntity) {
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
            this.room = result.data;
        }
        log.info("玩家：{} 添加准备金：{}", playerId, predictCostGold);
        return Code.SUCCESS;
    }

    public void deductBankerGold(long bankerFlowing) {
        CommonResult<R> result = roomDao.doSave(room, new DataSaveCallback<>() {
            @Override
            public void updateData(R dataEntity) {

            }

            @Override
            public Boolean updateDataWithRes(FriendRoom dataEntity) {
                dataEntity.deductBankerGold(Math.abs(bankerFlowing));
                return true;
            }
        });
        if (result.success()) {
            this.room = result.data;
        }
    }
}
