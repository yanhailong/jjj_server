package com.jjg.game.table.baccarat.gamephase;

import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.core.utils.PokerCardUtils;
import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.table.baccarat.BaccaratGameController;
import com.jjg.game.table.baccarat.data.BaccaratGameDataVo;
import com.jjg.game.table.common.gamephase.WaitReadyPhase;

import java.util.*;

/**
 * @author 2CL
 */
public class BaccaratWaitReadyPhase extends WaitReadyPhase<BaccaratGameDataVo> {

    public BaccaratWaitReadyPhase(BaccaratGameController gameController) {
        super(gameController);
    }

    @Override
    public void phaseDoAction() {
        super.phaseDoAction();
        // 清除每轮的场上临时数据
        gameDataVo.clearRoundData();
        // 如果牌只有6张直接全部洗牌
        if (gameDataVo.getCardList().size() < 6) {
            // 重新洗牌前需要删除场上记录的输赢路单数据
            gameDataVo.getBetRecord().clear();
            // 重新洗牌
            shuffleCard();
        }
    }

    /**
     * 随机卡牌
     */
    public void shuffleCard() {
        int cardsNeedRand = RandomUtils.randomMinMax(5, 7);
        int totalNum = cardsNeedRand * PokerCardUtils.ONE_POKERS_NUM;
        Set<Byte> pokerIdExceptJoker = PokerCardUtils.getPokerIdExceptJoker();
        // 洗牌
        ArrayList<Byte> pokerIds = new ArrayList<>(totalNum);
        for (int i = 0; i < cardsNeedRand; i++) {
            pokerIds.addAll(pokerIdExceptJoker);
        }
        Collections.shuffle(pokerIds);
        gameDataVo.setCardList(pokerIds);
        gameDataVo.setInitCardNum(pokerIds.size());
    }

    @Override
    public void phaseFinish() {
        super.phaseFinish();
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }
}
