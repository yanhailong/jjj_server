package com.jjg.game.ploy.games.hillo;

import cn.hutool.core.collection.CollectionUtil;
import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.Pair;
import com.jjg.game.common.timer.TimerEvent;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.constant.GameConstant;
import com.jjg.game.core.data.Card;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.ploy.controller.AbstractSinglePloyController;
import com.jjg.game.ploy.data.PloyBetDivideInfo;
import com.jjg.game.ploy.games.hillo.data.HilloChoose;
import com.jjg.game.ploy.games.hillo.data.HilloConstant;
import com.jjg.game.ploy.games.hillo.data.HilloHistory;
import com.jjg.game.ploy.games.hillo.data.HilloHistoryInfo;
import com.jjg.game.ploy.games.hillo.data.HilloPloyGameData;
import com.jjg.game.ploy.games.hillo.pb.bean.HilloChooseInfo;
import com.jjg.game.ploy.games.hillo.pb.bean.HilloRecordInfo;
import com.jjg.game.ploy.games.hillo.pb.req.ReqHilloAutoBet;
import com.jjg.game.ploy.games.hillo.pb.req.ReqHilloCancelAuto;
import com.jjg.game.ploy.games.hillo.pb.req.ReqHilloChoose;
import com.jjg.game.ploy.games.hillo.pb.req.ReqHilloExchange;
import com.jjg.game.ploy.games.hillo.pb.req.ReqHilloSkip;
import com.jjg.game.ploy.games.hillo.pb.res.ResHilloAutoBet;
import com.jjg.game.ploy.games.hillo.pb.res.ResHilloBet;
import com.jjg.game.ploy.games.hillo.pb.res.ResHilloChoose;
import com.jjg.game.ploy.games.hillo.pb.res.ResHilloEnterGame;
import com.jjg.game.ploy.games.hillo.pb.res.ResHilloExchange;
import com.jjg.game.ploy.games.hillo.pb.res.ResHilloRecord;
import com.jjg.game.ploy.games.hillo.pb.res.ResHilloSkip;
import com.jjg.game.ploy.games.hillo.util.HilloUtil;
import com.jjg.game.ploy.pb.ReqPloyRecord;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.PloygameRoomCfg;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Component
public class HilloController extends AbstractSinglePloyController<HilloPloyGameData> {
    private static final String AUTO_TIMER_PREFIX = "hillo:auto:";
    private static final BigDecimal AUTO_WIN_RATE_THRESHOLD = BigDecimal.valueOf(HilloConstant.Common.AUTO_WIN_RATE_THRESHOLD)
            .divide(GameConstant.TEN_THOUSAND_BD, 4, RoundingMode.DOWN);
    private final HilloUtil hilloUtil;

    public HilloController(HilloUtil hilloUtil) {
        super(LoggerFactory.getLogger(HilloController.class), HilloPloyGameData.class);
        this.hilloUtil = hilloUtil;
    }

    @Override
    public AbstractMessage reqPloyRecord(PlayerController playerController, ReqPloyRecord req) {
        ResHilloRecord res = new ResHilloRecord(Code.SUCCESS);
        HilloPloyGameData playerGameData = getPlayerGameData(playerController.playerId());
        if (playerGameData == null) {
            res.code = Code.NOT_FOUND;
            return res;
        }
        List<HilloHistory> totalHistories = playerGameData.getTotalHistories();
        if (CollectionUtil.isNotEmpty(totalHistories)) {
            res.historyInfoList = new ArrayList<>(totalHistories.size());
            // 最新在前
            for (int i = totalHistories.size() - 1; i >= 0; i--) {
                HilloHistory history = totalHistories.get(i);
                HilloRecordInfo recordInfo = new HilloRecordInfo();
                if (CollectionUtil.isNotEmpty(history.getHistory())) {
                    recordInfo.historyInfos = new ArrayList<>(history.getHistory());
                }
                recordInfo.totalIncome = history.getTotalProfit();
                recordInfo.startTime = history.getStartTime();
                recordInfo.bet = history.getBet();
                recordInfo.betMode = history.getBetMode();
                recordInfo.balanceAfter = history.getBalanceAfter();
                res.historyInfoList.add(recordInfo);
            }
        }
        return res;
    }

    @Override
    public int beforeMoneyToPoolCheck(HilloPloyGameData playerGameData) {
        return playerGameData.hasActiveGame() ? Code.ERROR_REQ : Code.SUCCESS;
    }

