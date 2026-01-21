package com.jjg.game.poker.game.common.gamephase;

import com.jjg.game.common.concurrent.BaseHandler;
import com.jjg.game.common.proto.Pair;
import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.core.constant.GlobalSampleConstantId;
import com.jjg.game.core.dao.room.AbstractRoomDao;
import com.jjg.game.core.data.FriendRoom;
import com.jjg.game.core.data.Room;
import com.jjg.game.core.data.RoomPlayer;
import com.jjg.game.core.data.RoomType;
import com.jjg.game.core.utils.SampleDataUtils;
import com.jjg.game.poker.game.common.BasePokerGameController;
import com.jjg.game.poker.game.common.BasePokerGameDataVo;
import com.jjg.game.poker.game.common.constant.PokerPhase;
import com.jjg.game.poker.game.common.dao.PokerRoomDao;
import com.jjg.game.poker.game.common.data.PokerDataHelper;
import com.jjg.game.room.constant.EGamePhase;
import com.jjg.game.room.controller.AbstractPhaseGameController;
import com.jjg.game.sampledata.bean.Room_ChessCfg;

import java.math.BigDecimal;
import java.math.RoundingMode;


/**
 * 通用扑克游戏结算阶段
 *
 * @author lm
 * @date 2025/7/26 11:06
 */
public abstract class BaseSettlementPhase<T extends BasePokerGameDataVo> extends BasePokerPhase<T> {
    public BaseSettlementPhase(AbstractPhaseGameController<Room_ChessCfg, T> gameController) {
        super(gameController);
    }


    @Override
    public int getPhaseRunTime() {
        return PokerDataHelper.getExecutionTime(gameDataVo, PokerPhase.SETTLEMENT);
    }

    @Override
    public void phaseFinish() {
        if (gameController instanceof BasePokerGameController<T> controller) {
            try {
                phaseFinishDoAction();
            } catch (Exception e) {
                log.error("结算结算处理异常", e);
            }
            controller.setCurrentGamePhase(new BaseWaitReadyPhase<>(gameController));
            gameDataVo.resetData(controller);
            //开启下一局
            controller.tryStartNextGame();
        }
    }

    /**
     * 计算房主应得的收益
     */
    protected long calcRoomCreatorIncome(long betValue) {
        RoomType roomType = RoomType.getRoomType(gameDataVo.getRoomCfg().getId());
        long bankerIncome = 0;
        // 如果是好友房需要扣除一部分金币给房主
        if (roomType == RoomType.POKER_TEAM_UP_ROOM || roomType == RoomType.BET_TEAM_UP_ROOM) {
            // 庄家扣税比例
            int bankerIncomeRatio =
                    SampleDataUtils.getIntGlobalData(GlobalSampleConstantId.CREATE_ROOM_FUNC_INCOME_RATIO);
            bankerIncome =
                    (long) Math.floor((betValue) * bankerIncomeRatio / 10000.0);
        }
        return bankerIncome;
    }

    public void phaseFinishDoAction() {
    }

    @Override
    public EGamePhase getGamePhase() {
        return EGamePhase.GAME_ROUND_OVER_SETTLEMENT;
    }

    /**
     * 是否能触发回收
     *
     * @return 当前池的数量
     */
    public Pair<Long, Long> canTriggerRecycling() {
        //判断是否有真人
        int realPlayerBetCount = gameDataVo.getRealPlayerBetCount();
        if (realPlayerBetCount == 0) {
            return null;
        }
        AbstractRoomDao<? extends Room, ? extends RoomPlayer> roomDao = gameController.getRoomController().getRoomDao();
        Room_ChessCfg roomCfg = gameDataVo.getRoomCfg();
        Long roomPool = null;
        Long basePool = null;
        if (roomDao instanceof PokerRoomDao pokerRoomDao) {
            roomPool = pokerRoomDao.getRoomPool(roomCfg.getGameID(), roomCfg.getId(), roomCfg.getInitBasePool());
            basePool = roomCfg.getInitBasePool();
            if (roomPool > roomCfg.getInitBasePool()) {
                return null;
            }
        }
        if (roomPool == null) {
            return null;
        }
        int pro = BigDecimal.valueOf(10000)
                .subtract(BigDecimal.valueOf(10000)
                        .multiply(BigDecimal.valueOf(Math.max(roomPool, 1)))
                        .divide(BigDecimal.valueOf(Math.max(basePool, 1)), 0, RoundingMode.DOWN))
                .intValue();
        if (RandomUtils.getRandomNumInt10000() <= pro) {
            return Pair.newPair(roomPool, basePool);
        }
        return null;
    }

    public void dealRoomPool(long poolWinValue, long poolLoseValue) {
        if (poolWinValue == 0 && poolLoseValue == 0) {
            return;
        }
        //计算赢钱税后
        Room_ChessCfg roomCfg = gameDataVo.getRoomCfg();
        long changeValue = BigDecimal.valueOf(poolWinValue)
                .multiply(BigDecimal.valueOf(roomCfg.getEffectiveRatio()))
                .divide(BigDecimal.valueOf(10000), RoundingMode.DOWN)
                .subtract(BigDecimal.valueOf(poolLoseValue))
                .longValue();

        if (gameController.getRoom() instanceof FriendRoom || changeValue == 0) {
            return;
        }
        gameController.getRoomController().getRoomProcessor()
                .tryPublish(0, new BaseHandler<String>() {
                    @Override
                    public void action() {
                        AbstractRoomDao<? extends Room, ? extends RoomPlayer> roomDao = gameController.getRoomController().getRoomDao();
                        if (roomDao instanceof PokerRoomDao pokerRoomDao) {
                            Room_ChessCfg roomCfg = gameDataVo.getRoomCfg();
                            long roomPool = pokerRoomDao.modifyRoomPool(roomCfg.getGameID(), roomCfg.getId(), changeValue);
                            log.info("gameType:{} roomCfgId:{} 奖池回收触发 变化值:{} 变化后{}  ", roomCfg.getGameID(), roomCfg.getId(), changeValue, roomPool);
                        }
                    }
                }.setHandlerParamWithSelf("dealRoomPool"));
    }
}
