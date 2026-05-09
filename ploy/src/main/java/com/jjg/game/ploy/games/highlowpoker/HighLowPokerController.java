package com.jjg.game.ploy.games.highlowpoker;

import cn.hutool.core.collection.CollectionUtil;
import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.Pair;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.constant.GameConstant;
import com.jjg.game.core.data.Card;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.utils.PokerCardUtils;
import com.jjg.game.ploy.controller.AbstractSinglePloyController;
import com.jjg.game.ploy.data.PloyBetDivideInfo;
import com.jjg.game.ploy.games.highlowpoker.data.HighLowChoose;
import com.jjg.game.ploy.games.highlowpoker.data.HighLowPokerConstant;
import com.jjg.game.ploy.games.highlowpoker.data.HighLowPokerHistory;
import com.jjg.game.ploy.games.highlowpoker.data.HighLowPokerPloyGameData;
import com.jjg.game.ploy.games.highlowpoker.pb.bean.HighLowRecordInfo;
import com.jjg.game.ploy.games.highlowpoker.pb.req.ReqHighLowPokerChoose;
import com.jjg.game.ploy.games.highlowpoker.pb.req.ReqHighLowPokerExchange;
import com.jjg.game.ploy.games.highlowpoker.pb.res.*;
import com.jjg.game.ploy.games.highlowpoker.util.HighLowUtil;
import com.jjg.game.ploy.pb.ReqPloyRecord;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.PloygameRoomCfg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author lm
 * @date 2026/4/1 09:46
 */
@Component
public class HighLowPokerController extends AbstractSinglePloyController<HighLowPokerPloyGameData> {
    private final HighLowUtil highLowUtil;

    public HighLowPokerController(HighLowUtil highLowUtil) {
        super(LoggerFactory.getLogger(HighLowPokerController.class), HighLowPokerPloyGameData.class);
        this.highLowUtil = highLowUtil;
    }

    @Override
    public void init() {
        super.init();
        log.info("初始化高低扑克控制器");
    }

    @Override
    public AbstractMessage reqPloyRecord(PlayerController playerController, ReqPloyRecord req) {
        ResHighLowPokerRecord res = new ResHighLowPokerRecord(Code.SUCCESS);
        HighLowPokerPloyGameData playerGameData = getPlayerGameData(playerController.playerId());
        if (playerGameData == null) {
            res.code = Code.NOT_FOUND;
            log.warn("未找到玩家的 playerGameData,查看记录失败 playerId = {}", playerController.playerId());
            return res;
        }
        List<HighLowPokerHistory> totalHistories = playerGameData.getTotalHistories();
        if (CollectionUtil.isNotEmpty(totalHistories)) {
            res.historyInfoList = new ArrayList<>(totalHistories.size());
            for (HighLowPokerHistory history : totalHistories) {
                HighLowRecordInfo recordInfo = new HighLowRecordInfo();
                recordInfo.historyInfos = new ArrayList<>(history.getHistory());
                recordInfo.totalIncome = history.getTotalProfit();
                res.historyInfoList.add(recordInfo);
            }
        }
        return res;
    }

    @Override
    protected AbstractResponse buildResPloyConfigMessage(int code, int gameType, int roomCfgId, HighLowPokerPloyGameData playerGameData) {
        ResHighLowPokerEnterGame res = new ResHighLowPokerEnterGame(code);
        if (code != Code.SUCCESS) {
            return res;
        }
        PloygameRoomCfg cfg = GameDataManager.getPloygameRoomCfg(playerGameData.getRoomCfgId());
        if (cfg == null) {
            res.code = Code.SAMPLE_ERROR;
            return res;
        }
        res.stakeList = cfg.getLineBetScore();
        res.defaultBet = cfg.getDefaultBet();
        if (playerGameData.getCard() != null) {
            //发送剩余牌数
            res.remainCardNum = playerGameData.getCard().size() - playerGameData.getCurrentIndex() - 1;
            //发送每个选择区域的赔率
            BigDecimal returnRate = BigDecimal.valueOf(cfg.getRewardRate()).divide(GameConstant.TEN_THOUSAND_BD, 4, RoundingMode.DOWN);
            res.chooseRate = highLowUtil.calculateAllChooseRate(playerGameData.getCard(), playerGameData.getCurrentIndex(), returnRate);
            //发送当前牌
            res.currentCard = playerGameData.getCard().get(playerGameData.getCurrentIndex());
            //发送历史选择
            res.historyChoose = playerGameData.getHistory();
            //当前能兑换的金币
            res.currentCoin = playerGameData.getCurrentCoin();
        }
        return res;
    }

