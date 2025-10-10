package com.jjg.game.poker.game.texas.autohandler;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.lang.WeightRandom;
import cn.hutool.core.util.RandomUtil;
import com.jjg.game.poker.game.common.BasePokerGameController;
import com.jjg.game.poker.game.common.constant.PokerConstant;
import com.jjg.game.poker.game.common.data.PlayerSeatInfo;
import com.jjg.game.poker.game.common.gamephase.BasePokerRobotProcessorHandler;
import com.jjg.game.poker.game.common.message.req.ReqPokerBet;
import com.jjg.game.poker.game.texas.constant.TexasConstant;
import com.jjg.game.poker.game.texas.data.TexasDataHelper;
import com.jjg.game.poker.game.texas.message.TexasBuilder;
import com.jjg.game.poker.game.texas.room.TexasGameController;
import com.jjg.game.poker.game.texas.room.data.TexasGameDataVo;
import com.jjg.game.poker.game.texas.util.HandResult;
import com.jjg.game.room.constant.EGamePhase;
import com.jjg.game.room.data.robot.GameRobotPlayer;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.ChessRobotCfg;
import com.jjg.game.sampledata.bean.ChessTexasStrategyCfg;
import com.jjg.game.sampledata.bean.TexasCfg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * @author lm
 * @date 2025/9/30 10:53
 */
public class TexasRobotHandler extends BasePokerRobotProcessorHandler<TexasGameDataVo> {
    private static final Logger log = LoggerFactory.getLogger(TexasRobotHandler.class);
    //1：加注行为，需要判断牌桌筹码是否满足目标，加注时筹码量根据权重随机
    private final int RAISE = 1;
    //2：跟注行为，金币不足时All in
    private final int FOLLOW = 2;
    //3: 弃牌行为
    private final int DISCARD = 3;
    //4：目标筹码，加注行为时需要判断牌桌上【筹码/总筹码】与此万分比的大小
    private final int TARGET_CHIP = 4;

    //handler type
    //进行准备
    public static final int GO_READY = 1;
    //策略行为
    public static final int DO_STRATEGY = 2;
    //展示牌型
    public static final int SHOW_CARDS = 3;
    //准备概率
    private final int readyPro;

    public TexasRobotHandler(GameRobotPlayer gameRobotPlayer, int type, BasePokerGameController<TexasGameDataVo> gameController, int readyPro) {
        super(gameRobotPlayer, type, gameController);
        this.readyPro = readyPro;
    }

    public TexasRobotHandler(GameRobotPlayer gameRobotPlayer, int type, BasePokerGameController<TexasGameDataVo> gameController) {
        super(gameRobotPlayer, type, gameController);
        this.readyPro = 0;
    }

