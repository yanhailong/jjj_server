package com.jjg.game.table.common.gamephase;

import com.alibaba.fastjson.JSON;
import com.jjg.game.common.timer.TimerEvent;
import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.exception.GameSampleException;
import com.jjg.game.room.base.AbstractMsgDealRoomPhase;
import com.jjg.game.room.constant.EGamePhase;
import com.jjg.game.room.controller.AbstractGameController;
import com.jjg.game.room.controller.AbstractPhaseGameController;
import com.jjg.game.room.data.robot.GameRobotPlayer;
import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.room.sample.bean.BetRobotCfg;
import com.jjg.game.room.sample.bean.RobotCfg;
import com.jjg.game.room.sample.bean.Room_BetCfg;
import com.jjg.game.room.timer.RoomEventType;
import com.jjg.game.table.betsample.sample.GameDataManager;
import com.jjg.game.table.betsample.sample.bean.BetAreaCfg;
import com.jjg.game.table.common.data.TableGameDataVo;
import com.jjg.game.table.common.data.TableSampleDataHolder;
import com.jjg.game.table.common.message.TableMessageBuilder;
import com.jjg.game.table.common.message.TableRoomMessageConstant;
import com.jjg.game.table.common.message.bean.BetTableInfo;
import com.jjg.game.table.common.message.bean.ReqBetBean;
import com.jjg.game.table.common.message.req.ReqBet;
import com.jjg.game.table.common.message.res.NotifyPlayerBet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 押注阶段,通用押注逻辑
 *
 * @author 2CL
 */
