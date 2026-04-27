package com.jjg.game.poker.game.common;

import com.jjg.game.common.concurrent.BaseHandler;
import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.common.data.DataSaveCallback;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.common.utils.WheelTimerUtil;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.data.*;
import com.jjg.game.room.friendroom.AbstractFriendRoomController;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.RoomExpendCfg;
import com.jjg.game.sampledata.bean.Room_ChessCfg;
import com.jjg.game.sampledata.bean.WarehouseCfg;
import io.netty.util.Timeout;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 扑克好友房，房间控制器
 *
 * @author 2CL
 */
public class PokerFriendRoomController extends AbstractFriendRoomController<Room_ChessCfg, PokerFriendRoom> {
    private volatile Timeout autoRenewalTimeout;

    public PokerFriendRoomController(Class<? extends RoomPlayer> roomPlayerClazz, PokerFriendRoom room) {
        super(roomPlayerClazz, room);
    }


    @Override
    public void cancelWheelTimers() {
        super.cancelWheelTimers();
        Timeout tickTimeout = autoRenewalTimeout;
        if (tickTimeout != null && !tickTimeout.isCancelled()) {
            tickTimeout.cancel();
        }
        autoRenewalTimeout = null;
    }

    @Override
    public <G extends Room> void initial(G room) {
        super.initial(room);
        autoRenewalTimeout = WheelTimerUtil.scheduleAtFixedRate(this::autoRenewalCheck, TimeHelper.ONE_MINUTE_OF_MILLIS,
                TimeHelper.ONE_MINUTE_OF_MILLIS, TimeUnit.MILLISECONDS);
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
        long curTime = System.currentTimeMillis();
        long now = System.currentTimeMillis();
        //提前续费
        long diff = room.getOverdueTime() - now;
        if (diff > TimeUnit.MINUTES.toMillis(2)) {
            return;
        }
        // 如果时间到期且没有开启自动续费，先暂停游戏
        if (!room.isAutoRenewal() || roomManager.getNodeManager().nodeConfig.waitClose()) {
            log.info("房间：{} 时长到期", room.logStr());
            return;
        }
        if (room.isSendPauseRenewalMail()) {
            return;
        }
        if (room.getRoomPlayers().isEmpty()) {
            List<LanguageParamData> params = new ArrayList<>();
            WarehouseCfg warehouseCfg = GameDataManager.getWarehouseCfg(room.getRoomCfgId());
            params.add(new LanguageParamData(1, warehouseCfg.getNameid() + ""));
            params.add(new LanguageParamData(TimeHelper.getDate(System.currentTimeMillis())));
            roomManager.getMailService().addCfgMail(room.getCreator(), 2, null, params, AddType.FRIEND_ROOM_NO_PLAYER_PAUSE_RENEWAL);
            log.info("房间：{} 时长到期,没有玩家暂停续费", room.logStr());
            updateRenewalMailStatus(true);
            return;
        }
        // 自动续费，检查玩家金币是否足够
        RoomExpendCfg roomExpendCfg = getAutoRenewalCfg();
        if (roomExpendCfg == null) {
            return;
        }
        List<Integer> requiredMoney = roomExpendCfg.getRequiredMoney();
        int itemNum = requiredMoney.get(1);
        // 时长，毫秒
        long durationTime = (long) roomExpendCfg.getDurationTime() * TimeHelper.ONE_MINUTE_OF_MILLIS;
        // 从房间底庄中扣除金币，如果不足直接暂停游戏
        if (itemNum > room.getPredictCostGoldNum()) {
            // 自动续费失败，房间准备金不足
            log.info("自动续费失败，房间准备金不足: need: {} rest: {}", itemNum, room.getPredictCostGoldNum());
            List<LanguageParamData> params = new ArrayList<>();
            WarehouseCfg warehouseCfg = GameDataManager.getWarehouseCfg(room.getRoomCfgId());
            params.add(new LanguageParamData(1, warehouseCfg.getNameid() + ""));
            params.add(new LanguageParamData(TimeHelper.getDate(System.currentTimeMillis())));
            roomManager.getMailService().addCfgMail(room.getCreator(), 3, null, params, AddType.FRIEND_ROOM_RENEWAL_FAIL);
            updateRenewalMailStatus(true);
            return;
        }
        long overdueTime = room.getOverdueTime();
        long totalTake = 0;
        while (itemNum < room.getPredictCostGoldNum()) {
            room.setPredictCostGoldNum(room.getPredictCostGoldNum() - itemNum);
            totalTake += itemNum;
            overdueTime += durationTime;
            if (overdueTime > curTime) {
                break;
            }
        }
        // 续费时长
        long finalOverdueTime = overdueTime;
        CommonResult<PokerFriendRoom> result = roomDao.doSave(room, new DataSaveCallback<>() {
            @Override
            public void updateData(PokerFriendRoom dataEntity) {
            }

            @Override
            public boolean updateDataWithRes(PokerFriendRoom dataEntity) {
                dataEntity.setSendPauseRenewalMail(false);
                dataEntity.setOverdueTime(finalOverdueTime);
                // TODO日志
                dataEntity.setPredictCostGoldNum(dataEntity.getPredictCostGoldNum());
                return true;
            }
        });
        if (result.success()) {
            this.room = result.data;
            Map<Integer, Long> itemMap = Map.of(requiredMoney.getFirst(), (long) itemNum);
            ItemOperationResult itemOperationResult = new ItemOperationResult();
            itemOperationResult.setDiamond(this.room.getPredictCostGoldNum());
            roomManager.getCoreLogger().roomOperate(this.room, 2, roomExpendCfg.getDurationTime(), itemMap, itemOperationResult);
            log.info("房间：{} 自动续费成功, 过期时间：{} 总花费：{}", room.logStr(), overdueTime, totalTake);
            updateRenewalMailStatus(false);
        }
    }

    private void updateRenewalMailStatus(boolean state) {
        room.setSendPauseRenewalMail(true);
        roomDao.doSave(room, new DataSaveCallback<>() {
            @Override
            public void updateData(PokerFriendRoom dataEntity) {
            }

            @Override
            public boolean updateDataWithRes(PokerFriendRoom dataEntity) {
                dataEntity.setSendPauseRenewalMail(state);
                return true;
            }
        });
    }

    @Override
    protected boolean checkBankerCanNextRound() {
        return switch (room.getGameType()) {
            // 德州、南方前进为系统庄家，不限制庄家，也没有上庄
            case CoreConst.GameType.TEXAS, CoreConst.GameType.TO_SOUTH -> true;
            // TODO 暂定，后续确认需不需要庄家后再确定
            case CoreConst.GameType.VEGAS_THREE, CoreConst.GameType.BLACK_JACK -> false;
            default -> false;
        };
    }

    @Override
    public void reloadRoomCfg() {
        // 重载配置表引用
        roomCfg = GameDataManager.getRoom_ChessCfg(room.getRoomCfgId());
        gameController.getGameDataVo().reloadRoomCfg();
    }

    @Override
    public boolean canBeBanker() {
        return room.getGameType() != CoreConst.GameType.TEXAS
                && room.getGameType() != CoreConst.GameType.TO_SOUTH;
    }
}