    @Override
    protected AbstractResponse buildResBetMessage(int code, HighLowPokerPloyGameData playerGameData, long betValue, int value) {
        ResHighLowPokerBet res = new ResHighLowPokerBet(code);
        if (code != Code.SUCCESS) {
            return res;
        }
        PloygameRoomCfg cfg = GameDataManager.getPloygameRoomCfg(playerGameData.getRoomCfgId());
        if (cfg == null) {
            res.code = Code.SAMPLE_ERROR;
            return res;
        }
        if (CollectionUtil.isNotEmpty(playerGameData.getCard())) {
            res.code = Code.ERROR_REQ;
            return res;
        }
        //打乱牌型
        playerGameData.setCard(PokerCardUtils.getPokerIntIdExceptJoker());
        playerGameData.setCurrentIndex(0);
        Collections.shuffle(playerGameData.getCard());
        //发送剩余牌数
        res.remainCardNum = playerGameData.getCard().size() - playerGameData.getCurrentIndex() - 1;
        //发送每个选择区域的赔率
        BigDecimal returnRate = BigDecimal.valueOf(cfg.getRewardRate()).divide(GameConstant.TEN_THOUSAND_BD, 4, RoundingMode.DOWN);
        res.chooseRate = highLowUtil.calculateAllChooseRate(playerGameData.getCard(), playerGameData.getCurrentIndex(), returnRate);
        //发送当前牌
        res.currentCard = playerGameData.getCard().get(playerGameData.getCurrentIndex());
        return res;
    }


    /**
     * 对局开始进行选择
     *
     */
    public AbstractResponse choose(PlayerController playerController, ReqHighLowPokerChoose req) {
        //TODO 奖池判断
        ResHighLowPokerChoose res = new ResHighLowPokerChoose(Code.SUCCESS);
        HighLowChoose highLowChoose = HighLowChoose.getChoose(req.chooseId);
        if (highLowChoose == null) {
            res.code = Code.PARAM_ERROR;
            return res;
        }
        HighLowPokerPloyGameData playerGameData = getPlayerGameData(playerController.playerId());
        if (playerGameData == null) {
            res.code = Code.NOT_FOUND;
            log.warn("未找到玩家的 playerGameData,选择失败 playerId = {}", playerController.playerId());
            return res;
        }
        PloygameRoomCfg cfg = GameDataManager.getPloygameRoomCfg(playerGameData.getRoomCfgId());
        if (cfg == null) {
            res.code = Code.SAMPLE_ERROR;
            return res;
        }
        //计算赔率
        List<Integer> card = playerGameData.getCard();
        if (card == null) {
            res.code = Code.ERROR_REQ;
            return res;
        }
        BigDecimal returnRate = BigDecimal.valueOf(cfg.getRewardRate()).divide(GameConstant.TEN_THOUSAND_BD, 4, RoundingMode.DOWN);
        List<String> chooseRate = highLowUtil.calculateAllChooseRate(card, playerGameData.getCurrentIndex(), returnRate);
        //获取下一张牌，获取玩家的选择，进行判断
        int nextIndex = playerGameData.getCurrentIndex() + 1;
        Card nextCard = new Card(card.get(nextIndex));
        Card oldCard = new Card(card.get(playerGameData.getCurrentIndex()));
        String rate = chooseRate.get(highLowChoose.getIndex());
        boolean check = highLowChoose.check(oldCard, nextCard);
        if (!check) {
            //失败了
            res.nextCardId = nextCard.getValue();
            res.currentCoin = 0;
            playerGameData.setCurrentCoin(0);
            playerGameData.addHistory(Pair.newPair(oldCard.getValue(), rate));
            resetData(playerGameData, 0);
            return res;
        }
        //计算可兑换金币
        long lastBet = playerGameData.getLastBet();
        long addGold = BigDecimal.valueOf(lastBet).multiply(new BigDecimal(rate)).longValue();
        playerGameData.setCurrentCoin(playerGameData.getCurrentCoin() + addGold);
        playerGameData.addHistory(Pair.newPair(oldCard.getValue(), rate));
        //如果到达30局直接退
        if (playerGameData.getHistory().size() >= HighLowPokerConstant.Common.MAX_JOIN_TIMES) {
            //获胜了
            CommonResult<Pair<PloyBetDivideInfo, Player>> winResult = winFromPool(playerGameData, playerGameData.getCurrentCoin(), cfg.getTaxRate());
            if (!winResult.success()) {
                //失败回滚
                playerGameData.getHistory().removeLast();
                playerGameData.setCurrentCoin(playerGameData.getCurrentCoin() - addGold);
                log.error("高低扑克发送奖励失败 playerId:{} winGold:{} ", playerController.playerId(), playerGameData.getCurrentCoin());
                res.code = winResult.code;
                return res;
            }
            long tax = winResult.data.getFirst().getTax();
            res.exchangeNum = playerGameData.getCurrentCoin() - tax;
            res.nextCardId = nextCard.getValue();
            resetData(playerGameData, tax);
            return res;
        }
        playerGameData.setCurrentIndex(nextIndex);
        res.currentCoin = playerGameData.getCurrentCoin();
        //重新计算赔率
        res.chooseRate = highLowUtil.calculateAllChooseRate(playerGameData.getCard(), playerGameData.getCurrentIndex(), returnRate);
        res.nextCardId = nextCard.getValue();
        return res;
    }

