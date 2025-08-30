package com.jjg.game.poker.game.blackjack.gamephase;

import com.jjg.game.poker.game.blackjack.constant.BlackJackConstant;
import com.jjg.game.poker.game.blackjack.data.BlackJackBuilder;
import com.jjg.game.poker.game.blackjack.data.BlackJackDataHelper;
import com.jjg.game.poker.game.blackjack.message.resp.NotifyBlackJackSendCardInfo;
import com.jjg.game.poker.game.blackjack.room.BlackJackGameController;
import com.jjg.game.poker.game.blackjack.room.data.BlackJackGameDataVo;
import com.jjg.game.poker.game.common.constant.PokerPhase;
import com.jjg.game.poker.game.common.data.PlayerSeatInfo;
import com.jjg.game.poker.game.common.data.PokerCard;
import com.jjg.game.poker.game.common.gamephase.BasePlayCardPhase;
import com.jjg.game.poker.game.common.gamephase.BaseWaitReadyPhase;
import com.jjg.game.room.controller.AbstractPhaseGameController;
import com.jjg.game.room.message.RoomMessageBuilder;
import com.jjg.game.sampledata.bean.Room_ChessCfg;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;


/**
 * @author lm
 * @date 2025/7/28 14:48
 */
public class BlackJackPlayCardPhase extends BasePlayCardPhase<BlackJackGameDataVo> {

    public BlackJackPlayCardPhase(AbstractPhaseGameController<Room_ChessCfg, BlackJackGameDataVo> gameController) {
        super(gameController);
    }


    @Override
    public void playCardPhaseDoAction() {
        if (gameController instanceof BlackJackGameController controller) {
            if (gameDataVo.getPlayerSeatInfoList().isEmpty()) {
                controller.addPokerPhase(new BaseWaitReadyPhase<>(controller));
                return;
            }
            Room_ChessCfg roomChessCfg = gameDataVo.getRoomCfg();
            //发牌
            int poolId = BlackJackDataHelper.getPoolId(gameDataVo);
            Map<Integer, PokerCard> cardListMap = BlackJackDataHelper.getCardListMap(poolId);
            int sendNum = sendCards(cardListMap, gameDataVo);
            //发庄家
            List<Integer> dealerCards = gameDataVo.getCards().subList(0, roomChessCfg.getHandPoker());
            List<Integer> cards = new ArrayList<>(dealerCards);
            dealerCards.clear();
            gameDataVo.setDealerCards(cards);
            //通知发牌信息
            NotifyBlackJackSendCardInfo notifyBlackJackSendCardInfo = new NotifyBlackJackSendCardInfo();
            notifyBlackJackSendCardInfo.cardIdList = BlackJackBuilder.getBlackJackCardInfoList(gameDataVo);
            //添加庄家能显示的牌
            PokerCard pokerCard = cardListMap.get(gameDataVo.getDealerCards().getFirst());
            notifyBlackJackSendCardInfo.cardId = pokerCard.getClientId();
            //天牌判断是否结算
            //设置第一个开始的玩家 并添加定时
            PlayerSeatInfo first = gameDataVo.getPlayerSeatInfoList().get(gameDataVo.getIndex());
            int totalPoint = BlackJackDataHelper.getTotalPoint(first.getCurrentCards());
            int sendCardNum = sendNum + roomChessCfg.getHandPoker();
            if (totalPoint == BlackJackConstant.Common.PERFECT_POINT) {
                PlayerSeatInfo nextExePlayer = controller.getNextExePlayer();
                if (Objects.isNull(nextExePlayer)) {
                    gameDataVo.setSettlementType(1);
                    gameDataVo.setShowDealer(false);
                    gameDataVo.setSettlementDelayTime(sendCardNum * BlackJackDataHelper.getExecutionTime(gameDataVo, PokerPhase.SEND_CARDS));
                    controller.startNextRoundOrSettlement();
                    return;
                }
                first = nextExePlayer;
            }
            //如果庄家的牌包含A通知购买ACE
            boolean canBuy = pokerCard.getRank() == 1;
            if (canBuy) {
                // 添加定时器并设置购买ACE结束时间
                controller.addACETimer(sendCardNum);
                gameDataVo.setAceBuyEndTime(gameDataVo.getPlayerTimerEvent().getNextTime());
            } else {
                controller.addNextTimer(first, sendCardNum);
                notifyBlackJackSendCardInfo.operationId = first.getPlayerId();
            }
            notifyBlackJackSendCardInfo.overTime = gameDataVo.getPlayerTimerEvent().getNextTime();
            gameDataVo.setCanBuyACE(canBuy);
            notifyBlackJackSendCardInfo.canBuyACE = canBuy;
            broadcastBuilderToRoom(RoomMessageBuilder.newBuilder().sendAllPlayer(notifyBlackJackSendCardInfo));
        }
    }

}