public abstract class BaseTableBetPhase<D extends TableGameDataVo> extends
    AbstractMsgDealRoomPhase<Room_BetCfg, D, ReqBet> {

    private static final Logger log = LoggerFactory.getLogger(BaseTableBetPhase.class);

    public BaseTableBetPhase(AbstractPhaseGameController<Room_BetCfg, D> gameController) {
        super(gameController);
    }

    @Override
    public int getPhaseRunTime() {
        List<Integer> stageTime = gameDataVo.getRoomCfg().getStageTime();
        if (stageTime.size() >= 2) {
            return stageTime.get(1);
        }
        return 0;
    }

    @Override
    public EGamePhase getGamePhase() {
        return EGamePhase.BET;
    }

    @Override
    public void phaseDoAction() {
        super.phaseDoAction();
        // 清除房间的数据
        clearRoomData();
        broadcastMsgToRoom(
            TableMessageBuilder.getNotifyPhaseChangInfo(getGamePhase(), gameDataVo.getPhaseEndTime()));
    }

    @Override
    public void phaseFinish() {
    }

    @Override
    public void dealMsg(PlayerController playerController, ReqBet reqBet) {
        List<ReqBetBean> reqBetBeans = reqBet.reqBetBeans;
        NotifyPlayerBet notifyPlayerBet = new NotifyPlayerBet(Code.SUCCESS);
        notifyPlayerBet.playerId = playerController.playerId();
        if (reqBetBeans == null || reqBetBeans.isEmpty()) {
            notifyPlayerBet.code = Code.FAIL;
            playerController.send(notifyPlayerBet);
            return;
        }
        // 判断合法性
        GamePlayer gamePlayer = gameDataVo.getGamePlayer(playerController.playerId());
        // 检查是否是合法押注
        int checkRes = checkBetAction(gamePlayer, reqBetBeans);
        log.info("玩家：{} 请求下注，下注数据：{}, checkRes：{}", playerController.playerId(), JSON.toJSONString(reqBet), checkRes);
        if (checkRes != Code.SUCCESS) {
            notifyPlayerBet.code = checkRes;
            playerController.send(notifyPlayerBet);
            return;
        }
        long playerTotalBetGold = 0;
        notifyPlayerBet.betTableInfoList = new ArrayList<>();
        // 处理下注数据
        Map<Integer, List<Integer>> playerAreaInfoMap = gameDataVo.getPlayerBetInfo(playerController.playerId());
        Map<Integer, List<Integer>> playerReqBetMap = new HashMap<>();
        for (ReqBetBean betBean : reqBetBeans) {
            playerReqBetMap.computeIfAbsent(betBean.betAreaIdx, k -> new ArrayList<>()).add((int) betBean.betValue);
        }
        for (Map.Entry<Integer, List<Integer>> entry : playerReqBetMap.entrySet()) {
            int betAreaIdx = entry.getKey();
            long betValue = entry.getValue().stream().mapToInt(Integer::intValue).sum();
            playerTotalBetGold += betValue;
            if (playerAreaInfoMap == null) {
                playerAreaInfoMap = new HashMap<>();
            }
            playerAreaInfoMap.computeIfAbsent(
                betAreaIdx, k -> new ArrayList<>()).addAll(entry.getValue());
        }
        // 更新押注数据
        gameDataVo.updatePlayerBetInfo(playerController.playerId(), playerAreaInfoMap);
        // 前端返回
        for (ReqBetBean reqBetBean : reqBetBeans) {
            BetTableInfo betTableInfo = new BetTableInfo();
            int betAreaIdx = reqBetBean.betAreaIdx;
            long betValue = reqBetBean.betValue;
            betTableInfo.betIdx = betAreaIdx;
            betTableInfo.playerBetTotal = playerAreaInfoMap.get(betAreaIdx).stream().mapToInt(Integer::intValue).sum();
            betTableInfo.betIdxTotal = gameDataVo.getAreaTotalBet(betAreaIdx);
            // 返回下注响应消息
            betTableInfo.betValue = betValue;
            notifyPlayerBet.betTableInfoList.add(betTableInfo);
        }
        // TODO 扣除玩家金币
        gamePlayer.setGold(gamePlayer.getGold() - playerTotalBetGold);
        gamePlayer.getTableGameData().addTotalBet(playerTotalBetGold);
        notifyPlayerBet.playerCurGold = gamePlayer.getGold();
        // 向房间广播下注改变信息
        broadcastMsgToRoom(notifyPlayerBet);
    }

    /**
     * 押注前清理房间数据
     */
    protected void clearRoomData() {
    }

    @Override
    protected void robotActionOnPhaseStart(GameRobotPlayer gameRobotPlayer) {
        // 机器人押注默认行为
        // 需要将上一局的押注数据进行清除
        clearBetData(gameRobotPlayer);
        // 机器人逻辑首先进行NextTime等待再进行DelayTime再进行NextTime等待
        addFirstRobotAction(gameRobotPlayer);
    }

    /**
     * 清除押注数据
     */
    protected void clearBetData(GameRobotPlayer gameRobotPlayer) {
        // 每轮开始清除所有之前的押注数据
        gameDataVo.getPlayerBetInfo().clear();
    }

    /**
     * 机器人初始行为逻辑
     */
    protected void addFirstRobotAction(GameRobotPlayer gameRobotPlayer) {
        BetRobotCfg betRobotCfg = getRobotBetActionCfg(gameRobotPlayer);
        if (betRobotCfg == null) {
            return;
        }
        addRobotBetActionTimer(betRobotCfg, gameRobotPlayer);
    }

    /**
     * 机器人押注行为
     */
    protected void robotBetAction(GameRobotPlayer gameRobotPlayer) {
        BetRobotCfg betRobotCfg = getRobotBetActionCfg(gameRobotPlayer);
        if (betRobotCfg == null) {
            return;
        }
        // 未触发押注逻辑
        if (!RandomUtils.getRandomBoolean10000(betRobotCfg.getBetAction())) {
            addRobotBetActionTimer(betRobotCfg, gameRobotPlayer);
            return;
        }
        // 押注的随机时间
        Integer betRandTime = RandomUtils.randomMaxMinByWeightList(betRobotCfg.getDelayTime());
        if (betRandTime == null) {
            throw new GameSampleException("机器人押注随机时间错误，机器人：" + gameRobotPlayer.getId());
        }
        // 给机器人添加异步执行押注的逻辑,执行时会回调到房间线程处理
        addPhaseTimer(
            new TimerEvent<>(gameController, betRandTime, () -> doRobotBet(gameRobotPlayer, betRobotCfg)),
            RoomEventType.TRIGGER_ROBOT_BET_ACTION);
    }

    /**
     * 获取机器人押注行为配置
     */
    private BetRobotCfg getRobotBetActionCfg(GameRobotPlayer gameRobotPlayer) {
        RobotCfg robotCfg = com.jjg.game.room.sample.GameDataManager.getRobotCfg((int) gameRobotPlayer.getId());
        List<List<Integer>> betRobotId = robotCfg.getBetRobotID();
        int betActionId = RandomUtils.randomByWeightList(betRobotId);
        Integer betAction = TableSampleDataHolder.getBetActionDataCache(
            betActionId, gameDataVo.getRoomCfg().getId());
        if (betAction == null) {
            return null;
        }
        BetRobotCfg betRobotCfg = com.jjg.game.room.sample.GameDataManager.getBetRobotCfg(betAction);
        if (betRobotCfg == null) {
            throw new GameSampleException("机器人押注错误，机器人：" + gameRobotPlayer.getId()
                + "机器人押注表中未找到押注策略配置");
        }
        return betRobotCfg;
    }

    /**
     * 机器人执行押注逻辑
     */
    protected void doRobotBet(GameRobotPlayer robotPlayer, BetRobotCfg betRobotCfg) {
        // 随机一个押注区域
        int randomBetArea = robotRandomBetArea(betRobotCfg);
        // 随机押注金额
        int randomGoldIdx = RandomUtils.randomByWeightList(betRobotCfg.getBetChips());
        List<Integer> betList = gameDataVo.getRoomCfg().getBetList();
        Integer randomGold = betList.get(randomGoldIdx - 1);
        if (randomGold == null) {
            log.error("配置表异常，机器人押注筹码表：{} 下标：{} 在压分列表中找不到", BetRobotCfg.EXCEL_NAME, randomGoldIdx);
            return;
        }
        // 检查下注行为
        int code = checkBetAction(robotPlayer, randomBetArea, randomGold);
        // 检查机器人是否下注
        if (code != Code.SUCCESS) {
            return;
        }
        Map<Integer, List<Integer>> tableBetAreaInfoMap = gameDataVo.getPlayerBetInfo(robotPlayer.getId());
        if (tableBetAreaInfoMap == null) {
            tableBetAreaInfoMap = new HashMap<>();
        }
        // 更新押注数据
        gameDataVo.updatePlayerBetInfo(robotPlayer.getId(), tableBetAreaInfoMap);
        tableBetAreaInfoMap.computeIfAbsent(randomBetArea, k -> new ArrayList<>()).add(randomGold);
        NotifyPlayerBet notifyPlayerBet = new NotifyPlayerBet(Code.SUCCESS);
        notifyPlayerBet.betTableInfoList = new ArrayList<>();
        BetTableInfo betTableInfo = new BetTableInfo();
        notifyPlayerBet.betTableInfoList.add(betTableInfo);
        betTableInfo.betIdx = randomBetArea;
        notifyPlayerBet.playerId = robotPlayer.getId();
        betTableInfo.betValue = randomGold;
        betTableInfo.playerBetTotal = tableBetAreaInfoMap.get(randomBetArea).stream().mapToInt(Integer::intValue).sum();
        // 更新单局总押注数据
        betTableInfo.betIdxTotal = gameDataVo.getAreaTotalBet(randomBetArea);
        // TODO 给机器人扣除金币
        GamePlayer gamePlayer = gameDataVo.getGamePlayer(robotPlayer.getId());
        gamePlayer.setGold(gamePlayer.getGold() - randomGold);
        gamePlayer.getTableGameData().addTotalBet(randomGold);
        notifyPlayerBet.playerCurGold = gamePlayer.getGold();
        // 向玩家广播下注数据
        broadcastMsgToRoom(notifyPlayerBet);
        // 添加timer
        addRobotBetActionTimer(betRobotCfg, robotPlayer);
    }

    /**
     * 给机器人随机一个区域
     */
    protected int robotRandomBetArea(BetRobotCfg betRobotCfg) {
        return RandomUtils.randomByWeightList(betRobotCfg.getBettingArea());
    }

    /**
     * 添加机器人押注行为的timer
     */
    private void addRobotBetActionTimer(BetRobotCfg betRobotCfg, GameRobotPlayer robotPlayer) {
        // 机器人需要重复模拟下注，再随机一个定时器
        Integer roundTime = RandomUtils.randomMaxMinByWeightList(betRobotCfg.getNextTime());
        if (roundTime == null) {
            throw new GameSampleException("获取" + BetRobotCfg.EXCEL_NAME + "中的机器人再次押注等待时间 异常");
        }
        // 添加计时器，进行循环模拟押注
        addPhaseTimer(
            new TimerEvent<>(gameController, roundTime, () -> robotBetAction(robotPlayer)),
            RoomEventType.ROBOT_BET_LOOP);

    }

    /**
     * 返回押注区域配置
     */
    protected Map<Integer, BetAreaCfg> getBetAreaCfgMap() {
        return GameDataManager.getBetAreaCfgList()
            .stream()
            .filter(betRobotCfg -> betRobotCfg.getGameID() == gameController.gameControlType().getGameTypeId())
            .collect(HashMap::new, (map, cfg) -> map.put(cfg.getId(), cfg), HashMap::putAll);
    }

    /**
     * 检查是否可以进行押注
     */
    protected int checkBetAction(GamePlayer gamePlayer, int reqBetIdx, long reqBetValue) {
        ReqBetBean betBean = new ReqBetBean();
        betBean.betAreaIdx = reqBetIdx;
        betBean.betValue = reqBetValue;
        return checkBetAction(gamePlayer, Collections.singletonList(betBean));
    }

    /**
     * 检查是否可以进行押注
     */
    protected int checkBetAction(GamePlayer gamePlayer, List<ReqBetBean> reqBetBean) {
        // 检查当前区域是否还可以进行下注
        Map<Integer, BetAreaCfg> betAreaCfgMap = getBetAreaCfgMap();
        List<Integer> betList = gameDataVo.getRoomCfg().getBetList();
        // 场上最大下注倍数
        int betMax = betList.stream().max(Integer::compareTo).get();
        long totalBetValue = 0;
        Map<Integer, Long> playerReqBetMap = new HashMap<>();
        for (ReqBetBean betBean : reqBetBean) {
            playerReqBetMap.put(betBean.betAreaIdx,
                playerReqBetMap.getOrDefault(betBean.betAreaIdx, 0L) + betBean.betValue);
        }
        for (Map.Entry<Integer, Long> entry : playerReqBetMap.entrySet()) {
            // 下注区域
            int betAreaIdx = entry.getKey();
            // 下注总值
            long betValue = entry.getValue();
            // 检查是否是合法值
            if (!betAreaCfgMap.containsKey(betAreaIdx)) {
                return Code.PARAM_ERROR;
            }
            BetAreaCfg betAreaCfg = betAreaCfgMap.get(betAreaIdx);
            // 判断场上单区域的总数量是否达到上限
            // 配置的上限
            long roomIdxMaxLimit = (long) betAreaCfg.getTbUpperLimit() * betMax;
            // 当前房间的请求的下注区的总数
            long curIdxTotalBet = gameDataVo.getAreaTotalBet(betAreaIdx);
            if (curIdxTotalBet + betValue > roomIdxMaxLimit) {
                log.debug("区域：{} 房间押注总和：{} 玩家请求：{} 限制值：{}",
                    betAreaCfg.getId(), curIdxTotalBet, betValue, roomIdxMaxLimit);
                return Code.AREA_BET_TO_LIMIT;
            }
            // 玩家区域上限
            long playerIdxMaxLimit = (long) betAreaCfg.getTbPlayerUpperLimit() * betMax;
            Map<Integer, List<Integer>> playerBetInfo = gameDataVo.getPlayerBetInfo(gamePlayer.getId());
            long playerBetTotal = 0;
            if (playerBetInfo != null) {
                playerBetTotal =
                    playerBetInfo.getOrDefault(betAreaIdx, new ArrayList<>()).stream().mapToInt(Integer::intValue).sum();
            }
            if (playerBetTotal + betValue > playerIdxMaxLimit) {
                log.debug("区域：{} 玩家押注总和：{}  当前下注：{} 限制值：{}",
                    betAreaCfg.getId(), playerBetTotal, betValue, playerIdxMaxLimit);
                return Code.BET_TO_LIMIT;
            }
            totalBetValue += betValue;
        }
        // 检查玩家的钱是否带够
        long needTake = totalBetValue;
        if (needTake > gamePlayer.getGold()) {
            return Code.NOT_ENOUGH;
        }
        return Code.SUCCESS;
    }

    @Override
    public void onPlayerHalfwayJoinPhase(GamePlayer gamePlayer) {
        // 如果是机器人中途加入，加入押注逻辑
        if (gamePlayer instanceof GameRobotPlayer gameRobotPlayer) {
            robotBetAction(gameRobotPlayer);
        }
    }

    @Override
    public void onPlayerHalfwayExitPhase(GamePlayer gamePlayer) {

    }

    @Override
    protected void hostingPlayerActionOnPhaseStart(GamePlayer gamePlayer) {

    }

    @Override
    public int reqMsgId() {
        return TableRoomMessageConstant.ReqMsgBean.REQ_BET;
    }
}