    /**
     * 重置数据
     *
     * @param pokerPloyGameData 玩家数据
     * @param tax
     */
    private void resetData(HighLowPokerPloyGameData pokerPloyGameData, long tax) {
        HighLowPokerHistory highLowPokerHistory = new HighLowPokerHistory();
        highLowPokerHistory.setHistory(pokerPloyGameData.getHistory());
        highLowPokerHistory.setTotalProfit(pokerPloyGameData.getCurrentCoin() - tax - pokerPloyGameData.getLastBet());
        pokerPloyGameData.setCard(null);
        pokerPloyGameData.setCurrentIndex(0);
        pokerPloyGameData.setCurrentCoin(0);
        pokerPloyGameData.addTotalHistory(highLowPokerHistory);
        pokerPloyGameData.setHistory(null);
    }

    /**
     * 兑换金币
     *
     * @param playerController 玩家控制器
     * @param req              请求
     * @return 响应
     */
    public AbstractResponse exchange(PlayerController playerController, ReqHighLowPokerExchange req) {
        ResHighLowPokerExchange res = new ResHighLowPokerExchange(Code.SUCCESS);
        HighLowPokerPloyGameData playerGameData = getPlayerGameData(playerController.playerId());
        if (playerGameData == null) {
            res.code = Code.NOT_FOUND;
            log.warn("未找到玩家的 playerGameData,兑换金币 playerId = {}", playerController.playerId());
            return res;
        }
        PloygameRoomCfg cfg = GameDataManager.getPloygameRoomCfg(playerGameData.getRoomCfgId());
        if (cfg == null) {
            res.code = Code.SAMPLE_ERROR;
            return res;
        }
        if (playerGameData.getCurrentCoin() <= 0) {
            res.code = Code.NOT_FOUND;
            log.warn("玩家当前金币小于等于0 playerId = {}", playerController.playerId());
            return res;
        }
        //兑换金币
        //获胜了
        CommonResult<Pair<PloyBetDivideInfo, Player>> winResult = winFromPool(playerGameData, playerGameData.getCurrentCoin(), cfg.getTaxRate());
        if (!winResult.success()) {
            log.error("高低扑克发送奖励失败 playerId:{} winGold:{} ", playerController.playerId(), playerGameData.getCurrentCoin());
            res.code = winResult.code;
            return res;
        }
        long tax = winResult.data.getFirst().getTax();
        res.getGoldNum = playerGameData.getCurrentCoin() - tax;
        resetData(playerGameData, tax);
        return res;
    }

    @Override
    public int getGameType() {
        return CoreConst.GameType.HIGH_LOW_POKER;
    }
}