    @Override
    protected AbstractResponse buildResPloyConfigMessage(int code, int gameType, int roomCfgId, HilloPloyGameData playerGameData) {
        ResHilloEnterGame res = new ResHilloEnterGame(code);
        if (code != Code.SUCCESS) {
            return res;
        }
        PloygameRoomCfg cfg = GameDataManager.getPloygameRoomCfg(roomCfgId);
        if (cfg == null) {
            res.code = Code.SAMPLE_ERROR;
            return res;
        }
        res.stakeList = cfg.getLineBetScore();
        res.defaultBet = cfg.getDefaultBet();
        res.remainRoundNum = HilloConstant.Common.MAX_JOIN_TIMES;
        res.remainSkipTimes = HilloConstant.Common.MAX_SKIP_TIMES;
        if (playerGameData != null && playerGameData.hasActiveGame()) {
            res.currentCard = playerGameData.getCurrentCardId();
            res.currentCoin = playerGameData.getCurrentCoin();
            res.historyChoose = playerGameData.getHistory();
            res.remainRoundNum = HilloConstant.Common.MAX_JOIN_TIMES - playerGameData.getSuccessTimes();
            res.remainSkipTimes = playerGameData.getSkipTimes();
            res.currentBetMode = playerGameData.getCurrentBetMode();
            res.chooseInfos = hilloUtil.buildChooseInfos(playerGameData.getCurrentCardId(), getReturnRate(cfg));
        }
        if (playerGameData != null) {
            res.autoBetting = playerGameData.isAutoBetting();
            res.autoInfiniteBet = playerGameData.isAutoInfiniteBet();
            res.autoBet = playerGameData.getAutoBet();
            res.autoGuessTimes = playerGameData.getAutoGuessTimes();
            res.autoRemainBetTimes = playerGameData.getAutoRemainBetTimes();
        }
        return res;
    }

    @Override
    protected AbstractResponse buildResBetMessage(int code, HilloPloyGameData playerGameData, long betValue, int value) {
        ResHilloBet res = new ResHilloBet(code);
        if (code != Code.SUCCESS) {
            return res;
        }
        if (playerGameData == null) {
            res.code = Code.FAIL;
            return res;
        }
        PloygameRoomCfg cfg = GameDataManager.getPloygameRoomCfg(playerGameData.getRoomCfgId());
        if (cfg == null) {
            res.code = Code.SAMPLE_ERROR;
            return res;
        }
        if (!isValidBetMode(value)) {
            res.code = Code.PARAM_ERROR;
            return res;
        }

        int currentCardId = hilloUtil.randomCardId();
        playerGameData.setCurrentCardId(currentCardId);
        playerGameData.setCurrentCoin(0);
        playerGameData.setSuccessTimes(0);
        playerGameData.setSkipTimes(HilloConstant.Common.MAX_SKIP_TIMES);
        playerGameData.setCurrentBetMode(value);
        playerGameData.setCurrentRoundStartTime(System.currentTimeMillis());
        playerGameData.setHistory(null);

        res.currentCard = currentCardId;
        res.remainRoundNum = HilloConstant.Common.MAX_JOIN_TIMES;
        res.remainSkipTimes = playerGameData.getSkipTimes();
        res.chooseInfos = hilloUtil.buildChooseInfos(currentCardId, getReturnRate(cfg));
        return res;
    }

