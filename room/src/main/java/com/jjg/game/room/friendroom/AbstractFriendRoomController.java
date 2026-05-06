package com.jjg.game.room.friendroom;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSON;
import com.jjg.game.common.concurrent.BaseHandler;
import com.jjg.game.common.data.DataSaveCallback;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.common.utils.WheelTimerUtil;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.constant.GameConstant;
import com.jjg.game.core.constant.GlobalSampleConstantId;
import com.jjg.game.core.data.*;
import com.jjg.game.core.utils.SampleDataUtils;
import com.jjg.game.room.base.EGameState;
import com.jjg.game.room.controller.AbstractRoomController;
import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.room.message.FriendRoomMessageBuilder;
import com.jjg.game.room.message.RoomMessageBuilder;
import com.jjg.game.room.message.resp.NotifyFriendRoomDataChange;
import com.jjg.game.room.message.resp.ResBankerApplyListInFriendRoom;
import com.jjg.game.room.message.resp.ResEditBankerPredicateGold;
import com.jjg.game.room.message.struct.ApplyBankPlayerInfo;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.GlobalConfigCfg;
import com.jjg.game.sampledata.bean.RoomCfg;
import com.jjg.game.sampledata.bean.RoomExpendCfg;
import com.jjg.game.sampledata.bean.WarehouseCfg;
import io.netty.util.Timeout;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author 2CL
 */
