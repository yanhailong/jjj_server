package com.jjg.game.poker.game.common.gamephase;

import com.jjg.game.core.data.Card;
import com.jjg.game.poker.game.common.BasePokerGameController;
import com.jjg.game.poker.game.common.BasePokerGameDataVo;
import com.jjg.game.poker.game.common.data.PlayerSeatInfo;
import com.jjg.game.poker.game.common.data.PokerCard;
import com.jjg.game.poker.game.common.data.PokerDataHelper;
import com.jjg.game.room.constant.EGamePhase;
import com.jjg.game.room.controller.AbstractPhaseGameController;
import com.jjg.game.sampledata.bean.Room_ChessCfg;

import java.util.*;

/**
 * @author lm
 * @date 2025/7/26 15:04
 */
public abstract class BasePlayCardPhase<T extends BasePokerGameDataVo> extends BasePokerPhase<T> {
    public BasePlayCardPhase(AbstractPhaseGameController<Room_ChessCfg, T> gameController) {
        super(gameController);
    }

    @Override
    public void phaseDoAction() {
        super.phaseDoAction();
        if (gameController instanceof BasePokerGameController<T>) {
            gameDataVo.setId(PokerDataHelper.getNextId());
            playCardPhaseDoAction();
        }

    }

    public abstract void playCardPhaseDoAction();


    @Override
    public void phaseFinish() {
    }

    @Override
    public int getPhaseRunTime() {
        return -1;
    }

    @Override
    public EGamePhase getGamePhase() {
        return EGamePhase.PLAY_CART;
    }

    public List<Integer> getCards(Map<Integer, PokerCard> cardListMap) {
        if (Objects.nonNull(gameDataVo.getTempCard())) {
            return new ArrayList<>(gameDataVo.getTempCard());
        }
        List<Integer> list = new ArrayList<>(cardListMap.keySet());
        Collections.shuffle(list);
        return list;
    }

    /**
     * 发牌
     */
    public int sendCards(Map<Integer, PokerCard> cardListMap, BasePokerGameDataVo gameDataVo) {
        gameDataVo.setCards(getCards(cardListMap));
        List<Integer> cards = gameDataVo.getCards();
        int sendNum = 0;
        //从第一个执行者开始发牌
        int handPoker = gameDataVo.getRoomCfg().getHandPoker();
        for (PlayerSeatInfo info : gameDataVo.getPlayerSeatInfoList()) {
            if (info.isDelState()) {
                continue;
            }
            sendNum += handPoker;
            List<Integer> playCard = new ArrayList<>();
            for (int i = 0; i < handPoker; i++) {
                playCard.add(cards.removeFirst());
            }
            info.setCards(new ArrayList<>());
            info.getCards().add(playCard);
        }
        return sendNum;
    }

    /**
     * 找到某张指定牌的拥有者
     * @param specifyCard
     * @return
     */
    public PlayerSeatInfo findSeatWithSpecifyCard(int specifyCard) {
        for (PlayerSeatInfo playerSeatInfo : gameDataVo.getPlayerSeatInfoList()) {
            for (Integer currentCard : playerSeatInfo.getCurrentCards()) {
                if (currentCard == specifyCard) {
                    return playerSeatInfo;
                }
            }
        }
        return null;
    }
}