    public AbstractResponse choose(PlayerController playerController, ReqHilloChoose req) {
        ResHilloChoose res = new ResHilloChoose(Code.SUCCESS);
        HilloChoose hilloChoose = HilloChoose.getChoose(req.chooseId);
        if (hilloChoose == null) {
            res.code = Code.PARAM_ERROR;
            return res;
        }

        HilloPloyGameData playerGameData = getPlayerGameData(playerController.playerId());
        if (playerGameData == null || !playerGameData.hasActiveGame()) {
            res.code = Code.NOT_FOUND;
            return res;
        }

        PloygameRoomCfg cfg = GameDataManager.getPloygameRoomCfg(playerGameData.getRoomCfgId());
        if (cfg == null) {
            res.code = Code.SAMPLE_ERROR;
            return res;
        }

        BigDecimal returnRate = getReturnRate(cfg);
        List<HilloChooseInfo> chooseInfos = hilloUtil.buildChooseInfos(playerGameData.getCurrentCardId(), returnRate);
        HilloChooseInfo chooseInfo = chooseInfos.stream()
                .filter(info -> info.chooseId == req.chooseId)
                .findFirst()
                .orElse(null);
        if (chooseInfo == null) {
            res.code = Code.PARAM_ERROR;
            return res;
        }

        Card currentCard = new Card(playerGameData.getCurrentCardId());
        int nextCardId = hilloUtil.randomCardId();
        Card nextCard = new Card(nextCardId);

        if (!hilloChoose.check(currentCard, nextCard)) {
            playerGameData.addHistory(playerGameData.getCurrentCardId(), req.chooseId, chooseInfo.odd, nextCardId);
            settleAndArchive(playerGameData, 0, 0, getCurrentBalance(playerGameData));
            res.nextCardId = nextCardId;
            return res;
        }

        long nextCoin = calculateNextCoin(playerGameData.getLastBet(), playerGameData.getCurrentCoin(), chooseInfo.odd);
        int successTimes = playerGameData.getSuccessTimes() + 1;
        if (successTimes >= HilloConstant.Common.MAX_JOIN_TIMES) {
            CommonResult<Pair<PloyBetDivideInfo, Player>> winResult = winFromPool(playerGameData, nextCoin, cfg.getTaxRate());
            if (!winResult.success()) {
                res.code = winResult.code;
                return res;
            }
            long tax = winResult.data.getFirst().getTax();
            res.nextCardId = nextCardId;
            res.exchangeNum = nextCoin - tax;
            playerGameData.addHistory(playerGameData.getCurrentCardId(), req.chooseId, chooseInfo.odd, nextCardId);
            settleAndArchive(playerGameData, tax, nextCoin, winResult.data.getSecond().getGold());
            return res;
        }

        playerGameData.addHistory(playerGameData.getCurrentCardId(), req.chooseId, chooseInfo.odd, nextCardId);
        playerGameData.setCurrentCardId(nextCardId);
        playerGameData.setCurrentCoin(nextCoin);
        playerGameData.setSuccessTimes(successTimes);

        res.nextCardId = nextCardId;
        res.currentCoin = nextCoin;
        res.remainRoundNum = HilloConstant.Common.MAX_JOIN_TIMES - successTimes;
        res.remainSkipTimes = playerGameData.getSkipTimes();
        res.chooseInfos = hilloUtil.buildChooseInfos(nextCardId, returnRate);
        return res;
    }

    public AbstractResponse exchange(PlayerController playerController, ReqHilloExchange req) {
        ResHilloExchange res = new ResHilloExchange(Code.SUCCESS);
        HilloPloyGameData playerGameData = getPlayerGameData(playerController.playerId());
        if (playerGameData == null || !playerGameData.hasActiveGame()) {
            res.code = Code.NOT_FOUND;
            return res;
        }
        PloygameRoomCfg cfg = GameDataManager.getPloygameRoomCfg(playerGameData.getRoomCfgId());
        if (cfg == null) {
            res.code = Code.SAMPLE_ERROR;
            return res;
        }
        if (playerGameData.getCurrentCoin() <= 0) {
            res.code = Code.ERROR_REQ;
            return res;
        }

        long settleCoin = playerGameData.getCurrentCoin();
        CommonResult<Pair<PloyBetDivideInfo, Player>> winResult = winFromPool(playerGameData, settleCoin, cfg.getTaxRate());
        if (!winResult.success()) {
            res.code = winResult.code;
            return res;
        }
        long tax = winResult.data.getFirst().getTax();
        res.getGoldNum = settleCoin - tax;
        settleAndArchive(playerGameData, tax, settleCoin, winResult.data.getSecond().getGold());
        return res;
    }

