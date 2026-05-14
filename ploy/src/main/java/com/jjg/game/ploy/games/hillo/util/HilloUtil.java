package com.jjg.game.ploy.games.hillo.util;

import com.jjg.game.core.data.Card;
import com.jjg.game.ploy.games.hillo.data.HilloChoose;
import com.jjg.game.ploy.games.hillo.pb.bean.HilloChooseInfo;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class HilloUtil {
    public int randomCardId() {
        return ThreadLocalRandom.current().nextInt(1, 53);
    }

    public List<HilloChooseInfo> buildChooseInfos(int currentCardId, BigDecimal returnRate) {
        int currentRank = new Card(currentCardId).getRank();
        List<HilloChooseInfo> chooseInfos = new ArrayList<>(2);
        for (HilloChoose choose : HilloChoose.getValidChoices(currentCardId)) {
            chooseInfos.add(new HilloChooseInfo(
                    choose.getChooseId(),
                    choose.getChooseName(),
                    choose.calculateOdds(returnRate, currentRank),
                    choose.calculateWinRate(currentRank)
            ));
        }
        return chooseInfos;
    }
}