    @Override
    public void action() {
        BasePokerGameController<TexasGameDataVo> gameController = getGameController();
        if (gameController instanceof TexasGameController controller) {
            GameRobotPlayer robotPlayer = getGameRobotPlayer();
            TexasGameDataVo gameDataVo = controller.getGameDataVo();
            //handler类型
            switch (getType()) {
                case GO_READY -> {
                    log.debug("机器人:{} 进行准备 pro:{}", robotPlayer.getId(), readyPro);
                    if (controller.getCurrentGamePhase() == EGamePhase.START_GAME && readyPro > RandomUtil.randomInt(10000)) {
                        log.debug("机器人:{} 进行准备", robotPlayer.getId());
                        //机器人进行准备
                        controller.reqTexasGoReady(getPlayerId());
                    }
                }
                case DO_STRATEGY -> {
                    PlayerSeatInfo playerSeatInfo = gameDataVo.getCurrentPlayerSeatInfo();
                    if (playerSeatInfo == null || playerSeatInfo.getPlayerId() != robotPlayer.getId()) {
                        return;
                    }
                    //获取牌型
                    HandResult tempHandType = TexasBuilder.getTempHandType(playerSeatInfo, gameDataVo);
                    if (tempHandType == null) {
                        return;
                    }
                    log.debug("机器人:{} 获取牌型", tempHandType.getHandRank());
                    //获取执行id 最大点数对应执行id
                    int maxRank = tempHandType.getMaxRank();
                    log.debug("机器人:{} 最大点数", maxRank);
                    //获取执行策略
                    ChessTexasStrategyCfg robotActionCfg = TexasDataHelper.getRobotActionCfg(tempHandType.getHandRank().rank, maxRank);
                    if (robotActionCfg == null) {
                        return;
                    }
                    int round = gameDataVo.getRound();
                    log.debug("机器人:{} round", round);
                    Map<Integer, Integer> strategyDataMap = getStrategyDataMap(controller, robotActionCfg, tempHandType, round);
                    if (CollectionUtil.isEmpty(strategyDataMap)) {
                        return;
                    }
                    ChessRobotCfg chessRobotCfg = GameDataManager.getChessRobotCfg(robotPlayer.getActionId());
                    if (chessRobotCfg == null) {
                        return;
                    }
                    //根据权重值抽取行为时，需要做一次数据处理，加注的权重 = 加注权重 × 权重放大万分比 ÷ 10000
                    int raise = strategyDataMap.getOrDefault(RAISE, 0);
                    raise = raise * chessRobotCfg.getAddProactiveWeight() / 10000;
                    //随机策略
                    WeightRandom<Integer> random = new WeightRandom<>();
                    random.add(RAISE, raise);
                    random.add(FOLLOW, strategyDataMap.getOrDefault(FOLLOW, 0));
                    random.add(DISCARD, strategyDataMap.getOrDefault(DISCARD, 0));
                    Integer next = random.next();
                    log.debug("机器人:{} next", next);
                    switch (next) {
                        case RAISE -> {
                            if (gameDataVo.getRound() == TexasConstant.Common.MAX_ROUND) {
                                //计算all_in
                                if (chessRobotCfg.getAllinWeight() > RandomUtil.randomInt(10000)) {
                                    ReqPokerBet reqPokerBet = new ReqPokerBet();
                                    reqPokerBet.betType = PokerConstant.PlayerOperation.ALL_IN;
                                    controller.dealBet(robotPlayer.getId(), reqPokerBet);
                                }
                                return;
                            }
                            TexasCfg texasCfg = TexasDataHelper.getTexasCfg(gameDataVo);
                            // 	牌桌平均下注量百分比 = 当前已押注总量 ÷ （参与人数 × 当前牌局的入座金币coinsNum）
                            //当前下注
                            long currentBet = gameDataVo.getBaseBetInfo().getOrDefault(robotPlayer.getId(), 0L);
                            //总下注
                            long sum = gameDataVo.getBaseBetInfo().values().stream().mapToLong(Long::longValue).sum();
                            long proportion = sum / ((long) gameDataVo.getBaseBetInfo().size() * texasCfg.getCoinsNum());
                            // 	要求平均下注量百分比 取值 ÷ 10000，取值在chessTexasStrategy表中对应行为字段中类型为4的值
                            //大于等于【要求平均下注量百分比】，执行【跟注】行为，反之执行【加注】，
                            if (proportion >= strategyDataMap.getOrDefault(TARGET_CHIP, 0) || CollectionUtil.isEmpty(chessRobotCfg.getAddBetMultiple())) {
                                //跟注
                                followBet(controller, robotPlayer);
                            } else {
                                // 根据权重中随机获取【大盲】X倍的加注筹码，
                                // 在ChessRobot表中【addBetMultiple 加注时大盲倍数】字段根据权重取大盲倍数。当携带的筹码不足时，执行【All in】
                                //加注
                                WeightRandom<Integer> addBetRandom = new WeightRandom<>();
                                for (Map.Entry<Integer, Integer> entry : chessRobotCfg.getAddBetMultiple().entrySet()) {
                                    addBetRandom.add(entry.getKey(), entry.getValue());
                                }
                                Integer addBet = addBetRandom.next();
                                long addValue = gameDataVo.getMaxBetValue() - currentBet + (long) addBet * texasCfg.getBbNum();
                                ReqPokerBet reqPokerBet = new ReqPokerBet();
                                reqPokerBet.betType = PokerConstant.PlayerOperation.BET;
                                reqPokerBet.betValue = addValue;
                                controller.dealBet(robotPlayer.getId(), reqPokerBet);
                            }
                        }
                        case FOLLOW -> {
                            log.debug("机器人:{} 跟牌", tempHandType);
                            followBet(controller, robotPlayer);
                        }
                        case DISCARD -> {
                            log.debug("机器人:{} 弃牌", tempHandType);
                            controller.discardCard(robotPlayer.getId());
                        }
                    }
                }
                case SHOW_CARDS -> {
                    //是否显示牌型
                    ChessRobotCfg chessRobotCfg = GameDataManager.getChessRobotCfg(robotPlayer.getActionId());
                    if (chessRobotCfg == null) {
                        return;
                    }
                    if (chessRobotCfg.getContinueAfterFail().getLast() > RandomUtil.randomInt(10000)) {
                        controller.reqShowCard(robotPlayer.getId(), controller);
                    }
                }
            }
        }
    }