    public AbstractResponse skip(PlayerController playerController, ReqHilloSkip req) {
        ResHilloSkip res = new ResHilloSkip(Code.SUCCESS);
        HilloPloyGameData playerGameData = getPlayerGameData(playerController.playerId());
        if (playerGameData == null || !playerGameData.hasActiveGame()) {
            res.code = Code.NOT_FOUND;
            return res;
        }
        // 起手牌（successTimes == 0）不允许跳过
        if (playerGameData.getSuccessTimes() <= 0) {
            res.code = Code.ERROR_REQ;
            return res;
        }
        if (playerGameData.getSkipTimes() <= 0) {
            res.code = Code.ERROR_REQ;
            return res;
        }
        PloygameRoomCfg cfg = GameDataManager.getPloygameRoomCfg(playerGameData.getRoomCfgId());
        if (cfg == null) {
            res.code = Code.SAMPLE_ERROR;
            return res;
        }

        playerGameData.addSkipHistory(playerGameData.getCurrentCardId());
        int newCardId = hilloUtil.randomCardId();
        playerGameData.setCurrentCardId(newCardId);
        playerGameData.setSkipTimes(playerGameData.getSkipTimes() - 1);

        res.currentCard = newCardId;
        res.remainSkipTimes = playerGameData.getSkipTimes();
        res.chooseInfos = hilloUtil.buildChooseInfos(newCardId, getReturnRate(cfg));
        return res;
    }

    public AbstractResponse startAutoBet(PlayerController playerController, ReqHilloAutoBet req) {
        ResHilloAutoBet res = new ResHilloAutoBet(Code.SUCCESS);
        HilloPloyGameData playerGameData = getPlayerGameData(playerController.playerId());
        if (playerGameData == null) {
            res.code = Code.NOT_FOUND;
            return res;
        }
        if (req.betTimes < 0) {
            res.code = Code.PARAM_ERROR;
            return res;
        }
        if (playerGameData.hasActiveGame() && playerGameData.getCurrentBetMode() != HilloConstant.BetMode.AUTO) {
            res.code = Code.ERROR_REQ;
            return res;
        }

        playerGameData.setAutoBetting(true);
        playerGameData.setAutoInfiniteBet(req.betTimes == 0);
        playerGameData.setAutoBet(req.bet);
        playerGameData.setAutoGuessTimes(normalizeAutoGuessTimes(req.guessTimes));
        playerGameData.setAutoRemainBetTimes(req.betTimes);

        res = runAutoStep(playerController, playerGameData);
        if (playerGameData.isAutoBetting()) {
            startAutoTimer(playerController.playerId());
        }
        return res;
    }

    public AbstractResponse cancelAutoBet(PlayerController playerController, ReqHilloCancelAuto req) {
        ResHilloAutoBet res = new ResHilloAutoBet(Code.SUCCESS);
        HilloPloyGameData playerGameData = getPlayerGameData(playerController.playerId());
        if (playerGameData == null) {
            res.code = Code.NOT_FOUND;
            return res;
        }

        stopAutoTimer(playerController.playerId());
        PloygameRoomCfg cfg = GameDataManager.getPloygameRoomCfg(playerGameData.getRoomCfgId());
        if (cfg != null && playerGameData.hasActiveGame() && playerGameData.getCurrentCoin() > 0) {
            fillAutoExchangeResult(res, playerGameData, cfg);
        } else {
            res.action = HilloConstant.AutoAction.STOP;
        }
        playerGameData.clearAutoBet();
        fillAutoState(res, playerGameData, cfg);
        return res;
    }

    @Override
    public void onTimer(TimerEvent<String> e) {
        String parameter = e.getParameter();
        if (parameter == null || !parameter.startsWith(AUTO_TIMER_PREFIX)) {
            return;
        }
        long playerId;
        try {
            playerId = Long.parseLong(parameter.substring(AUTO_TIMER_PREFIX.length()));
        } catch (NumberFormatException ex) {
            timerCenter.remove(this, parameter);
            return;
        }

        HilloPloyGameData playerGameData = getPlayerGameData(playerId);
        if (playerGameData == null || !playerGameData.isAutoBetting()) {
            timerCenter.remove(this, parameter);
            return;
        }
        PlayerController playerController = playerGameData.getPlayerController();
        if (playerController == null) {
            playerGameData.clearAutoBet();
            timerCenter.remove(this, parameter);
            return;
        }

        ResHilloAutoBet res = runAutoStep(playerController, playerGameData);
        playerController.send(res);
        if (!playerGameData.isAutoBetting()) {
            timerCenter.remove(this, parameter);
        }
    }

    @Override
    public int getGameType() {
        return CoreConst.GameType.HILLO;
    }

