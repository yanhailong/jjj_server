package com.jjg.game.poker.game.blackjack.autohandler;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.RandomUtil;
import com.jjg.game.common.proto.Pair;
import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.poker.game.blackjack.data.BlackJackDataHelper;
import com.jjg.game.poker.game.blackjack.room.BlackJackGameController;
import com.jjg.game.poker.game.blackjack.room.data.BlackJackGameDataVo;
import com.jjg.game.poker.game.common.BasePokerGameController;
import com.jjg.game.poker.game.common.constant.PokerConstant;
import com.jjg.game.poker.game.common.data.PlayerSeatInfo;
import com.jjg.game.poker.game.common.data.PokerCard;
import com.jjg.game.poker.game.common.gamephase.BasePokerRobotProcessorHandler;
import com.jjg.game.poker.game.common.message.req.ReqPokerBet;
import com.jjg.game.poker.game.common.message.req.ReqPokerSampleCardOperation;
import com.jjg.game.room.constant.EGamePhase;
import com.jjg.game.room.data.robot.GameRobotPlayer;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BlackjackCfg;
import com.jjg.game.sampledata.bean.ChessJackStrategyCfg;
import com.jjg.game.sampledata.bean.ChessRobotCfg;
import com.jjg.game.sampledata.bean.Room_ChessCfg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author lm
 * @date 2025/9/30 10:53
 */
public class BlackJackRobotHandler extends BasePokerRobotProcessorHandler<BlackJackGameDataVo> {
    private static final Logger log = LoggerFactory.getLogger(BlackJackRobotHandler.class);
    //1：执行【加倍】操作，只拿一次牌并且再次下注，携带金额不足时，改为停牌
    private final int DOUBLE_BET = 1;
    //2：执行【要牌】操作，要牌后继续根据当前牌型和牌值进行行为随机
    private final int GET_CARD = 2;
    //3: 执行【停牌】操作，不再拿牌
    private final int STOP = 3;
    //4：分牌
    private final int CUT_CARD = 4;

    //handler type
    //下注
    public static final int BET = 1;
    //策略行为
    public static final int DO_STRATEGY = 2;
    //下注概率
    private final int betPro;

    public BlackJackRobotHandler(GameRobotPlayer gameRobotPlayer, int type, BasePokerGameController<BlackJackGameDataVo> gameController, int readyPro) {
        super(gameRobotPlayer, type, gameController);
        this.betPro = readyPro;
    }

    public BlackJackRobotHandler(GameRobotPlayer gameRobotPlayer, int type, BasePokerGameController<BlackJackGameDataVo> gameController) {
        super(gameRobotPlayer, type, gameController);
        this.betPro = 0;
    }

    @Override
    public void action() {
        BasePokerGameController<BlackJackGameDataVo> gameController = getGameController();
        if (gameController instanceof BlackJackGameController controller) {
            GameRobotPlayer robotPlayer = getGameRobotPlayer();
            BlackJackGameDataVo gameDataVo = controller.getGameDataVo();
            ChessRobotCfg chessRobotCfg = GameDataManager.getChessRobotCfg(robotPlayer.getActionId());
            //handler类型
            final long playerId = robotPlayer.getId();
            switch (getType()) {
                case BET -> {
                    if (CollectionUtil.isEmpty(chessRobotCfg.getBlackjackBet())) {
                        log.debug("机器人:{} 进行下注为空 pro:{}", playerId, betPro);
                        return;
                    }
                    if (controller.getCurrentGamePhase() == EGamePhase.BET && betPro > RandomUtil.randomInt(10000)) {
                        int random = RandomUtils.randomByWeight(chessRobotCfg.getBlackjackBet()) - 1;
                        BlackjackCfg blackjackCfg = BlackJackDataHelper.getBlackjackCfg(gameDataVo);
                        if (random >= 0 && random < blackjackCfg.getBetList().size()) {
                            Integer betValue = blackjackCfg.getBetList().get(random);
                            ReqPokerBet reqPokerBet = new ReqPokerBet();
                            reqPokerBet.betType = PokerConstant.PlayerOperation.BET;
                            reqPokerBet.betValue = betValue;
                            //机器人进行准备
                            controller.dealBet(getPlayerId(), reqPokerBet);
                        }
                    }
                }
                case DO_STRATEGY -> {
                    PlayerSeatInfo playerSeatInfo = gameDataVo.getCurrentPlayerSeatInfo();
                    if (playerSeatInfo == null || playerSeatInfo.getPlayerId() != playerId) {
                        return;
                    }
                    //获取点数
                    Pair<Boolean, Integer> totalPointInfo = BlackJackDataHelper.getTotalPointInfo(playerSeatInfo.getCurrentCards());
                    Map<Integer, ChessJackStrategyCfg> cfgMap = BlackJackDataHelper.getRobotActionMap().get(totalPointInfo.getFirst());
                    ChessJackStrategyCfg cfg = cfgMap.get(totalPointInfo.getSecond());
                    Map<Integer, Integer> hashMap = new HashMap<>(cfg.getStrategy());
                    //行为前置检查
                    long betValue = gameDataVo.getBaseBetInfo().getOrDefault(playerId, 0L);
                    if (controller.getTransactionItemNum(playerId) < betValue) {
                        //不能分牌 和双倍
                        hashMap.remove(DOUBLE_BET);
                        hashMap.remove(CUT_CARD);
                    }
                    if (playerSeatInfo.getCurrentCards().size() != gameDataVo.getRoomCfg().getHandPoker()) {
                        hashMap.remove(DOUBLE_BET);
                    }
                    if (!canCutCard(gameDataVo, playerSeatInfo)) {
                        hashMap.remove(CUT_CARD);
                    }
                    //行为权重随机
                    Integer next = RandomUtils.randomByWeight(hashMap);
                    ReqPokerSampleCardOperation operation = new ReqPokerSampleCardOperation();
                    switch (next) {
                        case DOUBLE_BET -> {
                            operation.type = PokerConstant.PlayerOperation.DOUBLE_BET;
                            controller.sampleCardOperation(playerId, operation);
                        }
                        case GET_CARD -> {
                            operation.type = PokerConstant.PlayerOperation.GET_CARD;
                            controller.sampleCardOperation(playerId, operation);
                        }
                        case STOP -> {
                            operation.type = PokerConstant.PlayerOperation.STOP;
                            controller.sampleCardOperation(playerId, operation);
                        }
                        case CUT_CARD -> {
                            operation.type = PokerConstant.PlayerOperation.CUT_CARD;
                            controller.sampleCardOperation(playerId, operation);
                        }
                    }
                }
            }
        }
    }

    /**
     * 是否能分牌
     */
    private boolean canCutCard(BlackJackGameDataVo gameDataVo, PlayerSeatInfo playerSeatInfo) {
        Room_ChessCfg roomCfg = gameDataVo.getRoomCfg();
        //只能分一次牌并且只能在发牌时分牌
        if (playerSeatInfo.getCurrentCards().size() != roomCfg.getHandPoker() || playerSeatInfo.getCardIndex() != 0 || playerSeatInfo.getCards().size() != 1) {
            return false;
        }
        List<Integer> cards = playerSeatInfo.getCurrentCards();
        Map<Integer, PokerCard> cardListMap = BlackJackDataHelper.getCardListMap(BlackJackDataHelper.getPoolId(gameDataVo));
        int firstCard = cardListMap.get(cards.get(0)).getRank();
        int secondCard = cardListMap.get(cards.get(1)).getRank();
        //判断是否能分牌
        return firstCard == secondCard;
    }




}
