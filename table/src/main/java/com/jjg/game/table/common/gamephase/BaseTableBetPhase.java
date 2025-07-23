package com.jjg.game.table.common.gamephase;

import com.jjg.game.common.concurrent.IProcessorHandler;
import com.jjg.game.common.timer.TimerEvent;
import com.jjg.game.common.timer.TimerListener;
import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.exception.GameSampleException;
import com.jjg.game.room.base.AbstractMsgDealRoomPhase;
import com.jjg.game.room.constant.EGamePhase;
import com.jjg.game.room.controller.AbstractGameController;
import com.jjg.game.room.data.robot.GameRobotPlayer;
import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.room.sample.bean.BetRobotCfg;
import com.jjg.game.room.sample.bean.RobotCfg;
import com.jjg.game.room.sample.bean.Room_BetCfg;
import com.jjg.game.table.baccarat.sample.GameDataManager;
import com.jjg.game.table.baccarat.sample.bean.BetAreaCfg;
import com.jjg.game.table.common.data.TableGameDataVo;
import com.jjg.game.table.common.data.TableSampleDataHolder;
import com.jjg.game.table.common.message.TableRoomMessageConstant;
import com.jjg.game.table.common.message.req.ReqBet;
import com.jjg.game.table.common.message.req.ReqBetBean;
import com.jjg.game.table.common.message.res.BetTableInfo;
import com.jjg.game.table.common.message.res.NotifyPlayerBet;

import java.util.*;

/**
 * 押注阶段,通用押注逻辑
 *
 * @author 2CL
 */