    private ResHilloAutoBet runAutoStep(PlayerController playerController, HilloPloyGameData playerGameData) {
        ResHilloAutoBet res = new ResHilloAutoBet(Code.SUCCESS);
        PloygameRoomCfg cfg = GameDataManager.getPloygameRoomCfg(playerGameData.getRoomCfgId());
        if (cfg == null) {
            res.code = Code.SAMPLE_ERROR;
            stopAutoBet(playerController.playerId(), playerGameData);
            return res;
        }
        if (!playerGameData.isAutoBetting()) {
            res.code = Code.ERROR_REQ;
            fillAutoState(res, playerGameData, cfg);
            return res;
        }

        if (!playerGameData.hasActiveGame()) {
            if (!playerGameData.isAutoInfiniteBet() && playerGameData.getAutoRemainBetTimes() <= 0) {
                stopAutoBet(playerController.playerId(), playerGameData);
                res.action = HilloConstant.AutoAction.STOP;
                fillAutoState(res, playerGameData, cfg);
                return res;
            }
            return startAutoRound(playerController, playerGameData, cfg);
        }

        if (playerGameData.getCurrentCoin() > 0 && playerGameData.getSuccessTimes() >= playerGameData.getAutoGuessTimes()) {
            fillAutoExchangeResult(res, playerGameData, cfg);
            fillAutoState(res, playerGameData, cfg);
            return res;
        }

        AutoDecision decision = buildAutoDecision(playerGameData, cfg);
        if (decision.skip()) {
            fillAutoSkipResult(res, playerGameData, cfg);
        } else {
            fillAutoChooseResult(res, playerController.playerId(), playerGameData, cfg, decision.chooseId());
        }
        stopAutoIfNoMoreRounds(playerController.playerId(), playerGameData);
        fillAutoState(res, playerGameData, cfg);
        return res;
    }

    private ResHilloAutoBet startAutoRound(PlayerController playerController, HilloPloyGameData playerGameData, PloygameRoomCfg cfg) {
        AbstractMessage betMessage = bet(playerGameData, playerGameData.getAutoBet(), HilloConstant.BetMode.AUTO);
        ResHilloAutoBet res = new ResHilloAutoBet(betMessage instanceof AbstractResponse response ? response.code : Code.FAIL);
        if (res.code != Code.SUCCESS || !(betMessage instanceof ResHilloBet betRes)) {
            stopAutoBet(playerController.playerId(), playerGameData);
            res.action = HilloConstant.AutoAction.STOP;
            fillAutoState(res, playerGameData, cfg);
            return res;
        }

        playerGameData.decreaseAutoRemainBetTimes();
        res.action = HilloConstant.AutoAction.START_ROUND;
        res.currentCard = betRes.currentCard;
        res.remainRoundNum = betRes.remainRoundNum;
        res.remainSkipTimes = betRes.remainSkipTimes;
        res.chooseInfos = betRes.chooseInfos;
        fillAutoState(res, playerGameData, cfg);
        return res;
    }

    private void fillAutoChooseResult(ResHilloAutoBet res, long playerId, HilloPloyGameData playerGameData,
                                      PloygameRoomCfg cfg, int chooseId) {
        BigDecimal returnRate = getReturnRate(cfg);
        HilloChoose hilloChoose = HilloChoose.getChoose(chooseId);
        HilloChooseInfo chooseInfo = hilloUtil.buildChooseInfos(playerGameData.getCurrentCardId(), returnRate).stream()
                .filter(info -> info.chooseId == chooseId)
                .findFirst()
                .orElse(null);
        if (hilloChoose == null || chooseInfo == null) {
            res.code = Code.PARAM_ERROR;
            stopAutoBet(playerId, playerGameData);
            return;
        }

        Card currentCard = new Card(playerGameData.getCurrentCardId());
        int nextCardId = hilloUtil.randomCardId();
        Card nextCard = new Card(nextCardId);
        res.chooseId = chooseId;
        res.nextCardId = nextCardId;

        if (!hilloChoose.check(currentCard, nextCard)) {
            playerGameData.addHistory(playerGameData.getCurrentCardId(), chooseId, chooseInfo.odd, nextCardId);
            settleAndArchive(playerGameData, 0, 0, getCurrentBalance(playerGameData));
            res.action = HilloConstant.AutoAction.ROUND_LOSE;
            return;
        }

        long nextCoin = calculateNextCoin(playerGameData.getLastBet(), playerGameData.getCurrentCoin(), chooseInfo.odd);
        int successTimes = playerGameData.getSuccessTimes() + 1;
        if (successTimes >= HilloConstant.Common.MAX_JOIN_TIMES || successTimes >= playerGameData.getAutoGuessTimes()) {
            CommonResult<Pair<PloyBetDivideInfo, Player>> winResult = winFromPool(playerGameData, nextCoin, cfg.getTaxRate());
            if (!winResult.success()) {
                res.code = winResult.code;
                stopAutoBet(playerId, playerGameData);
                return;
            }
            long tax = winResult.data.getFirst().getTax();
            res.action = HilloConstant.AutoAction.EXCHANGE;
            res.exchangeNum = nextCoin - tax;
            playerGameData.addHistory(playerGameData.getCurrentCardId(), chooseId, chooseInfo.odd, nextCardId);
            settleAndArchive(playerGameData, tax, nextCoin, winResult.data.getSecond().getGold());
            return;
        }

        playerGameData.addHistory(playerGameData.getCurrentCardId(), chooseId, chooseInfo.odd, nextCardId);
        playerGameData.setCurrentCardId(nextCardId);
        playerGameData.setCurrentCoin(nextCoin);
        playerGameData.setSuccessTimes(successTimes);

        res.action = HilloConstant.AutoAction.CHOOSE;
        res.currentCoin = nextCoin;
        res.remainRoundNum = HilloConstant.Common.MAX_JOIN_TIMES - successTimes;
        res.remainSkipTimes = playerGameData.getSkipTimes();
        res.chooseInfos = hilloUtil.buildChooseInfos(nextCardId, returnRate);
    }