public abstract class AbstractFriendRoomController<RC extends RoomCfg, R extends FriendRoom>
        extends AbstractRoomController<RC, R> {

    private final Map<Long, GamePlayer> bankerPredicateInfo = new HashMap<>();
    //自动续费timer
    private volatile Timeout autoRenewalTimeout;

    public AbstractFriendRoomController(Class<? extends RoomPlayer> roomPlayerClazz, R room) {
        super(roomPlayerClazz, room);
    }


    private void autoRenewalCheck() {
        getRoomProcessor().tryPublish(0, new BaseHandler<String>() {
            @Override
            public void action() {
                checkAutoRenewal();
            }
        }.setHandlerParamWithSelf("autoRenewalCheck"));
    }

    private void checkAutoRenewal() {
        // 如果时间到期且没有开启自动续费，先暂停游戏
        if (!room.isAutoRenewal() || roomManager.getNodeManager().nodeConfig.waitClose()) {
            log.info("房间：{} 时长到期", room.logStr());
            return;
        }
        if (room.getStatus() != 1) {
            log.info("房间：{} 暂停 不进行自动续费", room.logStr());
            return;
        }
        if (room.getRoomPlayers().isEmpty()) {
            onStopAutoRenewal(1);
            return;
        }
        int renewalCode = renewalOverTime();
        if (renewalCode != Code.SUCCESS) {
            log.info("房间自动续费失败 room:{} code:{}", room.logStr(), renewalCode);
            if (renewalCode == Code.AMOUNT_OF_RESERVES_IS_NOT_ENOUGHT) {
                onStopAutoRenewal(2);
            }
        }
    }

    @Override
    public void cancelWheelTimers() {
        super.cancelWheelTimers();
        cancelAutoRenewalTimer();
    }

    /**
     * 取消自动续费timer
     */
    private void cancelAutoRenewalTimer() {
        log.info("取消自动续费定时器 roomId:{}", room.getId());
        Timeout tickTimeout = autoRenewalTimeout;
        if (tickTimeout != null && !tickTimeout.isCancelled()) {
            tickTimeout.cancel();
        }
        autoRenewalTimeout = null;
    }

    @Override
    public <G extends Room> void initial(G room) {
        super.initial(room);
        // 在初始化完成后，保存房间的游戏运行状态
        CommonResult<R> result = roomDao.doSave(room.getGameType(), room.getId(), new DataSaveCallback<>() {
            @Override
            public void updateData(R dataEntity) {

            }

            @Override
            public boolean updateDataWithRes(FriendRoom dataEntity) {
                dataEntity.setInGaming(true);
                return true;
            }
        });
        if (result.success()) {
            this.room = result.data;
            if (room instanceof FriendRoom friendRoom) {
                LinkedHashMap<Long, Long> bankerPredicateMap = friendRoom.getBankerPredicateMap();
                if (CollectionUtil.isNotEmpty(bankerPredicateMap)) {
                    List<Player> players = getRoomManager().getPlayerService().multiGetPlayer(bankerPredicateMap.keySet());
                    for (Player player : players) {
                        GamePlayer gamePlayer = new GamePlayer();
                        gamePlayer.fromPlayer(player);
                        bankerPredicateInfo.put(player.getId(), gamePlayer);
                    }
                }
            }
        }
        loadRenewalTimer();
    }

    @Override
    protected void tryStartGameOnPlayerJoinIn(PlayerController playerController) {
        boolean checkRoomCanContinue = checkRoomCanContinue();
        if (!playerController.isRobotPlayer()) {
            log.info("尝试启动游戏：玩家：{} 房间是否可以开始：{} 游戏是否可以开始：{} 房间状态：{} 游戏当前状态：{}",
                    playerController.playerId(),
                    checkRoomCanContinue,
                    gameController.checkRoomCanStart(),
                    room.getStatus(),
                    gameController.getGameState()
            );
        }
        // 检查房间开始的逻辑，如果房间游戏暂停需要尝试启动游戏
        if (checkRoomCanContinue) {
            // 检查通过开始游戏
            tryContinueGame();
        }
    }

    public Map<Long, GamePlayer> getBankerPredicateInfo() {
        return bankerPredicateInfo;
    }

    protected void loadRenewalTimer() {
        log.info("添加自动续费定时器 roomId:{}", room.getId());
        if (autoRenewalTimeout != null) {
            cancelAutoRenewalTimer();
        }
        if (room.getStatus() != 1 || !room.isAutoRenewal()) {
            return;
        }
        long currentTimeMillis = System.currentTimeMillis();
        if (room.getOverdueTime() > currentTimeMillis) {
            //提前100毫秒
            long delay = room.getOverdueTime() - currentTimeMillis;
            autoRenewalTimeout = WheelTimerUtil.schedule(this::autoRenewalCheck, delay, TimeUnit.MILLISECONDS);
        }
    }


    /**
     * 更新运行中的好友房数据。
     */
    public CommonResult<FriendRoom> updateFriendRoom(long playerId, int addTime, boolean autoRenewal, long predictCostGoldNum, String roomAliasName) {
        if (playerId != room.getCreator()) {
            log.warn("修改好友房失败,房主不匹配 playerId = {},roomId = {},roomCreator = {}", playerId, room.getId(), room.getCreator());
            return new CommonResult<>(Code.FORBID);
        }
        CommonResult<R> result = roomDao.doSave(room.getGameType(), room.getId(), new DataSaveCallback<>() {
            @Override
            public void updateData(R dataEntity) {
            }

            @Override
            public boolean updateDataWithRes(FriendRoom dataEntity) {
                if (roomAliasName != null && !roomAliasName.isEmpty()) {
                    dataEntity.setAliasName(roomAliasName);
                }
                dataEntity.setPredictCostGoldNum(dataEntity.getPredictCostGoldNum() + predictCostGoldNum);
                dataEntity.setPool(dataEntity.getPool() + predictCostGoldNum);
                dataEntity.setAutoRenewal(autoRenewal);
                if (addTime > 0 || predictCostGoldNum > 0) {
                    dataEntity.setSendPauseRenewalMail(false);
                }
                if (addTime > 0) {
                    long curTime = System.currentTimeMillis();
                    if (dataEntity.getOverdueTime() < curTime) {
                        dataEntity.setOverdueTime(curTime + addTime);
                    } else {
                        dataEntity.setOverdueTime(dataEntity.getOverdueTime() + addTime);
                    }
                }
                return true;
            }
        });
        if (!result.success()) {
            return new CommonResult<>(result.code);
        }
        this.room = result.data;
        if (predictCostGoldNum > 0 && this.room instanceof BetFriendRoom) {
            getRoomDao().modifyRoomPool(this.room.getGameType(), this.room.getId(), predictCostGoldNum);
        }
        loadRenewalTimer();
        if (addTime > 0 || predictCostGoldNum > 0) {
            tryContinueGame();
            gameController.onContinueGameAction();
        } else if (room.isAutoRenewal() && room.getStatus() == 1 && room.getOverdueTime() <= System.currentTimeMillis()) {
            int code = renewalOverTime();
            if (code == Code.SUCCESS && gameController.getGameState() == EGameState.PAUSED) {
                tryContinueGame();
                gameController.onContinueGameAction();
            }
        }
        return new CommonResult<>(Code.SUCCESS, this.room);
    }

    public long getCoolDownTime() {
        GlobalConfigCfg globalConfigCfg = GameDataManager.getGlobalConfigCfg(GlobalSampleConstantId.INVITATION_REFRESH_INTERVAL);
        long timeMillis = System.currentTimeMillis();
        if (globalConfigCfg != null) {
            return timeMillis + (long) globalConfigCfg.getIntValue() * TimeHelper.ONE_MINUTE_OF_MILLIS;
        }
        return timeMillis + TimeHelper.ONE_MINUTE_OF_MILLIS;
    }

    /**
     * 继续房间
     *
     * @return 错误码
     */
    public int continueRoom() {
        CommonResult<R> result = roomDao.doSave(room.getGameType(), room.getId(), new DataSaveCallback<>() {
            @Override
            public void updateData(FriendRoom dataEntity) {
            }

            @Override
            public boolean updateDataWithRes(FriendRoom dataEntity) {
                dataEntity.setStatus(1);
                dataEntity.setOperationCoolingTime(getCoolDownTime());
                return true;
            }
        });
        if (result.success()) {
            room = result.data;
        }
        return result.code;
    }


    @Override
    public boolean tryContinueGame() {
        log.warn("请求尝试开启游戏");
        // 更新房间数据
        if (this.room.getStatus() != 1) {
            log.warn("请求开始游戏，但是房间状态不为 1");
            return false;
        }
        boolean continueGameRes = super.tryContinueGame();
        if (continueGameRes) {
            CommonResult<R> result = roomDao.doSave(room.getGameType(), room.getId(), new DataSaveCallback<>() {
                @Override
                public void updateData(R dataEntity) {
                }

                @Override
                public boolean updateDataWithRes(FriendRoom dataEntity) {
                    if (dataEntity.getPauseTime() != 0) {
                        // 动态加上时间
                        long resetTime = dataEntity.getOverdueTime() - dataEntity.getPauseTime();
                        long curTime = System.currentTimeMillis();
                        dataEntity.setOverdueTime(curTime + resetTime);
                    }
                    dataEntity.setStatus(1);
                    dataEntity.setPauseTime(0);
                    dataEntity.setSendPauseRenewalMail(false);
                    return true;
                }
            });
            if (result.success()) {
                this.room = result.data;
                loadRenewalTimer();
            } else {
                log.warn("请求尝试开启游戏，但是房间更新失败");
                return false;
            }
            // 如果能开始需要更新房间最新消息
            broadFriendRoomChange();
        } else {
            log.warn("请求尝试开启游戏，尝试失败，准备尝试开始游戏：{}", getRoom().logStr());
            // 如果房间刚开始，则需要尝试启动游戏
            boolean roomCanContinue = checkRoomCanContinue();
            if (roomCanContinue && gameController.checkRoomCanStart()) {
                log.debug("尝试继续游戏，处于游戏开始阶段，准备开始游戏");
                startGame();
                // 房间刚开始，启动成功后需要广播房间数据
                broadFriendRoomChange();
            }
        }
        return continueGameRes;
    }

    /**
     * 添加房间准备金
     */
    public void addRoomPredicateGold(long addPredicateGold) {
        if (addPredicateGold < 0) {
            return;
        }
        CommonResult<R> result = roomDao.doSave(room.getGameType(), room.getId(), new DataSaveCallback<>() {
            @Override
            public void updateData(R dataEntity) {

            }

            @Override
            public boolean updateDataWithRes(FriendRoom dataEntity) {
                dataEntity.addBankerBankerPredicateItem(addPredicateGold);
                if (addPredicateGold > 0) {
                    dataEntity.setSendPauseRenewalMail(false);
                }
                return true;
            }
        });
        if (result.success()) {
            this.room = result.data;
        }
    }

    @Override
    public void pauseGame() {
        // 已经暂停不能再请求暂停游戏，否则会出现异常
        if (gameController.getGameState() == EGameState.PAUSED) {
            return;
        }
        // 暂停游戏
        CommonResult<R> result = roomDao.doSave(room.getGameType(), room.getId(), new DataSaveCallback<>() {
            @Override
            public void updateData(FriendRoom dataEntity) {
            }

            @Override
            public boolean updateDataWithRes(FriendRoom dataEntity) {
                dataEntity.setStatus(2);
                dataEntity.setPauseTime(System.currentTimeMillis());
                dataEntity.setOperationCoolingTime(getCoolDownTime());
                return true;
            }
        });
        if (!result.success()) {
            return;
        }
        this.room = result.data;
        super.pauseGame();
        cancelAutoRenewalTimer();
    }

    @Override
    protected CommonResult<R> checkRoomCanJoin(PlayerController playerController) {
        // 房间不为运行状态不能加入,暂停可以加入
        if (room.getStatus() != 1 && room.getStatus() != 2) {
            log.debug("玩家：{} 不能进入房间：{} 当前状态：{}", playerController.playerId(), room.getId(), room.getStatus());
            return new CommonResult<>(Code.FORBID);
        }
        return super.checkRoomCanJoin(playerController);
    }


    /**
     * 在下一轮开始时，进行销毁
     */
    public void destroyOnNextRoundStart() {
        CommonResult<R> result = roomDao.doSave(room.getGameType(), room.getId(), new DataSaveCallback<>() {
            @Override
            public void updateData(FriendRoom dataEntity) {
            }

            @Override
            public boolean updateDataWithRes(FriendRoom dataEntity) {
                dataEntity.setStatus(3);
                dataEntity.setPauseTime(System.currentTimeMillis());
                return true;
            }
        });
        if (!result.success()) {
            return;
        }
        // 重新刷新room数据
        room = result.data;
        // 如果房间处于未开启游戏,直接走销毁逻辑
        if (gameController.getGameState() == EGameState.INIT_DONE) {
            log.info("房间处于未开启状态，直接解散");
            gameDestroy(true, true);
        }
        gameController.onDestroyRoomAction();
    }

    @Override
    public void gameDestroy(boolean closeByPlayer, boolean notifyExitRoom) {
        // 标记游戏为销毁中，
        gameController.gameDestroy(closeByPlayer, notifyExitRoom);
    }

    @Override
    public void disbandRoom(Boolean closeByPlayer) {
        super.disbandRoom(closeByPlayer);
        // 解散完成后需要将剩余的准备金返给玩家
        if (room.getStatus() == 3 || roomManager.getNodeManager().nodeConfig.waitClose()) {
            int gameTransactionItemId = gameController.getGameTransactionItemId();
            int gainRatio = SampleDataUtils.getIntGlobalData(GlobalSampleConstantId.FRIEND_ROOM_DESTROY_GAIN_RATIO);
            if (room.getPredictCostGoldNum() > 0) {
                long gainGold = (long) (room.getPredictCostGoldNum() * (Math.clamp(gainRatio, 0, GameConstant.TEN_THOUSAND) / GameConstant.TEN_THOUSAND_DOUBLE));
                log.info("房间：{} 销毁返回准备金币：{} {}", room.logStr(), gainGold, room.getPredictCostGoldNum());
                sendDisbandRoomBack(room.getCreator(), gameTransactionItemId, gainGold);
            }
            if (CollectionUtil.isNotEmpty(room.getBankerPredicateMap())) {
                for (Map.Entry<Long, Long> entry : room.getBankerPredicateMap().entrySet()) {
                    log.info("房间：{} 销毁返回准备金币 playerId:{} num:{}", room.logStr(), entry.getKey(), entry.getValue());
                    sendDisbandRoomBack(entry.getKey(), gameTransactionItemId, entry.getValue());
                }
            }
        }
        //删除池子信息
        getRoomDao().removeRoomPool(room.getGameType(), room.getId());
    }

    /**
     * 发送销毁房间时返回保证金
     *
     * @param gameTransactionItemId 房间货币id
     * @param gainGold              返回金额
     * @param playerId              玩家id
     */
    public void sendDisbandRoomBack(long playerId, int gameTransactionItemId, long gainGold) {
        List<LanguageParamData> params = new ArrayList<>(2);
        WarehouseCfg warehouseCfg = GameDataManager.getWarehouseCfg(room.getRoomCfgId());
        params.add(new LanguageParamData(1, warehouseCfg.getNameid() + ""));
        params.add(new LanguageParamData(TimeHelper.getDate(System.currentTimeMillis())));

        List<Item> returnItems = List.of(new Item(gameTransactionItemId, gainGold));
        Mail mail = roomManager.getMailService().addCfgMail(playerId, 35, returnItems, params, AddType.FRIEND_ROOM_DESTROY_ROOM_BANKER_ADD_COIN);
        roomManager.getCoreLogger().roomDisband(this.room, mail.getId(), returnItems);
    }

    /**
     * 发送下庄时返回保证金
     *
     * @param gameTransactionItemId 房间货币id
     * @param gainGold              返回金额
     * @param playerId              玩家id
     */
    public void sendComeDownRoomBack(long playerId, int gameTransactionItemId, long gainGold) {
        List<LanguageParamData> params = new ArrayList<>(2);
        WarehouseCfg warehouseCfg = GameDataManager.getWarehouseCfg(room.getRoomCfgId());
        params.add(new LanguageParamData(1, warehouseCfg.getNameid() + ""));
        params.add(new LanguageParamData(TimeHelper.getDate(System.currentTimeMillis())));
        roomManager.getMailService().addCfgMail(playerId, 39, List.of(new Item(gameTransactionItemId, gainGold)), params, AddType.FRIEND_ROOM_CANCEL_BANKER_ADD_COIN);
    }

    /**
     * 不能让机器人加入房间
     */
    @Override
    protected void checkRobotJoinRoom() {
    }

    @Override
    public boolean checkRoomCanContinue() {
        broadFriendRoomChange();
        // 如果房间状态为解散中.直接暂停游戏
        if (room.getStatus() != 1) {
            return false;
        }
        long curTime = System.currentTimeMillis();
        // 需要检查房间时长
        if (room.getOverdueTime() < curTime) {
            // 如果时间到期且没有开启自动续费，先暂停游戏
            if (!room.isAutoRenewal() || roomManager.getNodeManager().nodeConfig.waitClose()) {
                log.info("房间：{} 时长到期", room.logStr());
                return false;
            }
            if (room.getRoomPlayers().isEmpty()) {
                onStopAutoRenewal(1);
                return false;
            }
            int renewalCode = renewalOverTime();
            if (renewalCode != Code.SUCCESS) {
                if (renewalCode == Code.AMOUNT_OF_RESERVES_IS_NOT_ENOUGHT) {
                    onStopAutoRenewal(2);
                }
                return false;
            }
        }
        int minBankerAmount = FriendRoomSampleUtils.getRoomMinBankerAmount(roomCfg.getId());
        long bankerResetGold = room.roomBankerResetGold();
        boolean checkAmount = (room.getPredictCostGoldNum() + bankerResetGold) >= minBankerAmount;
        if (!checkAmount) {
            log.info(" 房间准备金不足，房间即将暂停，房间准备金余额：{} 庄家：{}", room.roomBankerResetGold(), bankerResetGold);
        }
        // 好友房，如果房间庄家不为空且房间房主底注大于最小上庄金币
        return checkBankerCanNextRound() && checkAmount;
    }

    /**
     * 停止自动续费
     *
     * @param type 类型 1房间没人 2准备金不足
     */
    private void onStopAutoRenewal(int type) {
        List<LanguageParamData> params = new ArrayList<>();
        WarehouseCfg warehouseCfg = GameDataManager.getWarehouseCfg(room.getRoomCfgId());
        if (warehouseCfg == null) {
            log.error("房间：{} 时长到期,未找到配置信息", room.logStr());
            return;
        }
        CommonResult<R> result = roomDao.doSave(room, new DataSaveCallback<>() {
            @Override
            public void updateData(R dataEntity) {
            }

            @Override
            public boolean updateDataWithRes(FriendRoom dataEntity) {
                if (dataEntity.isSendPauseRenewalMail()) {
                    return false;
                }
                dataEntity.setSendPauseRenewalMail(true);
                return true;
            }
        });
        if (!result.success()) {
            return;
        }
        this.room = result.data;
        if (type == 1) {
            params.add(new LanguageParamData(1, warehouseCfg.getNameid() + ""));
            params.add(new LanguageParamData(TimeHelper.getDate(System.currentTimeMillis())));
            roomManager.getMailService().addCfgMail(room.getCreator(), 2, null, params, AddType.FRIEND_ROOM_NO_PLAYER_PAUSE_RENEWAL);
            log.info("房间：{} 时长到期,没有玩家暂停续费", room.logStr());
        } else if (type == 2) {
            // 自动续费失败，房间准备金不足
            params.add(new LanguageParamData(1, warehouseCfg.getNameid() + ""));
            params.add(new LanguageParamData(TimeHelper.getDate(System.currentTimeMillis())));
            roomManager.getMailService().addCfgMail(room.getCreator(), 3, null, params, AddType.FRIEND_ROOM_RENEWAL_FAIL);
            log.info("自动续费失败，房间准备金不足: rest: {}", room.getPredictCostGoldNum());
        }
    }

    /**
     * 续费时间
     *
     * @return 错误码
     */
    public int renewalOverTime() {
        // 自动续费，检查玩家金币是否足够
        RoomExpendCfg roomExpendCfg = getAutoRenewalCfg();
        if (roomExpendCfg == null) {
            return Code.SAMPLE_ERROR;
        }
        long currentTimeMillis = System.currentTimeMillis();
        List<Integer> requiredMoney = roomExpendCfg.getRequiredMoney();
        int itemNum = requiredMoney.get(1);
        // 时长，毫秒
        long durationTime = (long) roomExpendCfg.getDurationTime() * TimeHelper.ONE_MINUTE_OF_MILLIS;
        if (itemNum < 0 || durationTime <= 0) {
            return Code.SAMPLE_ERROR;
        }
        if (itemNum > room.getPredictCostGoldNum()) {
            return Code.AMOUNT_OF_RESERVES_IS_NOT_ENOUGHT;
        }
        if (!room.isAutoRenewal() || room.getOverdueTime() > currentTimeMillis) {
            return Code.FAIL;
        }
        // 续费时长
        CommonResult<R> result = roomDao.doSave(room, new DataSaveCallback<>() {
            @Override
            public void updateData(R dataEntity) {
            }

            @Override
            public boolean updateDataWithRes(FriendRoom dataEntity) {
                dataEntity.setOverdueTime(currentTimeMillis + durationTime);
                dataEntity.setPredictCostGoldNum(dataEntity.getPredictCostGoldNum() - itemNum);
                dataEntity.setSendPauseRenewalMail(false);
                return true;
            }
        });
        if (!result.success()) {
            return Code.FAIL;
        }
        this.room = result.data;
        Map<Integer, Long> itemMap = Map.of(requiredMoney.getFirst(), (long) itemNum);
        ItemOperationResult itemOperationResult = new ItemOperationResult();
        itemOperationResult.setDiamond(this.room.getPredictCostGoldNum());
        roomManager.getCoreLogger().roomOperate(this.room, 2, roomExpendCfg.getDurationTime(), itemMap, itemOperationResult);
        log.info("房间：{} 自动续费成功, 过期时间：{} 总花费：{}", room.logStr(), room.getOverdueTime(), itemNum);
        loadRenewalTimer();
        return Code.SUCCESS;
    }

    /**
     * 房间自动续费时获取续费配置
     */
    public RoomExpendCfg getAutoRenewalCfg() {
        for (RoomExpendCfg roomExpendCfg : GameDataManager.getRoomExpendCfgList()) {
            if (roomExpendCfg.getDurationtype() == 1) {
                return roomExpendCfg;
            }
        }
        return null;
    }

    /**
     * 检查庄家是否能满足开始条件
     */
    protected abstract boolean checkBankerCanNextRound();

    @Override
    public void onRoomCantContinue() {
        super.onRoomCantContinue();
        // 当房间处于销毁状态时，需要清理掉所有房间内处于断线状态的玩家
        if (room.getStatus() == 3) {
            for (Map.Entry<Long, RoomPlayer> entry : room.getRoomPlayers().entrySet()) {
                // 如果玩家不在线，房间又处于销毁状态，直接清理，如果在线等房间踢人
                if (!entry.getValue().isOnline()) {
                    // 踢出房间
                    roomManager.exitRoom(entry.getKey());
                }
            }
        }
    }

    /**
     * 是否能上庄
     *
     * @return true是
     */
    public boolean canBeBanker() {
        return true;
    }

    /**
     * 申请成为庄家
     */
    public int supplyBeBanker(long playerId, long predictCostGold) {
        if (!canBeBanker()) {
            return Code.FORBID;
        }
        if (room.roomBankerId() == playerId) {
            // 重复上庄
            return Code.REPEAT_OP;
        }
        if (roomCfg.getBankerBets() == 0) {
            // 房主不能成为庄家
            long roomCreator = room.getCreator();
            if (roomCreator == playerId) {
                return Code.ROOM_CREATOR_CANT_BE_BANKER;
            }
        }
        int addRes = addBankerPredicateGold(playerId, predictCostGold);
        // 如果申请成功，且当前游戏处于暂停状态，需要继续游戏
        if (addRes == Code.SUCCESS) {
            GamePlayer gamePlayer = gameController.getGamePlayer(playerId);
            if (gamePlayer != null) {
                bankerPredicateInfo.put(playerId, gamePlayer);
                log.info("玩家申请上庄：{} 放入缓存列表：{}", playerId, predictCostGold);
            }
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
        notify.bankerPredicateCostGold = room.roomBankerResetGold();
        notify.roomCreatorPredicateCostGold = room.getPredictCostGoldNum();
        // 游戏交易道具ID
        notify.gameTransactionItemId = gameController.getGameTransactionItemId();
        log.debug("广播变化数据: {}", JSON.toJSONString(notify));
        broadcastToPlayers(RoomMessageBuilder.newBuilder().setData(notify).toAllPlayer());
    }

    /**
     * 标记玩家下庄，具体操作需要在进入下一轮开始之前
     */
    public int markBankerCancel(long playerId) {
        // 如果当前玩家是庄家直接标记
        if (room.roomBankerId() == playerId && isStartedGame()) {
            gameController.getGameDataVo().setApplyCancelBeBankerPlayer(playerId);
            return Code.SUCCESS;
        }
        // 如果当前玩家不是庄家，直接从申请列表中删除，然后再返还金币
        long playerGold = room.getApplyBankerPlayerGold(playerId);
        if (playerGold <= 0) {
            return Code.SUCCESS;
        }
        CommonResult<R> result = roomDao.doSave(room.getGameType(), room.getId(),
                new DataSaveCallback<>() {
                    @Override
                    public void updateData(R dataEntity) {
                    }

                    @Override
                    public boolean updateDataWithRes(FriendRoom dataEntity) {
                        dataEntity.cancelApplyBanker(playerId);
                        return true;
                    }
                });
        if (result.success()) {
            //添加道具
            int code = gameController.addItem(playerId, playerGold, AddType.FRIEND_ROOM_CANCEL_BANKER_ADD_COIN);
            if (code != Code.SUCCESS) {
                log.error("玩家：{} 申请取消成为庄家 添加道具失败 itemId:{} num:{}", playerId, gameController.getGameTransactionItemId(), playerGold);
            }
            log.info("玩家：{} 申请取消成为庄家", playerId);
            this.room = result.data;
        }
        bankerPredicateInfo.remove(playerId);
        log.info("玩家：{} 申请取消成为庄家", playerId);
        return Code.SUCCESS;
    }

    /**
     * 取消成为庄家
     *
     * @param playerId 玩家ID
     * @param backNum  返回版主金的金额
     * @return 取消结果
     */
    public int cancelBeBanker(long playerId, AtomicLong backNum) {
        // 如果当前玩家不为庄家
        if (room.roomBankerId() != playerId) {
            return Code.PARAM_ERROR;
        }
        // 查询玩家当前的金币
        long bankerResetGold = room.roomBankerResetGold();
        // 将玩家移除
        CommonResult<R> result = roomDao.doSave(room.getGameType(), room.getId(),
                new DataSaveCallback<>() {
                    @Override
                    public void updateData(R dataEntity) {
                    }

                    @Override
                    public boolean updateDataWithRes(FriendRoom dataEntity) {
                        Map.Entry<Long, Long> removedBanker = dataEntity.removeBanker();
                        if (backNum != null && removedBanker != null) {
                            backNum.set(removedBanker.getValue());
                        }
                        return removedBanker != null;
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
        log.info("玩家：{} 下庄成功, 准备金：{}", playerId, bankerResetGold);
        // 取消上庄后，需要重置上庄次数
        gameController.getGameDataVo().setBeBankerTimes(0);
        return Code.SUCCESS;
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
            if (gamePlayer == null) {
                gamePlayer = bankerPredicateInfo.get(entry.getKey());
            }
            if (gamePlayer == null) {
                log.error("未获取到庄家信息 playerId:{}", entry.getKey());
                continue;
            }
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
        res.bankerResetGold = gameController.getTransactionItemNum(gamePlayer.getId());
        playerController.send(res);
        return resCode;
    }

    /**
     * 添加准备金
     */
    private int addBankerPredicateGold(long playerId, long predictCostGold) {
        //获取玩家身上的货币数据
        long transactionItemNum = gameController.getTransactionItemNum(playerId);
        if (transactionItemNum < predictCostGold) {
            return Code.NOT_ENOUGH;
        }
        RoomCfg roomCfg = getGameController().getGameDataVo().getRoomCfg();
        if (roomCfg.getMinBankerAmount() != null && roomCfg.getMinBankerAmount().size() > 1) {
            int minBankerAmount = roomCfg.getMinBankerAmount().get(1);
            // 请求的预付金币小于最低可以配置的金币
            if (predictCostGold < minBankerAmount) {
                return Code.AMOUNT_OF_RESERVES_IS_INCORRECT_CONFIG;
            }
            //扣钱
            int code = gameController.deductItem(playerId, predictCostGold, AddType.FRIEND_ROOM_APPLY_BANKER_DEDUCT_PREDICATE);
            if (code != Code.SUCCESS) {
                return code;
            }
            log.info("玩家上庄：{} 扣除玩家金额：{}", playerId, predictCostGold);
            // 保存房间数据
            CommonResult<R> result = roomDao.doSave(room.getGameType(), room.getId(),
                    new DataSaveCallback<>() {
                        @Override
                        public void updateData(R dataEntity) {
                        }

                        @Override
                        public boolean updateDataWithRes(FriendRoom dataEntity) {
                            dataEntity.addBankerSupply(playerId, predictCostGold);
                            return true;
                        }
                    });
            if (result.code != Code.SUCCESS) {
                return result.code;
            }
            this.room = result.data;
        }
        log.info("玩家上庄：{} 添加准备金：{}", playerId, predictCostGold);
        return Code.SUCCESS;
    }

    public void deductBankerGold(long bankerFlowing) {
        CommonResult<R> result = roomDao.doSave(room, new DataSaveCallback<>() {
            @Override
            public void updateData(R dataEntity) {

            }

            @Override
            public boolean updateDataWithRes(FriendRoom dataEntity) {
                dataEntity.deductBankerPredicateItem(Math.abs(bankerFlowing));
                return true;
            }
        });
        if (result.success()) {
            this.room = result.data;
        }
    }
}
