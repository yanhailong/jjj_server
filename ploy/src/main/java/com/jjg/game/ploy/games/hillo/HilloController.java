package com.jjg.game.ploy.games.hillo;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSON;
import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.Pair;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.constant.GameConstant;
import com.jjg.game.core.data.Card;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.ExitType;
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
import com.jjg.game.ploy.games.hillo.pb.res.ResHilloAutoBetStatus;
import com.jjg.game.ploy.games.hillo.pb.res.ResHilloBet;
import com.jjg.game.ploy.games.hillo.pb.res.ResHilloChoose;
import com.jjg.game.ploy.games.hillo.pb.res.ResHilloEnterGame;
import com.jjg.game.ploy.games.hillo.pb.res.ResHilloRecord;
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
    // 自动投注策略阈值：胜率达到 60% 才主动猜，否则优先跳过。
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
        // HILLO 一次只能存在一局未结束游戏，避免重复下注覆盖当前公牌和奖励。
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
            // 重连或重新进入时恢复当前局状态，让前端继续展示可猜牌面、可兑现奖励和历史过程。
            res.currentCard = playerGameData.getCurrentCardId();
            res.currentCoin = playerGameData.getCurrentCoin();
            res.historyChoose = playerGameData.getHistory();
            res.remainRoundNum = HilloConstant.Common.MAX_JOIN_TIMES - playerGameData.getSuccessTimes();
            res.remainSkipTimes = playerGameData.getSkipTimes();
            res.currentBetMode = playerGameData.getCurrentBetMode();
            res.chooseInfos = hilloUtil.buildChooseInfos(playerGameData.getCurrentCardId(), getReturnRate(cfg));
        }
        if (playerGameData != null) {
            // 重新进入游戏时不恢复自动投注，自动节奏必须由客户端本次重新开启。
            if (playerGameData.isAutoBetting()) {
                playerGameData.clearAutoBet();
            }
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

        // 通用下注流程已经完成扣钱和入池，这里只负责初始化 HILLO 的本局数据。
        int currentCardId = hilloUtil.randomCardId();
        playerGameData.setCurrentCardId(currentCardId);
        playerGameData.setCurrentCoin(0);
        playerGameData.setSuccessTimes(0);
        playerGameData.setSkipTimes(HilloConstant.Common.MAX_SKIP_TIMES);
        playerGameData.setCurrentBetMode(value);
        playerGameData.setCurrentRoundStartTime(System.currentTimeMillis() / 1000);
        playerGameData.setHistory(null);

        res.currentCard = currentCardId;
        res.remainRoundNum = HilloConstant.Common.MAX_JOIN_TIMES;
        res.remainSkipTimes = playerGameData.getSkipTimes();
        res.chooseInfos = hilloUtil.buildChooseInfos(currentCardId, getReturnRate(cfg));
        if (value == HilloConstant.BetMode.AUTO) {
            return res;
        }
        return logBetAndReturn("下注开局", playerGameData.playerId(), res);
    }

    public AbstractResponse choose(PlayerController playerController, ReqHilloChoose req) {
        if (req.chooseId == HilloConstant.Common.AUTO_CHOOSE_ID) {
            return autoChoose(playerController);
        }

        ResHilloChoose res = new ResHilloChoose(Code.SUCCESS);
        HilloChoose hilloChoose = HilloChoose.getChoose(req.chooseId);
        if (hilloChoose == null) {
            res.code = Code.PARAM_ERROR;
            return logChooseAndReturn("手动猜牌", playerController.playerId(), res);
        }
        res.chooseId = req.chooseId;

        HilloPloyGameData playerGameData = getPlayerGameData(playerController.playerId());
        if (playerGameData == null || !playerGameData.hasActiveGame()) {
            res.code = Code.NOT_FOUND;
            return logChooseAndReturn("手动猜牌", playerController.playerId(), res);
        }

        PloygameRoomCfg cfg = GameDataManager.getPloygameRoomCfg(playerGameData.getRoomCfgId());
        if (cfg == null) {
            res.code = Code.SAMPLE_ERROR;
            return logChooseAndReturn("手动猜牌", playerController.playerId(), res);
        }

        BigDecimal returnRate = getReturnRate(cfg);
        List<HilloChooseInfo> chooseInfos = hilloUtil.buildChooseInfos(playerGameData.getCurrentCardId(), returnRate);
        HilloChooseInfo chooseInfo = chooseInfos.stream()
                .filter(info -> info.chooseId == req.chooseId)
                .findFirst()
                .orElse(null);
        if (chooseInfo == null) {
            res.code = Code.PARAM_ERROR;
            return logChooseAndReturn("手动猜牌", playerController.playerId(), res);
        }

        Card currentCard = new Card(playerGameData.getCurrentCardId());
        int nextCardId = hilloUtil.randomCardId();
        Card nextCard = new Card(nextCardId);

        if (!hilloChoose.check(currentCard, nextCard)) {
            // 猜错直接结束本局，只归档过程和亏损；不会从奖池派发奖励。
            playerGameData.addHistory(playerGameData.getCurrentCardId(), req.chooseId, chooseInfo.odd, nextCardId);
            fillHistoryChoose(res, playerGameData);
            settleAndArchive(playerGameData, 0, 0, getCurrentBalance(playerGameData));
            res.action = HilloConstant.AutoAction.ROUND_LOSE;
            res.nextCardId = nextCardId;
            fillAutoBetStatus(res, playerGameData);
            return logChooseAndReturn("手动猜牌", playerController.playerId(), res);
        }

        long nextCoin = calculateNextCoin(playerGameData.getLastBet(), playerGameData.getCurrentCoin(), chooseInfo.odd);
        int successTimes = playerGameData.getSuccessTimes() + 1;
        if (successTimes >= HilloConstant.Common.MAX_JOIN_TIMES) {
            // 达到单局最大猜测次数时强制兑现，避免一局无限累乘。
            CommonResult<Pair<PloyBetDivideInfo, Player>> winResult = winFromPool(playerGameData, nextCoin, cfg.getTaxRate());
            if (!winResult.success()) {
                res.code = winResult.code;
                return logChooseAndReturn("手动猜牌", playerController.playerId(), res);
            }
            long tax = winResult.data.getFirst().getTax();
            res.action = HilloConstant.AutoAction.EXCHANGE;
            res.nextCardId = nextCardId;
            res.currentCoin = nextCoin;
            res.exchangeNum = nextCoin - tax;
            playerGameData.addHistory(playerGameData.getCurrentCardId(), req.chooseId, chooseInfo.odd, nextCardId);
            fillHistoryChoose(res, playerGameData);
            settleAndArchive(playerGameData, tax, nextCoin, winResult.data.getSecond().getGold());
            fillAutoBetStatus(res, playerGameData);
            return logChooseAndReturn("手动猜牌", playerController.playerId(), res);
        }

        playerGameData.addHistory(playerGameData.getCurrentCardId(), req.chooseId, chooseInfo.odd, nextCardId);
        // 猜中后，结果牌变成下一轮要比较的公牌。
        playerGameData.setCurrentCardId(nextCardId);
        playerGameData.setCurrentCoin(nextCoin);
        playerGameData.setSuccessTimes(successTimes);

        res.action = HilloConstant.AutoAction.CHOOSE;
        res.nextCardId = nextCardId;
        res.currentCard = nextCardId;
        res.currentCoin = nextCoin;
        res.remainRoundNum = HilloConstant.Common.MAX_JOIN_TIMES - successTimes;
        res.remainSkipTimes = playerGameData.getSkipTimes();
        res.chooseInfos = hilloUtil.buildChooseInfos(nextCardId, returnRate);
        fillHistoryChoose(res, playerGameData);
        fillAutoBetStatus(res, playerGameData);
        return logChooseAndReturn("手动猜牌", playerController.playerId(), res);
    }

    public AbstractResponse exchange(PlayerController playerController, ReqHilloExchange req) {
        ResHilloChoose res = new ResHilloChoose(Code.SUCCESS);
        HilloPloyGameData playerGameData = getPlayerGameData(playerController.playerId());
        if (playerGameData == null || !playerGameData.hasActiveGame()) {
            res.code = Code.NOT_FOUND;
            return logChooseAndReturn("手动兑现", playerController.playerId(), res);
        }
        PloygameRoomCfg cfg = GameDataManager.getPloygameRoomCfg(playerGameData.getRoomCfgId());
        if (cfg == null) {
            res.code = Code.SAMPLE_ERROR;
            return logChooseAndReturn("手动兑现", playerController.playerId(), res);
        }
        if (playerGameData.getCurrentCoin() <= 0) {
            res.code = Code.ERROR_REQ;
            return logChooseAndReturn("手动兑现", playerController.playerId(), res);
        }

        // currentCoin 是含本金的可兑现奖励，派发时统一走奖池和税率逻辑。
        long settleCoin = playerGameData.getCurrentCoin();
        CommonResult<Pair<PloyBetDivideInfo, Player>> winResult = winFromPool(playerGameData, settleCoin, cfg.getTaxRate());
        if (!winResult.success()) {
            res.code = winResult.code;
            return logChooseAndReturn("手动兑现", playerController.playerId(), res);
        }
        long tax = winResult.data.getFirst().getTax();
        res.action = HilloConstant.AutoAction.EXCHANGE;
        res.currentCoin = settleCoin;
        res.exchangeNum = settleCoin - tax;
        fillHistoryChoose(res, playerGameData);
        settleAndArchive(playerGameData, tax, settleCoin, winResult.data.getSecond().getGold());
        fillAutoBetStatus(res, playerGameData);
        return logChooseAndReturn("手动兑现", playerController.playerId(), res);
    }

    public AbstractResponse skip(PlayerController playerController, ReqHilloSkip req) {
        ResHilloChoose res = new ResHilloChoose(Code.SUCCESS);
        HilloPloyGameData playerGameData = getPlayerGameData(playerController.playerId());
        if (playerGameData == null || !playerGameData.hasActiveGame()) {
            res.code = Code.NOT_FOUND;
            return logChooseAndReturn("手动跳过", playerController.playerId(), res);
        }
        // 起手牌（successTimes == 0）不允许跳过
        if (playerGameData.getSuccessTimes() <= 0) {
            res.code = Code.ERROR_REQ;
            return logChooseAndReturn("手动跳过", playerController.playerId(), res);
        }
        if (playerGameData.getSkipTimes() <= 0) {
            res.code = Code.ERROR_REQ;
            return logChooseAndReturn("手动跳过", playerController.playerId(), res);
        }
        PloygameRoomCfg cfg = GameDataManager.getPloygameRoomCfg(playerGameData.getRoomCfgId());
        if (cfg == null) {
            res.code = Code.SAMPLE_ERROR;
            return logChooseAndReturn("手动跳过", playerController.playerId(), res);
        }

        // 跳过会把当前公牌记入过程，然后从完整 52 张牌中重新发一张公牌。
        playerGameData.addSkipHistory(playerGameData.getCurrentCardId());
        int newCardId = hilloUtil.randomCardId();
        playerGameData.setCurrentCardId(newCardId);
        playerGameData.setSkipTimes(playerGameData.getSkipTimes() - 1);

        res.action = HilloConstant.AutoAction.SKIP;
        res.currentCard = newCardId;
        res.currentCoin = playerGameData.getCurrentCoin();
        res.remainSkipTimes = playerGameData.getSkipTimes();
        res.chooseInfos = hilloUtil.buildChooseInfos(newCardId, getReturnRate(cfg));
        fillHistoryChoose(res, playerGameData);
        fillAutoBetStatus(res, playerGameData);
        return logChooseAndReturn("手动跳过", playerController.playerId(), res);
    }

    public AbstractResponse startAutoBet(PlayerController playerController, ReqHilloAutoBet req) {
        ResHilloAutoBetStatus res = new ResHilloAutoBetStatus(Code.SUCCESS);
        HilloPloyGameData playerGameData = getPlayerGameData(playerController.playerId());
        if (playerGameData == null) {
            res.code = Code.NOT_FOUND;
            return logAutoBetStatusAndReturn("开启自动投注", playerController.playerId(), res);
        }
        if (req.betTimes < 0) {
            res.code = Code.PARAM_ERROR;
            return logAutoBetStatusAndReturn("开启自动投注", playerController.playerId(), res);
        }
        if (playerGameData.hasActiveGame() && playerGameData.getCurrentBetMode() != HilloConstant.BetMode.AUTO) {
            res.code = Code.ERROR_REQ;
            return logAutoBetStatusAndReturn("开启自动投注", playerController.playerId(), res);
        }

        // 保存自动投注配置。betTimes=0 表示无限局，因此需要额外的 autoInfiniteBet 标记区分“无限”和“剩余 0 局”。
        playerGameData.setAutoBetting(true);
        playerGameData.setAutoInfiniteBet(req.betTimes == 0);
        playerGameData.setAutoBet(req.bet);
        playerGameData.setAutoGuessTimes(normalizeAutoGuessTimes(req.guessTimes));
        playerGameData.setAutoRemainBetTimes(req.betTimes);

        PloygameRoomCfg cfg = GameDataManager.getPloygameRoomCfg(playerGameData.getRoomCfgId());
        if (cfg == null) {
            playerGameData.clearAutoBet();
            res.code = Code.SAMPLE_ERROR;
            return logAutoBetStatusAndReturn("开启自动投注", playerController.playerId(), res);
        }
        // 开启自动投注只保存配置；真正推进由前端定时发送 chooseId=-1 触发。
        res.autoBetting = true;
        return logAutoBetStatusAndReturn("开启自动投注", playerController.playerId(), res);
    }

    public AbstractResponse cancelAutoBet(PlayerController playerController, ReqHilloCancelAuto req) {
        ResHilloAutoBetStatus res = new ResHilloAutoBetStatus(Code.SUCCESS);
        HilloPloyGameData playerGameData = getPlayerGameData(playerController.playerId());
        if (playerGameData == null) {
            res.code = Code.NOT_FOUND;
            return logAutoBetStatusAndReturn("取消自动投注", playerController.playerId(), res);
        }

        PloygameRoomCfg cfg = GameDataManager.getPloygameRoomCfg(playerGameData.getRoomCfgId());
        if (cfg != null && playerGameData.hasActiveGame() && playerGameData.getCurrentCoin() > 0) {
            // 取消自动投注时，如果当前局已有可兑现奖励，先兑现再停止，避免丢失玩家已赢奖励。
            fillAutoExchangeResult(res, playerGameData, cfg);
        }
        playerGameData.clearAutoBet();
        res.autoBetting = false;
        return logAutoBetStatusAndReturn("取消自动投注", playerController.playerId(), res);
    }

    @Override
    public int getGameType() {
        return CoreConst.GameType.HILLO;
    }

    @Override
    public HilloPloyGameData exit(PlayerController playerController, ExitType exitType) {
        HilloPloyGameData playerGameData = getPlayerGameData(playerController.playerId());
        if (playerGameData != null && playerGameData.isAutoBetting()) {
            playerGameData.clearAutoBet();
            log.info("HILLO玩家退出清理自动投注 playerId={}, exitType={}", playerController.playerId(), exitType);
        }
        return super.exit(playerController, exitType);
    }

    private ResHilloChoose autoChoose(PlayerController playerController) {
        ResHilloChoose res = new ResHilloChoose(Code.SUCCESS);
        HilloPloyGameData playerGameData = getPlayerGameData(playerController.playerId());
        if (playerGameData == null) {
            res.code = Code.NOT_FOUND;
            return logChooseAndReturn("自动投注推进", playerController.playerId(), res);
        }
        // 自动模式由前端节奏驱动：每次 chooseId=-1 只推进一个自动动作。
        return logChooseAndReturn("自动投注推进", playerController.playerId(), runAutoStep(playerController, playerGameData));
    }

    private ResHilloChoose runAutoStep(PlayerController playerController, HilloPloyGameData playerGameData) {
        ResHilloChoose res = new ResHilloChoose(Code.SUCCESS);
        PloygameRoomCfg cfg = GameDataManager.getPloygameRoomCfg(playerGameData.getRoomCfgId());
        if (cfg == null) {
            res.code = Code.SAMPLE_ERROR;
            stopAutoBet(playerGameData);
            return res;
        }
        if (!playerGameData.isAutoBetting()) {
            res.code = Code.ERROR_REQ;
            fillAutoState(res, playerGameData, cfg);
            return res;
        }

        if (!playerGameData.hasActiveGame()) {
            if (!playerGameData.isAutoInfiniteBet() && playerGameData.getAutoRemainBetTimes() <= 0) {
                // 有限局数已经跑完，自动投注自然停止。
                stopAutoBet(playerGameData);
                res.action = HilloConstant.AutoAction.STOP;
                fillAutoState(res, playerGameData, cfg);
                return res;
            }
            ResHilloChoose startRes = startAutoRound(playerController, playerGameData, cfg);
            return startRes;
        }

        if (playerGameData.getCurrentCoin() > 0 && playerGameData.getSuccessTimes() >= playerGameData.getAutoGuessTimes()) {
            fillAutoExchangeResult(res, playerGameData, cfg);
            fillAutoState(res, playerGameData, cfg);
            return res;
        }

        AutoDecision decision = buildAutoDecision(playerGameData, cfg);
        if (decision.skip()) {
            // 两个投注区域胜率都低于阈值时，自动策略选择跳过当前公牌。
            fillAutoSkipResult(res, playerGameData, cfg);
        } else {
            fillAutoChooseResult(res, playerGameData, cfg, decision.chooseId());
        }
        stopAutoIfNoMoreRounds(playerGameData);
        fillAutoState(res, playerGameData, cfg);
        return res;
    }

    private ResHilloChoose startAutoRound(PlayerController playerController, HilloPloyGameData playerGameData, PloygameRoomCfg cfg) {
        // 自动开局复用通用下注流程，保证扣钱、入池、下注上下限校验与手动一致。
        AbstractMessage betMessage = bet(playerGameData, playerGameData.getAutoBet(), HilloConstant.BetMode.AUTO);
        ResHilloChoose res = new ResHilloChoose(betMessage instanceof AbstractResponse response ? response.code : Code.FAIL);
        if (res.code != Code.SUCCESS || !(betMessage instanceof ResHilloBet betRes)) {
            stopAutoBet(playerGameData);
            res.action = HilloConstant.AutoAction.STOP;
            fillAutoState(res, playerGameData, cfg);
            return res;
        }

        // 有限局自动投注在成功开局时扣减剩余局数；无限局 autoRemainBetTimes 始终为 0。
        playerGameData.decreaseAutoRemainBetTimes();
        res.action = HilloConstant.AutoAction.START_ROUND;
        res.currentCard = betRes.currentCard;
        res.remainRoundNum = betRes.remainRoundNum;
        res.remainSkipTimes = betRes.remainSkipTimes;
        res.chooseInfos = betRes.chooseInfos;
        fillAutoState(res, playerGameData, cfg);
        return res;
    }

    private void fillAutoChooseResult(ResHilloChoose res, HilloPloyGameData playerGameData, PloygameRoomCfg cfg, int chooseId) {
        BigDecimal returnRate = getReturnRate(cfg);
        HilloChoose hilloChoose = HilloChoose.getChoose(chooseId);
        HilloChooseInfo chooseInfo = hilloUtil.buildChooseInfos(playerGameData.getCurrentCardId(), returnRate).stream()
                .filter(info -> info.chooseId == chooseId)
                .findFirst()
                .orElse(null);
        if (hilloChoose == null || chooseInfo == null) {
            res.code = Code.PARAM_ERROR;
            stopAutoBet(playerGameData);
            return;
        }

        Card currentCard = new Card(playerGameData.getCurrentCardId());
        int nextCardId = hilloUtil.randomCardId();
        Card nextCard = new Card(nextCardId);
        res.chooseId = chooseId;
        res.nextCardId = nextCardId;

        if (!hilloChoose.check(currentCard, nextCard)) {
            // 自动投注猜错后本局结束，但自动配置保留，下一次自动推进会尝试开下一局。
            playerGameData.addHistory(playerGameData.getCurrentCardId(), chooseId, chooseInfo.odd, nextCardId);
            fillHistoryChoose(res, playerGameData);
            settleAndArchive(playerGameData, 0, 0, getCurrentBalance(playerGameData));
            res.action = HilloConstant.AutoAction.ROUND_LOSE;
            return;
        }

        long nextCoin = calculateNextCoin(playerGameData.getLastBet(), playerGameData.getCurrentCoin(), chooseInfo.odd);
        int successTimes = playerGameData.getSuccessTimes() + 1;
        if (successTimes >= HilloConstant.Common.MAX_JOIN_TIMES) {
            // 猜中后如果达到系统上限，立即兑现并归档本局。
            CommonResult<Pair<PloyBetDivideInfo, Player>> winResult = winFromPool(playerGameData, nextCoin, cfg.getTaxRate());
            if (!winResult.success()) {
                res.code = winResult.code;
                stopAutoBet(playerGameData);
                return;
            }
            long tax = winResult.data.getFirst().getTax();
            res.action = HilloConstant.AutoAction.EXCHANGE;
            res.currentCoin = nextCoin;
            res.exchangeNum = nextCoin - tax;
            playerGameData.addHistory(playerGameData.getCurrentCardId(), chooseId, chooseInfo.odd, nextCardId);
            fillHistoryChoose(res, playerGameData);
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
        fillHistoryChoose(res, playerGameData);
    }

    private void fillAutoSkipResult(ResHilloChoose res, HilloPloyGameData playerGameData, PloygameRoomCfg cfg) {
        playerGameData.addSkipHistory(playerGameData.getCurrentCardId());
        int newCardId = hilloUtil.randomCardId();
        playerGameData.setCurrentCardId(newCardId);
        playerGameData.setSkipTimes(playerGameData.getSkipTimes() - 1);

        res.action = HilloConstant.AutoAction.SKIP;
        res.currentCard = newCardId;
        res.currentCoin = playerGameData.getCurrentCoin();
        res.remainSkipTimes = playerGameData.getSkipTimes();
        res.chooseInfos = hilloUtil.buildChooseInfos(newCardId, getReturnRate(cfg));
        fillHistoryChoose(res, playerGameData);
    }

    private void fillAutoExchangeResult(ResHilloChoose res, HilloPloyGameData playerGameData, PloygameRoomCfg cfg) {
        long settleCoin = playerGameData.getCurrentCoin();
        CommonResult<Pair<PloyBetDivideInfo, Player>> winResult = winFromPool(playerGameData, settleCoin, cfg.getTaxRate());
        if (!winResult.success()) {
            res.code = winResult.code;
            playerGameData.clearAutoBet();
            return;
        }
        long tax = winResult.data.getFirst().getTax();
        res.action = HilloConstant.AutoAction.EXCHANGE;
        res.currentCoin = settleCoin;
        res.exchangeNum = settleCoin - tax;
        fillHistoryChoose(res, playerGameData);
        settleAndArchive(playerGameData, tax, settleCoin, winResult.data.getSecond().getGold());
    }

    private void fillAutoExchangeResult(ResHilloAutoBetStatus res, HilloPloyGameData playerGameData, PloygameRoomCfg cfg) {
        long settleCoin = playerGameData.getCurrentCoin();
        CommonResult<Pair<PloyBetDivideInfo, Player>> winResult = winFromPool(playerGameData, settleCoin, cfg.getTaxRate());
        if (!winResult.success()) {
            res.code = winResult.code;
            playerGameData.clearAutoBet();
            return;
        }
        long tax = winResult.data.getFirst().getTax();
        settleAndArchive(playerGameData, tax, settleCoin, winResult.data.getSecond().getGold());
    }

    private void fillHistoryChoose(ResHilloChoose res, HilloPloyGameData playerGameData) {
        List<HilloHistoryInfo> history = playerGameData.getHistory();
        if (CollectionUtil.isNotEmpty(history)) {
            res.historyChoose = new ArrayList<>(history);
        }
    }

    private void fillAutoBetStatus(ResHilloChoose res, HilloPloyGameData playerGameData) {
        res.autoBetting = playerGameData.isAutoBetting();
        res.autoInfiniteBet = playerGameData.isAutoInfiniteBet();
        res.remainBetTimes = playerGameData.getAutoRemainBetTimes();
    }

    private ResHilloBet logBetAndReturn(String actionName, long playerId, ResHilloBet res) {
        log.info("HILLO玩家操作 actionName={}, playerId={}, code={}, res={}",
                actionName, playerId, res.code, JSON.toJSONString(res));
        return res;
    }

    private ResHilloChoose logChooseAndReturn(String actionName, long playerId, ResHilloChoose res) {
        log.info("HILLO玩家操作 actionName={}, playerId={}, code={}, action={}, res={}",
                actionName, playerId, res.code, res.action, JSON.toJSONString(res));
        return res;
    }

    private ResHilloAutoBetStatus logAutoBetStatusAndReturn(String actionName, long playerId, ResHilloAutoBetStatus res) {
        log.info("HILLO玩家操作 actionName={}, playerId={}, code={}, autoBetting={}, res={}",
                actionName, playerId, res.code, res.autoBetting, JSON.toJSONString(res));
        return res;
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
        // 文档要求胜率达到 60% 才自动猜；首轮不能跳过，所以首轮无论胜率如何都选最优区域。
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

    private void fillAutoState(ResHilloChoose res, HilloPloyGameData playerGameData, PloygameRoomCfg cfg) {
        // 自动响应统一补齐当前状态，前端可直接按这一份数据刷新界面。
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

    private void stopAutoBet(HilloPloyGameData playerGameData) {
        playerGameData.clearAutoBet();
    }

    private void stopAutoIfNoMoreRounds(HilloPloyGameData playerGameData) {
        if (!playerGameData.hasActiveGame() && !playerGameData.isAutoInfiniteBet() && playerGameData.getAutoRemainBetTimes() <= 0) {
            stopAutoBet(playerGameData);
        }
    }

    private record AutoDecision(int chooseId, boolean skip) {
    }

    private BigDecimal getReturnRate(PloygameRoomCfg cfg) {
        return BigDecimal.valueOf(cfg.getRewardRate()).divide(GameConstant.TEN_THOUSAND_BD, 4, RoundingMode.DOWN);
    }

    private long calculateNextCoin(long lastBet, long currentCoin, String odd) {
        // 奖励采用“上一轮可兑金额 x 本轮赔率”的连乘方式；第一轮基数为下注额。
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
        // 猜错归档需要记录余额，优先实时读取玩家金币；下注缓存可能因为重启而丢失。
        Player player = playerService.get(playerGameData.playerId());
        if (player != null) {
            return player.getGold();
        }
        PloyBetDivideInfo divideInfo = playerGameData.getPloyBetDivideInfo();
        return divideInfo != null ? divideInfo.getPlayerAfterMoney() : 0;
    }

    private void settleAndArchive(HilloPloyGameData playerGameData, long tax, long settleCoin, long balanceAfter) {
        // 本局结束时归档历史，并清空当前局状态；下一次下注会重新发起手牌。
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