    private void fillAutoSkipResult(ResHilloAutoBet res, HilloPloyGameData playerGameData, PloygameRoomCfg cfg) {
        playerGameData.addSkipHistory(playerGameData.getCurrentCardId());
        int newCardId = hilloUtil.randomCardId();
        playerGameData.setCurrentCardId(newCardId);
        playerGameData.setSkipTimes(playerGameData.getSkipTimes() - 1);

        res.action = HilloConstant.AutoAction.SKIP;
        res.currentCard = newCardId;
        res.remainSkipTimes = playerGameData.getSkipTimes();
        res.chooseInfos = hilloUtil.buildChooseInfos(newCardId, getReturnRate(cfg));
    }

    private void fillAutoExchangeResult(ResHilloAutoBet res, HilloPloyGameData playerGameData, PloygameRoomCfg cfg) {
        long settleCoin = playerGameData.getCurrentCoin();
        CommonResult<Pair<PloyBetDivideInfo, Player>> winResult = winFromPool(playerGameData, settleCoin, cfg.getTaxRate());
        if (!winResult.success()) {
            res.code = winResult.code;
            playerGameData.clearAutoBet();
            return;
        }
        long tax = winResult.data.getFirst().getTax();
        res.action = HilloConstant.AutoAction.EXCHANGE;
        res.exchangeNum = settleCoin - tax;
        settleAndArchive(playerGameData, tax, settleCoin, winResult.data.getSecond().getGold());
    }

    private AutoDecision buildAutoDecision(HilloPloyGameData playerGameData, PloygameRoomCfg cfg) {
        List<HilloChooseInfo> chooseInfos = hilloUtil.buildChooseInfos(playerGameData.getCurrentCardId(), getReturnRate(cfg));
        HilloChooseInfo bestChooseInfo = null;
        for (HilloChooseInfo chooseInfo : chooseInfos) {
            if (bestChooseInfo == null || new BigDecimal(chooseInfo.winRate).compareTo(new BigDecimal(bestChooseInfo.winRate)) > 0) {
                bestChooseInfo = chooseInfo;
            }
        }
        if (bestChooseInfo == null) {
            return new AutoDecision(HilloChoose.GREATER_EQUAL.getChooseId(), false);
        }

        BigDecimal bestWinRate = new BigDecimal(bestChooseInfo.winRate);
        if (bestWinRate.compareTo(AUTO_WIN_RATE_THRESHOLD) >= 0 || playerGameData.getSuccessTimes() <= 0 || playerGameData.getSkipTimes() <= 0) {
            return new AutoDecision(bestChooseInfo.chooseId, false);
        }
        return new AutoDecision(0, true);
    }

    private int normalizeAutoGuessTimes(int guessTimes) {
        if (guessTimes < 1) {
            return 1;
        }
        return Math.min(guessTimes, HilloConstant.Common.MAX_JOIN_TIMES);
    }

