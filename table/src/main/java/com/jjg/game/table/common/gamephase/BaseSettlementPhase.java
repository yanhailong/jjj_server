package com.jjg.game.table.common.gamephase;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSON;
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
import com.jjg.game.room.base.AbstractRoomPhase;
import com.jjg.game.room.constant.EGamePhase;
import com.jjg.game.room.controller.AbstractPhaseGameController;
import com.jjg.game.room.data.robot.GameRobotPlayer;
import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.room.data.room.RoomBankerChangeParam;
import com.jjg.game.room.data.room.SettlementData;
import com.jjg.game.room.datatrack.DataTrackNameConstant;
import com.jjg.game.sampledata.bean.Room_BetCfg;
import com.jjg.game.sampledata.bean.WinPosWeightCfg;
import com.jjg.game.table.common.dao.BetTableFriendRoomDao;
import com.jjg.game.table.common.dao.TableRoomDao;
import com.jjg.game.table.common.data.TableGameDataVo;
import com.jjg.game.table.common.message.bean.BetTableInfo;
import com.jjg.game.table.common.utils.BetDataTrackLogUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 结算阶段
 *
 * @author 2CL
 */
public abstract class BaseSettlementPhase<D extends TableGameDataVo> extends AbstractRoomPhase<Room_BetCfg, D> {

    public BaseSettlementPhase(AbstractPhaseGameController<Room_BetCfg, D> gameController) {
        super(gameController);
    }

    @Override
    public int getPhaseRunTime() {
        List<Integer> stageTime = gameDataVo.getRoomCfg().getStageTime();
        if (stageTime.size() >= 4) {
            return stageTime.get(2) + stageTime.get(3);
        }
        return 0;
    }


    @Override
    public void phaseDoAction() {
        super.phaseDoAction();
    }

    @Override
    public EGamePhase getGamePhase() {
        return EGamePhase.GAME_ROUND_OVER_SETTLEMENT;
    }

    @Override
    protected void hostingPlayerActionOnPhaseStart(GamePlayer gamePlayer) {

    }

    @Override
    protected void robotActionOnPhaseStart(GameRobotPlayer gamePlayer) {

    }

    @Override
    public void onPlayerHalfwayExitPhase(GamePlayer gamePlayer) {

    }

    @Override
    public void onPlayerHalfwayJoinPhase(GamePlayer gamePlayer) {

    }

    /**
     * 计算金币数量, 需要减去押注的钱
     */
    protected SettlementData calcGold(GamePlayer gamePlayer, WinPosWeightCfg weightCfg, long betValue) {
        return calcGold(gamePlayer, weightCfg.getOdds(), weightCfg.getReturnRate(), weightCfg, betValue);
    }

    /**
     * 计算金币数量, 需要减去押注的钱
     */
    protected SettlementData calcGold(GamePlayer gamePlayer, int odds, int returnRate, WinPosWeightCfg weightCfg, long betValue) {
        int winRatio = gameDataVo.getRoomCfg().getWinRatio();
        // 总赢钱
        BigDecimal totalGet = BigDecimal.valueOf(betValue)
                .multiply(BigDecimal.valueOf(odds))
                .divide(BigDecimal.valueOf(100), 4, RoundingMode.DOWN);

        BigDecimal multiAdd = totalGet.multiply(BigDecimal.valueOf((10000 - (weightCfg.getIsRatio() == 1 ? winRatio : 0))))
                .divide(BigDecimal.valueOf(10000), 4, RoundingMode.DOWN);

        // 赢的总值
        long totalWin = multiAdd.longValue() + BigDecimal.valueOf(betValue)
                .multiply(BigDecimal.valueOf(returnRate))
                .divide(BigDecimal.valueOf(10000), 4, RoundingMode.DOWN)
                .longValue();
        if (gamePlayer != null && !(gamePlayer instanceof GameRobotPlayer)) {
            log.info("玩家：{} {} 在压分区域：{}，押注：{}，获得： 赢 {} + 抽水返还 {}, 总值：{}",
                    gamePlayer.getId(),
                    gameDataVo.roomLogInfo(),
                    weightCfg.getId(),
                    betValue,
                    totalGet,
                    multiAdd,
                    totalWin);
        }
        // 倍率计算 + 压分返还 + 赢的总值
        return new SettlementData(multiAdd.longValue(), totalWin, betValue, totalGet.longValue() - multiAdd.longValue());
    }

    /**
     * 添加玩家区域下注日志
     *
     * @param gamePlayer 玩家数据
     */
    public void addPlayerAreaDataLog(GamePlayer gamePlayer) {
        Map<Integer, List<Integer>> playerBetInfoMap = gameDataVo.getPlayerBetInfo().get(gamePlayer.getId());
        if (playerBetInfoMap == null) {
            return;
        }
        List<BetTableInfo> betTableInfos = new ArrayList<>(playerBetInfoMap.size());
        for (Map.Entry<Integer, List<Integer>> entry : playerBetInfoMap.entrySet()) {
            long areaTotal = 0;
            for (Integer betValue : entry.getValue()) {
                areaTotal += betValue;
            }
            BetTableInfo betTableInfo = new BetTableInfo();
            betTableInfo.betIdx = entry.getKey();
            betTableInfo.playerBetTotal = areaTotal;
            betTableInfos.add(betTableInfo);
        }
        gameDataTracker.addPlayerLogData(gamePlayer, DataTrackNameConstant.AREA_DATA, JSON.toJSONString(betTableInfos));
    }

