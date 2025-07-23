package com.jjg.game.table.redblackwar.gamephase;

import com.jjg.game.common.utils.CommonUtil;
import com.jjg.game.core.utils.PokerCardUtils;
import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.room.data.room.TablePlayerGameData;
import com.jjg.game.room.message.RoomMessageBuilder;
import com.jjg.game.table.common.data.Card;
import com.jjg.game.table.common.gamephase.BaseSettlementPhase;
import com.jjg.game.table.common.message.TableMessageBuilder;
import com.jjg.game.table.redblackwar.constant.HandType;
import com.jjg.game.table.redblackwar.constant.RedBlackWarConstant;
import com.jjg.game.table.redblackwar.manager.RedBlackWarSampleManager;
import com.jjg.game.table.redblackwar.message.bean.RedBlackWarHistory;
import com.jjg.game.table.redblackwar.message.resp.NotifyRedBlackWarSettleInfo;
import com.jjg.game.table.redblackwar.room.data.RedBlackWarGameDataVo;
import com.jjg.game.table.redblackwar.room.manager.RedBlackWarRoomGameController;
import com.jjg.game.table.redblackwar.sample.bean.BetAreaCfg;
import com.jjg.game.table.redblackwar.sample.bean.WinPosWeightCfg;
import com.jjg.game.table.redblackwar.util.CardComparatorUtil;
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

    public RedBlackWarSettlementPhase(RedBlackWarRoomGameController gameController) {
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
        //红方牌型
        HandType redHandType = CardComparatorUtil.getCardType(redCard);
        //取黑方的牌
        List<Card> blackCard = joker.subList(3, 6).stream().map(Card::new).collect(Collectors.toList());
        //黑方牌型
        HandType blackHandType = CardComparatorUtil.getCardType(blackCard);
        //比较牌大小
        int result = redHandType.comper(blackHandType);
        if (result == 0) {
            result = CardComparatorUtil.compareCards(redCard.toArray(new Card[0]), blackCard.toArray(new Card[0]), redHandType);
        }
        //押注信息
        Map<Integer, Map<Long, Long>> betInfo = gameDataVo.getBetInfo();
        boolean luckBet;
        Map<RedBlackWarConstant.Camp, Map<HandType, List<WinPosWeightCfg>>> winMap = redBlackWarSampleManager.getWinMap();
        Map<Long, Long> playerGet = new HashMap<>();
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
            Map<Long, Long> betMap = betInfo.get(betAreaCfg.getId());
            if (Objects.nonNull(betMap)) {
                //计算奖励
                for (Map.Entry<Long, Long> entry : betMap.entrySet()) {
                    Long playerId = entry.getKey();
                    GamePlayer gamePlayer = gameDataVo.getGamePlayer(playerId);
                    if (gamePlayer == null) {
                        continue;
                    }
                    //返还押分
                    long backBet = entry.getValue() * cfg.getReturnRate() / 10000;
                    //总获得
                    long canGet = backBet * cfg.getOdds() / 100;
                    //TODO 抽税
                    if (!luckCfg) {
                        canGet = canGet * gameDataVo.getRoomCfg().getEffectiveRatio() / 10000;
                    }
                    canGet += backBet;
                    gamePlayer.setGold(canGet + gamePlayer.getGold());
                    playerGet.merge(playerId, canGet, Long::sum);
                }
            }
        }
        //通知
        int winState = result > 0 ? 1 : 2;
        NotifyRedBlackWarSettleInfo settleInfo = new NotifyRedBlackWarSettleInfo();
        settleInfo.winState = winState;
        settleInfo.blackCards = blackCard.stream().map(Card::getValue).toList();
        settleInfo.blackCardType = blackHandType.getRank();
        settleInfo.redCards = redCard.stream().map(Card::getValue).toList();
        settleInfo.redCardType = redHandType.getRank();
        settleInfo.playerSettleInfos = TableMessageBuilder.getPlayerSettleInfos(playerGet);
        //更新房间记录
        updateGameHistory(gameDataVo, blackHandType, winState);
        //清除押注历史
        betInfo.clear();
        //更新结算信息
        gameDataVo.setCurrentSettleInfo(settleInfo);
        //更新记录
        for (GamePlayer gamePlayer : gameDataVo.getGamePlayerMap().values()) {
            TablePlayerGameData tableGameData = gamePlayer.getTableGameData();
            long getGold = playerGet.getOrDefault(gamePlayer.getId(), 0L);
            tableGameData.addBetRecord(getGold);
        }
        //发送通知
        gameController.sendMessage(RoomMessageBuilder.newBuilder().setData(settleInfo));
    }

    @Override
    public void phaseFinish() {
        gameDataVo.setCurrentSettleInfo(null);
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
            return card.getRank() >= PAIR_MIN_LIMIT;
        }
        return true;
    }


}