    private void fillAutoState(ResHilloAutoBet res, HilloPloyGameData playerGameData, PloygameRoomCfg cfg) {
        res.autoBetting = playerGameData.isAutoBetting();
        res.autoInfiniteBet = playerGameData.isAutoInfiniteBet();
        res.remainBetTimes = playerGameData.getAutoRemainBetTimes();
        if (playerGameData.hasActiveGame()) {
            res.currentCard = playerGameData.getCurrentCardId();
            res.currentCoin = playerGameData.getCurrentCoin();
            res.remainRoundNum = HilloConstant.Common.MAX_JOIN_TIMES - playerGameData.getSuccessTimes();
            res.remainSkipTimes = playerGameData.getSkipTimes();
            res.historyChoose = playerGameData.getHistory();
            if (cfg != null) {
                res.chooseInfos = hilloUtil.buildChooseInfos(playerGameData.getCurrentCardId(), getReturnRate(cfg));
            }
        }
    }

    private void startAutoTimer(long playerId) {
        String timerKey = buildAutoTimerKey(playerId);
        timerCenter.remove(this, timerKey);
        timerCenter.add(new TimerEvent<>(this, timerKey, HilloConstant.Common.AUTO_INTERVAL_MS,
                TimerEvent.INFINITE_CYCLE, HilloConstant.Common.AUTO_INTERVAL_MS));
    }

    private void stopAutoTimer(long playerId) {
        timerCenter.remove(this, buildAutoTimerKey(playerId));
    }

    private void stopAutoBet(long playerId, HilloPloyGameData playerGameData) {
        playerGameData.clearAutoBet();
        stopAutoTimer(playerId);
    }

    private void stopAutoIfNoMoreRounds(long playerId, HilloPloyGameData playerGameData) {
        if (!playerGameData.hasActiveGame() && !playerGameData.isAutoInfiniteBet() && playerGameData.getAutoRemainBetTimes() <= 0) {
            stopAutoBet(playerId, playerGameData);
        }
    }

    private String buildAutoTimerKey(long playerId) {
        return AUTO_TIMER_PREFIX + playerId;
    }

    private record AutoDecision(int chooseId, boolean skip) {
    }

    private BigDecimal getReturnRate(PloygameRoomCfg cfg) {
        return BigDecimal.valueOf(cfg.getRewardRate()).divide(GameConstant.TEN_THOUSAND_BD, 4, RoundingMode.DOWN);
    }

    private long calculateNextCoin(long lastBet, long currentCoin, String odd) {
        long baseCoin = currentCoin > 0 ? currentCoin : lastBet;
        return BigDecimal.valueOf(baseCoin)
                .multiply(new BigDecimal(odd))
                .setScale(0, RoundingMode.DOWN)
                .longValue();
    }

    private boolean isValidBetMode(int betMode) {
        return betMode == HilloConstant.BetMode.MANUAL || betMode == HilloConstant.BetMode.AUTO;
    }

    private long getCurrentBalance(HilloPloyGameData playerGameData) {
        Player player = playerService.get(playerGameData.playerId());
        if (player != null) {
            return player.getGold();
        }
        PloyBetDivideInfo divideInfo = playerGameData.getPloyBetDivideInfo();
        return divideInfo != null ? divideInfo.getPlayerAfterMoney() : 0;
    }

    private void settleAndArchive(HilloPloyGameData playerGameData, long tax, long settleCoin, long balanceAfter) {
        HilloHistory history = new HilloHistory();
        List<HilloHistoryInfo> roundHistory = playerGameData.getHistory();
        if (CollectionUtil.isNotEmpty(roundHistory)) {
            history.setHistory(new ArrayList<>(roundHistory));
        }
        history.setTotalProfit(settleCoin - tax - playerGameData.getLastBet());
        history.setStartTime(playerGameData.getCurrentRoundStartTime());
        history.setBet(playerGameData.getLastBet());
        history.setBetMode(playerGameData.getCurrentBetMode());
        history.setBalanceAfter(balanceAfter);
        playerGameData.addTotalHistory(history);

        playerGameData.setCurrentCardId(0);
        playerGameData.setCurrentCoin(0);
        playerGameData.setSuccessTimes(0);
        playerGameData.setSkipTimes(0);
        playerGameData.setHistory(null);
        playerGameData.setPloyBetDivideInfo(null);
    }
}
