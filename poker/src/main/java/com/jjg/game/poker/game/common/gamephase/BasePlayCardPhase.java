package com.jjg.game.poker.game.common.gamephase;

import com.jjg.game.poker.game.common.BasePokerGameDataVo;
import com.jjg.game.poker.game.common.data.PlayerSeatInfo;
import com.jjg.game.poker.game.common.data.PokerCard;
import com.jjg.game.poker.game.texas.data.TexasDataHelper;
import com.jjg.game.room.constant.EGamePhase;
import com.jjg.game.room.controller.AbstractGameController;
import com.jjg.game.room.sample.bean.Room_ChessCfg;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author lm
 * @date 2025/7/26 15:04
 */
public abstract class BasePlayCardPhase<T extends BasePokerGameDataVo> extends BasePokerPhase<T> {
    public BasePlayCardPhase(AbstractGameController<Room_ChessCfg, T> gameController) {
        super(gameController);
    }

    @Override
    public void phaseDoAction() {
        super.phaseDoAction();
    }


    @Override
    public void phaseFinish() {
    }

    @Override
    public int getPhaseRunTime() {
        return 150 * 1000;
    }

    @Override
    public EGamePhase getGamePhase() {
        return EGamePhase.PLAY_CART;
    }

    /**
     * 发牌
     */
    public int sendCards(Map<Integer, PokerCard> cardListMap, BasePokerGameDataVo gameDataVo) {
        gameDataVo.setCards(new ArrayList<>(cardListMap.keySet()));
        List<Integer> cards = gameDataVo.getCards();
        Collections.shuffle(cards);
        int sendNum = 0;
        //从第一个执行者开始发牌
        int handPoker = gameDataVo.getRoomCfg().getHandPoker();
        for (PlayerSeatInfo info : gameDataVo.getPlayerSeatInfoList()) {
            sendNum += handPoker;
            List<Integer> playCard = new ArrayList<>();
            for (int i = 0; i < handPoker; i++) {
                playCard.add(cards.remove(0));
            }
            info.setCards(new ArrayList<>());
            info.getCards().add(playCard);
        }
        return sendNum;
    }
}
