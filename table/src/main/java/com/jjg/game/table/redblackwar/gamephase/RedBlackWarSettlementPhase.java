package com.jjg.game.table.redblackwar.gamephase;

import com.jjg.game.common.utils.CommonUtil;
import com.jjg.game.core.data.Card;
import com.jjg.game.core.utils.PokerCardUtils;
import com.jjg.game.room.base.ERoomItemReason;
import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.room.data.room.SettlementData;
import com.jjg.game.room.data.room.TablePlayerGameData;
import com.jjg.game.room.datatrack.EDataTrackLogType;
import com.jjg.game.room.datatrack.SaveLogUtil;
import com.jjg.game.sampledata.bean.BetAreaCfg;
import com.jjg.game.sampledata.bean.WinPosWeightCfg;
import com.jjg.game.table.common.BaseTableGameController;
import com.jjg.game.table.common.gamephase.BaseSettlementPhase;
import com.jjg.game.table.common.message.TableMessageBuilder;
import com.jjg.game.table.redblackwar.constant.HandType;
import com.jjg.game.table.redblackwar.constant.RedBlackWarConstant;
import com.jjg.game.table.redblackwar.manager.RedBlackWarSampleManager;
import com.jjg.game.table.redblackwar.message.bean.RedBlackWarHistory;
import com.jjg.game.table.redblackwar.message.resp.NotifyRedBlackWarSettleInfo;
import com.jjg.game.table.redblackwar.room.data.RedBlackWarGameDataVo;
import com.jjg.game.table.redblackwar.util.CardComparatorUtil;
import org.apache.commons.collections4.keyvalue.DefaultKeyValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static com.jjg.game.table.redblackwar.constant.RedBlackWarConstant.Common.PAIR_MIN_LIMIT;

/**
 * 进入结算阶段
 *
 * @author 2CL
 */
public class RedBlackWarSettlementPhase extends BaseSettlementPhase<RedBlackWarGameDataVo> {

    private final RedBlackWarSampleManager redBlackWarSampleManager;
    private final Logger log = LoggerFactory.getLogger(getClass());

    public RedBlackWarSettlementPhase(BaseTableGameController<RedBlackWarGameDataVo> gameController) {
        super(gameController);
        redBlackWarSampleManager = CommonUtil.getContext().getBean(RedBlackWarSampleManager.class);
    }