    /**
     * 获取RoomBankerChangeParam
     *
     * @param betInfo 下注信息
     * @return RoomBankerChangeParam
     */
    public RoomBankerChangeParam getRoomBankerChangeParam(Map<Integer, Map<Long, Long>> betInfo) {
        RoomBankerChangeParam param = new RoomBankerChangeParam();
        if (CollectionUtil.isNotEmpty(betInfo)) {
            param.initData(betInfo);
            return param;
        }
        return param;
    }

    /**
     * 计算房主应得的收益
     */
    protected long calcRoomCreatorIncome(long totalTaxRevenue) {
        RoomType roomType = RoomType.getRoomType(gameDataVo.getRoomCfg().getId());
        long bankerIncome = 0;
        // 如果是好友房需要扣除一部分金币给房主
        if (roomType == RoomType.POKER_TEAM_UP_ROOM || roomType == RoomType.BET_TEAM_UP_ROOM) {
            // 庄家扣税比例
            int bankerIncomeRatio = SampleDataUtils.getIntGlobalData(GlobalSampleConstantId.CREATE_ROOM_FUNC_INCOME_RATIO);
            bankerIncome = totalTaxRevenue * bankerIncomeRatio / 10000;
            log.info("房主：{} 收益：{}", gameController.getRoom().getCreator(), bankerIncome);
        }
        return bankerIncome;
    }

    /**
     * 计算最后的BankerChange
     */
    public void calculationFinalBankerChange(RoomBankerChangeParam param) {
        long totalGet = 0;
        for (Map.Entry<Integer, Map<Long, Long>> entry : param.getBankerChangeMap().entrySet()) {
            long sum = entry.getValue().values().stream().mapToLong(Long::longValue).sum();
            long realGet = sum * gameDataVo.getRoomCfg().getEffectiveRatio() / 10000;
            param.addTotalTaxRevenue(sum - realGet);
            totalGet += realGet;
        }
        param.addBankerChangeGold(-totalGet);
        //计算房主收益
        param.addRoomCreatorTotalIncome(calcRoomCreatorIncome(param.getTotalTaxRevenue()));
        gameDataTracker.addGameLogData("tax", param.getTotalTaxRevenue());
    }


    @Override
    public void phaseFinish() {
        //结算完成直接清除数据
        gameDataVo.clearRoundData(gameController);
        // 检查机器人的退出概率
        try {
            phaseFinishAction();
        } catch (Exception e) {
            log.error("结算完成操作异常", e);
        }
    }

    public void phaseFinishAction() {

    }

    public void dealRoomPool(RoomBankerChangeParam changeParam) {
        if (!changeParam.isInit() || changeParam.getBankerChangeGold() == 0) {
            return;
        }
        if (gameController.getRoom() instanceof FriendRoom room) {
            Map.Entry<Long, Long> longLongEntry = room.getBankerPredicateMap().firstEntry();
            if (longLongEntry != null) {
                return;
            }
        }
        gameController.getRoomController().getRoomProcessor()
                .tryPublish(0, new BaseHandler<String>() {
                    @Override
                    public void action() {
                        AbstractRoomDao<? extends Room, ? extends RoomPlayer> roomDao = gameController.getRoomController().getRoomDao();
                        if (roomDao instanceof TableRoomDao tableRoomDao) {
                            Room_BetCfg roomCfg = gameDataVo.getRoomCfg();
                            long roomPool = tableRoomDao.modifyRoomPool(roomCfg.getGameID(), roomCfg.getId(), -changeParam.getBankerChangeGold());
                            log.info("gameType:{} roomCfgId:{} 奖池回收触发 变化值:{} 变化后{}  ", roomCfg.getGameID(), roomCfg.getId(), -changeParam.getBankerChangeGold(), roomPool);
                        } else if (roomDao instanceof BetTableFriendRoomDao tableRoomDao) {
                            Room_BetCfg roomCfg = gameDataVo.getRoomCfg();
                            long roomPool = tableRoomDao.modifyRoomPool(roomCfg.getGameID(), gameController.getRoom().getId(), -changeParam.getBankerChangeGold());
                            log.info("好友房 gameType:{} roomCfgId:{} 奖池回收触发 变化值:{} 变化后{}  ", roomCfg.getGameID(), roomCfg.getId(), -changeParam.getBankerChangeGold(), roomPool);
                        }
                    }
                }.setHandlerParamWithSelf("dealRoomPool"));
    }