public abstract class BaseTableBetPhase<D extends TableGameDataVo> extends AbstractMsgDealRoomPhase<Room_BetCfg,
        D, ReqBet> implements TimerListener<IProcessorHandler> {

    public BaseTableBetPhase(AbstractGameController<Room_BetCfg, D> gameController) {
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
    }

    @Override
    public void dealMsg(PlayerController playerController, ReqBet message) {
        if (gameController.getRoom().getGameType() != playerController.getPlayer().getGameType()) {
            return;
        }
        dealBet(playerController, message);
    }

    public abstract void dealBet(PlayerController playerController, ReqBet message);

    /**
     * 押注前清理房间数据
     */
    protected void clearRoomData() {
    }

    @Override
    protected void robotAction(GameRobotPlayer gameRobotPlayer) {
        // 机器人押注默认行为
        // 需要将上一局的押注数据进行清除
        clearRobotBetData(gameRobotPlayer);
        // 判断是否进行押注
        robotBetAction(gameRobotPlayer);
    }

    @Override
    public void onTimer(TimerEvent<IProcessorHandler> e) {

    }

    /**
     * 清除机器人押注数据
     */
    protected void clearRobotBetData(GameRobotPlayer gameRobotPlayer) {
        gameDataVo.updatePlayerBetInfo(gameRobotPlayer.getId(), new HashMap<>());
    }

    /**
     * 机器人押注行为
     */
    protected void robotBetAction(GameRobotPlayer gameRobotPlayer) {
        RobotCfg robotCfg = com.jjg.game.room.sample.GameDataManager.getRobotCfg((int) gameRobotPlayer.getId());
        List<List<Integer>> betRobotId = robotCfg.getBetRobotID();
        int betActionId = RandomUtils.randomByWeightList(betRobotId);
        Integer betAction = TableSampleDataHolder.getBetActionDataCache(betActionId, gameDataVo.getRoomCfg().getGameID());
        if (betAction == null) {
            return;
        }
        BetRobotCfg betRobotCfg = com.jjg.game.room.sample.GameDataManager.getBetRobotCfg(betAction);
        if (betRobotCfg == null) {
            throw new GameSampleException("机器人押注错误，机器人：" + gameRobotPlayer.getId()
                    + "机器人押注表中未找到押注策略配置");
        }
        // 未触发押注逻辑
        if (!RandomUtils.getRandomBoolean10000(betRobotCfg.getBetAction())) {
            return;
        }
        // 押注的随机时间
        Integer betRandTime = RandomUtils.randomMaxMinByWeightList(betRobotCfg.getDelayTime());
        if (betRandTime == null) {
            throw new GameSampleException("机器人押注随机时间错误，机器人：" + gameRobotPlayer.getId());
        }
        // 给机器人添加异步执行押注的逻辑,执行时会回调到房间线程处理
        addPhaseTimer(new TimerEvent<>(this, betRandTime, () -> doRobotBet(gameRobotPlayer, betRobotCfg)));
    }

    /**
     * 机器人执行押注逻辑
     */
    protected void doRobotBet(GameRobotPlayer robotPlayer, BetRobotCfg betRobotCfg) {
        // 随机一个押注区域
        int randomBetArea = RandomUtils.randomByWeightList(betRobotCfg.getBettingArea());
        // 随机押注金额
        long randomGold = RandomUtils.randomByWeightList(betRobotCfg.getBetChips());
        // 检查机器人是否下注
        if (checkBetAction(robotPlayer, randomBetArea, randomGold) != Code.SUCCESS) {
            // 不满足下注条件直接返回
            return;
        }
        Map<Integer, List<Integer>> tableBetAreaInfoMap = gameDataVo.getPlayerBetInfo(robotPlayer.getId());
        tableBetAreaInfoMap.computeIfAbsent(randomBetArea, k -> new ArrayList<>()).add((int) randomGold);
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
        notifyPlayerBet.playerCurGold = gamePlayer.getGold();
        // 向玩家广播下注数据
        broadcastMsgToRoom(notifyPlayerBet);
        // 机器人需要重复模拟下注，再随机一个定时器
        Integer roundTime = RandomUtils.randomMaxMinByWeightList(betRobotCfg.getNextTime());
        if (roundTime == null) {
            throw new GameSampleException("获取" + BetRobotCfg.EXCEL_NAME + "中的机器人再次押注等待时间 异常");
        }
        // 添加计时器，进行循环模拟押注
        addPhaseTimer(new TimerEvent<>(this, roundTime, () -> robotBetAction(robotPlayer)));
    }

    /**
     * 返回押注区域配置
     */
    protected Map<Integer, BetAreaCfg> getBetAreaCfgMap() {
        return GameDataManager.getBetAreaCfgList()
                .stream()
                .filter(betRobotCfg -> betRobotCfg.getGameID() == gameController.gameControlType().getGameTypeId())
                .collect(HashMap::new, (map, cfg) -> map.put(cfg.getAreaID(), cfg), HashMap::putAll);
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
        long totalBetValue = 0;
        int betBase = gameDataVo.getRoomCfg().getBetBase();
        for (ReqBetBean betBean : reqBetBean) {
            // 检查是否是合法值
            if (!betAreaCfgMap.containsKey(betBean.betAreaIdx)) {
                return Code.PARAM_ERROR;
            }
            BetAreaCfg betAreaCfg = betAreaCfgMap.get(betBean.betAreaIdx);
            // 判断场上单区域的总数量是否达到上限
            // 配置的上限
            int roomIdxMaxLimit = betBase * betAreaCfg.getTbUpperLimit();
            // 当前房间的请求的下注区的总数
            long curIdxTotalBet = gameDataVo.getAreaTotalBet(betBean.betAreaIdx);
            if (curIdxTotalBet + betBean.betValue >= roomIdxMaxLimit) {
                return Code.BET_TO_LIMIT;
            }
            // 玩家区域上限
            int playerIdxMaxLimit = betBase * betAreaCfg.getTbPlayerUpperLimit();
            Map<Integer, List<Integer>> playerBetInfo = gameDataVo.getPlayerBetInfo(gamePlayer.getId());
            long playerBetTotal =
                    playerBetInfo.computeIfAbsent(betBean.betAreaIdx, k -> new ArrayList<>()).stream().mapToInt(Integer::intValue).sum();
            if (playerBetTotal + betBean.betValue >= playerIdxMaxLimit) {
                return Code.BET_TO_LIMIT;
            }
            totalBetValue += betBean.betValue;
        }
        // 检查玩家的钱是否带够
        long needTake = (long) betBase * totalBetValue;
        if (needTake > gamePlayer.getGold()) {
            return Code.NOT_FOUND;
        }
        return Code.SUCCESS;
    }

    @Override
    protected void hostingPlayerAction(GamePlayer gamePlayer) {

    }

    @Override
    public int reqMsgId() {
        return TableRoomMessageConstant.ReqMsgBean.REQ_BET;
    }
}