    @Override
    public void phaseDoAction() {
        super.phaseDoAction();
        //根据牌型获得牌
        List<Integer> joker = PokerCardUtils.getPokerIntIdExceptJoker();
        Collections.shuffle(joker);
        //取红方的牌
        List<Card> redCard = joker.subList(0, 3).stream().map(Card::new).collect(Collectors.toList());
        if (Objects.nonNull(gameDataVo.getRed()) && gameDataVo.getRed().size() == 3) {
            redCard = gameDataVo.getRed();
            gameDataVo.setRed(null);
        }
        Card[] redCardArr = redCard.toArray(CardComparatorUtil.SAMPLE);
        //红方牌型
        HandType redHandType = CardComparatorUtil.getCardType(redCardArr);
        //取黑方的牌
        List<Card> blackCard = joker.subList(3, 6).stream().map(Card::new).collect(Collectors.toList());
        if (Objects.nonNull(gameDataVo.getBlack()) && gameDataVo.getBlack().size() == 3) {
            blackCard = gameDataVo.getBlack();
            gameDataVo.setBlack(null);
        }
        Card[] blackCardArr = blackCard.toArray(CardComparatorUtil.SAMPLE);
        //黑方牌型
        HandType blackHandType = CardComparatorUtil.getCardType(blackCardArr);
        //比较牌大小
        int result = CardComparatorUtil.compareCards(redHandType, redCardArr, blackHandType, blackCardArr);
        //押注信息
        Map<Integer, Map<Long, List<Integer>>> betInfo = gameDataVo.getBetInfo();
        boolean luckBet;
        Map<RedBlackWarConstant.Camp, Map<HandType, List<WinPosWeightCfg>>> winMap =
            redBlackWarSampleManager.getWinMap();
        Map<Long, DefaultKeyValue<Long, Long>> playerGet = new HashMap<>();
        List<WinPosWeightCfg> weightCfgList;
        if (result > 0) {
            //红方胜利
            luckBet = isLuckBet(redHandType, redCard);
            weightCfgList = winMap.get(RedBlackWarConstant.Camp.RED).get(redHandType);
        } else {
            //黑方胜利
            luckBet = isLuckBet(blackHandType, blackCard);
            weightCfgList = winMap.get(RedBlackWarConstant.Camp.BLACK).get(blackHandType);
        }
        // 庄家变化的钱
        long bankerChangeGold = 0;
        Map<Long, SettlementData> settlementDataMap = new HashMap<>();
        //遍历获奖位置
        for (WinPosWeightCfg cfg : weightCfgList) {
            List<Integer> betArea = cfg.getBetArea();
            BetAreaCfg betAreaCfg = redBlackWarSampleManager.getBetAreaMap().get(betArea.get(0));
            //押注区域非幸运一击
            boolean luckCfg = betAreaCfg.getAreaID() == RedBlackWarConstant.Common.LUCK_AREA;
            if (!luckBet && luckCfg) {
                continue;
            }
            //获取押注玩家
            Map<Long, List<Integer>> betMap = betInfo.get(betAreaCfg.getId());
            if (Objects.nonNull(betMap)) {
                //计算奖励
                for (Map.Entry<Long, List<Integer>> entry : betMap.entrySet()) {
                    Long playerId = entry.getKey();
                    int totalBet = entry.getValue().stream().mapToInt(Integer::intValue).sum();
                    GamePlayer gamePlayer = gameDataVo.getGamePlayer(playerId);
                    if (gamePlayer == null) {
                        continue;
                    }
                    //返还押分
                    long backBet = (long) totalBet * cfg.getReturnRate() / 10000;
                    //总获得
                    long canGet = backBet * cfg.getOdds() / 100;
                    if (cfg.getIsRatio() == 1) {
                        canGet = canGet * gameDataVo.getRoomCfg().getEffectiveRatio() / 10000;
                    }
                    // 房主收益，如果不是好友房此值为0
                    long roomCreatorIncome = calcRoomCreatorIncome(cfg, totalBet);
                    canGet += backBet - roomCreatorIncome;
                    // 给玩家添加金币
                    gameController.addItem(
                        gamePlayer.getId(), canGet,
                        ERoomItemReason.GAME_SETTLEMENT.withCfgId(gameDataVo.getRoomCfg().getId()));
                    DefaultKeyValue<Long, Long> keyValue = playerGet.computeIfAbsent(playerId,
                        key -> new DefaultKeyValue<>(0L, 0L));
                    keyValue.setKey(keyValue.getKey() + totalBet);
                    keyValue.setValue(keyValue.getValue() + canGet);
                    SettlementData settlementData =
                        new SettlementData(canGet - backBet, backBet, canGet, totalBet, roomCreatorIncome);
                    if (!settlementDataMap.containsKey(playerId)) {
                        settlementDataMap.put(playerId, settlementData);
                    } else {
                        settlementDataMap.get(playerId).increaseBySettlementData(settlementData);
                    }
                    bankerChangeGold +=
                        settlementDataMap.get(playerId).getTotalWin() - settlementDataMap.get(playerId).getBetTotal();
                }
            }
        }
        gameController.dealBankerFlowing(bankerChangeGold, settlementDataMap);
        //通知
        int winState = result > 0 ? 1 : 2;
        NotifyRedBlackWarSettleInfo settleInfo = new NotifyRedBlackWarSettleInfo();
        settleInfo.winState = winState;
        settleInfo.blackCards = blackCard.stream().map(Card::getValue).toList();
        settleInfo.blackCardType = blackHandType.getRank();
        settleInfo.redCards = redCard.stream().map(Card::getValue).toList();
        settleInfo.redCardType = redHandType.getRank();
        settleInfo.playerSettleInfos = TableMessageBuilder.getPlayerSettleInfos(playerGet, gameDataVo);
        settleInfo.isLucky = luckBet;
        //记录
        addLog(gameDataVo, playerGet, settleInfo.redCards, settleInfo.blackCards);
        //更新房间记录
        updateGameHistory(gameDataVo, blackHandType, winState);
        //清除押注历史
        betInfo.clear();
        //更新结算信息
        gameDataVo.setCurrentSettleInfo(settleInfo);
        //更新记录
        for (GamePlayer gamePlayer : gameDataVo.getGamePlayerMap().values()) {
            TablePlayerGameData tableGameData = gamePlayer.getTableGameData();
            DefaultKeyValue<Long, Long> keyValue = playerGet.get(gamePlayer.getId());
            long getGold = keyValue == null ? 0 : keyValue.getValue() - keyValue.getKey();
            tableGameData.addBetRecord(getGold);
        }
        //发送通知
        broadcastMsgToRoom(settleInfo);
    }

    @Override
    public void phaseFinishAction() {
        gameDataVo.setCurrentSettleInfo(null);
    }

    private void addLog(RedBlackWarGameDataVo gameDataVo, Map<Long, DefaultKeyValue<Long, Long>> playerGet,
                        List<Integer> redCard, List<Integer> blackCard) {
        SaveLogUtil.generalLog(gameDataVo.getPlayerBetInfo(), playerGet, gameDataVo.getGamePlayerMap(),
            gameController);
        gameDataTracker.addGameLogData("redCard", redCard);
        gameDataTracker.addGameLogData("blackCard", blackCard);
        gameDataTracker.flushDataLog(EDataTrackLogType.SETTLEMENT);
    }

    private void updateGameHistory(RedBlackWarGameDataVo gameDataVo, HandType blackHandType, int result) {
        RedBlackWarHistory redBlackWarHistory = new RedBlackWarHistory();
        redBlackWarHistory.cardType = blackHandType.getRank();
        redBlackWarHistory.winner = result;
        gameDataVo.addHistory(redBlackWarHistory);
    }

    /**
     * 判断是否是幸运一击
     */
    public boolean isLuckBet(HandType handType, List<Card> black) {
        if (handType == HandType.HIGH_CARD) {
            return false;
        }
        if (handType == HandType.PAIR) {
            black.sort(Comparator.comparingInt(Card::getRank));
            //取中间牌
            Card card = black.get(1);
            return card.compare(PAIR_MIN_LIMIT, false) >= 0;
        }
        return true;
    }
}