    /**
     * 德州机器人跟注
     *
     * @param controller  游戏控制器
     * @param robotPlayer 机器人
     */
    private void followBet(TexasGameController controller, GameRobotPlayer robotPlayer) {

        //钱不够跟注直接All
        TexasGameDataVo gameDataVo = controller.getGameDataVo();
        //判断是否可以跟
        if (gameDataVo.getMaxBetValue() == 0) {
            controller.passCards(robotPlayer.getId());
            return;
        }
        ReqPokerBet reqPokerBet = new ReqPokerBet();
        reqPokerBet.betType = PokerConstant.PlayerOperation.FOLLOW_CARD;
        long need = gameDataVo.getMaxBetValue() - gameDataVo.getBaseBetInfo().getOrDefault(robotPlayer.getId(), 0L);

        if (need >= gameDataVo.getTempGold().getOrDefault(robotPlayer.getId(), 0L)) {
            reqPokerBet.betType = PokerConstant.PlayerOperation.ALL_IN;
        }
        controller.dealBet(robotPlayer.getId(), reqPokerBet);
    }

    /**
     * 获取德州机器人策略map
     *
     * @param gameController 游戏控制器
     * @param cfg            策略配置
     * @param tempHandType   机器人牌型
     * @param round          当前轮数
     * @return 策略
     */
    public Map<Integer, Integer> getStrategyDataMap(TexasGameController gameController, ChessTexasStrategyCfg cfg, HandResult tempHandType, int round) {
        TexasGameDataVo gameDataVo = gameController.getGameDataVo();
        PlayerSeatInfo raiseBetPlayer = gameController.getRaiseBetPlayer();
        boolean isRaise = raiseBetPlayer != null;
        //获取
        switch (round) {
            case 1: {
                return isRaise ? cfg.getPassiveStrategy_1() : cfg.getProactiveStrategy_1();
            }
            case 2: {
                //加注比牌
                if (isRaise) {
                    HandResult other = TexasBuilder.getTempHandType(raiseBetPlayer, gameDataVo);
                    if (other.compareTo(tempHandType) <= 0) {
                        return cfg.getPassiveStrategyWin_2();
                    }
                    return cfg.getPassiveStrategyFailed_2();
                } else {
                    return cfg.getProactiveStrategy_2();
                }
            }
            case 3: {
                if (isRaise) {
                    HandResult other = TexasBuilder.getTempHandType(raiseBetPlayer, gameDataVo);
                    if (other.compareTo(tempHandType) <= 0) {
                        return cfg.getPassiveStrategyWin_3();
                    }
                    return cfg.getPassiveStrategyFailed_3();
                } else {
                    return cfg.getProactiveStrategy_3();
                }
            }
            case 4: {
                if (isRaise) {
                    HandResult other = TexasBuilder.getTempHandType(raiseBetPlayer, gameDataVo);
                    if (other.compareTo(tempHandType) <= 0) {
                        return cfg.getPassiveStrategyWin_4();
                    }
                    return cfg.getPassiveStrategyFailed_4();
                } else {
                    return cfg.getProactiveStrategy_4();
                }
            }
        }
        return Map.of();
    }


}