    /**
     * 是否能触发回收
     *
     * @return 当前池的数量
     */
    public Pair<Long, Long> canTriggerRecycling() {
        //判断是否有真人
        Map<Long, Map<Integer, List<Integer>>> realPlayerBetInfo = gameDataVo.getRealPlayerBetInfo();
        if (CollectionUtil.isEmpty(realPlayerBetInfo)) {
            return null;
        }
        AbstractRoomDao<? extends Room, ? extends RoomPlayer> roomDao = gameController.getRoomController().getRoomDao();
        Room_BetCfg roomCfg = gameDataVo.getRoomCfg();
        Long roomPool = null;
        Long basePool = null;
        if (roomDao instanceof TableRoomDao tableRoomDao) {
            roomPool = tableRoomDao.getRoomPool(roomCfg.getGameID(), roomCfg.getId(), roomCfg.getInitBasePool());
            basePool = roomCfg.getInitBasePool();
            if (roomPool > roomCfg.getInitBasePool()) {
                return null;
            }
        } else if (roomDao instanceof BetTableFriendRoomDao tableRoomDao) {
            if (gameController.getRoom() instanceof FriendRoom friendRoom) {
                Map.Entry<Long, Long> longLongEntry = friendRoom.getBankerPredicateMap().firstEntry();
                if (longLongEntry == null) {
                    roomPool = tableRoomDao.getRoomPool(roomCfg.getGameID(), gameController.getRoom().getId());
                    basePool = friendRoom.getPool();
                }
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

    /**
     * 通用计算下注输赢
     *
     * @param realPlayerBetInfo   真人玩家下注
     * @param winPosWeightCfgList 配置信息
     * @return 赢的值->输的值
     */
    public Pair<Long, Long> getWinOrLoseResult(Map<Long, Map<Integer, List<Integer>>> realPlayerBetInfo, List<WinPosWeightCfg> winPosWeightCfgList) {
        long totalWin = 0;
        long totalLose = 0;
        for (Map.Entry<Long, Map<Integer, List<Integer>>> entry : realPlayerBetInfo.entrySet()) {
            SettlementData playerSettlementData = new SettlementData();
            Map<Integer, List<Integer>> playerBetInfo = entry.getValue();
            for (WinPosWeightCfg winPosWeightCfg : winPosWeightCfgList) {
                List<Integer> betAreas = winPosWeightCfg.getBetArea();
                for (Integer betAreaIdx : betAreas) {
                    if (playerBetInfo.containsKey(betAreaIdx)) {
                        List<Integer> playerBetGoldList = playerBetInfo.get(betAreaIdx);
                        // 玩家总押注
                        long playerBetGoldTotal = playerBetGoldList.stream().mapToInt(Integer::intValue).sum();
                        SettlementData settlementData = calcGold(null, winPosWeightCfg, playerBetGoldTotal);
                        playerSettlementData.increaseBySettlementData(settlementData);
                    }
                }
            }
            playerSettlementData.setBetTotal(playerBetInfo.values().stream()
                    .mapToLong(a -> a.stream().mapToInt(b -> b).sum())
                    .sum());
            totalLose += playerSettlementData.getTotalWin() + playerSettlementData.getTaxation();
            BigDecimal totalGet = BigDecimal.valueOf(playerSettlementData.getBetTotal() - playerSettlementData.getBankerWind())
                    .multiply(BigDecimal.valueOf((10000 - gameDataVo.getRoomCfg().getWinRatio())))
                    .divide(BigDecimal.valueOf(10000), 4, RoundingMode.DOWN);
            totalWin += playerSettlementData.getBankerWind() + totalGet.longValue();
        }
        return Pair.newPair(totalWin, totalLose);
    }

    /**
     * 计算结算金币
     */
    public SettlementData calcSettlementGold(GamePlayer gamePlayer, List<WinPosWeightCfg> winPosWeightCfgs,
                                             Map<Integer, List<Integer>> playerBetInfo, RoomBankerChangeParam changeParam) {
        SettlementData playerSettlementData = new SettlementData();
        for (WinPosWeightCfg winPosWeightCfg : winPosWeightCfgs) {
            List<Integer> betAreas = winPosWeightCfg.getBetArea();
            for (Integer betAreaIdx : betAreas) {
                if (playerBetInfo.containsKey(betAreaIdx)) {
                    if (changeParam != null) {
                        changeParam.removeArea(betAreaIdx);
                    }
                    List<Integer> playerBetGoldList = playerBetInfo.get(betAreaIdx);
                    // 玩家总押注
                    long playerBetGoldTotal = playerBetGoldList.stream().mapToInt(Integer::intValue).sum();
                    SettlementData settlementData = calcGold(gamePlayer, winPosWeightCfg, playerBetGoldTotal);
                    playerSettlementData.increaseBySettlementData(settlementData);
                }
            }
        }
        // 记录日志
        BetDataTrackLogUtils.recordBetLog(playerSettlementData, gamePlayer, gameController, playerBetInfo);
        return playerSettlementData;
    }
}
