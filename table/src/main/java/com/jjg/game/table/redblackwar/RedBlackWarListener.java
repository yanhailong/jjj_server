package com.jjg.game.table.redblackwar;

import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.Card;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.listener.GmListener;
import com.jjg.game.room.controller.AbstractGameController;
import com.jjg.game.room.data.room.GameDataVo;
import com.jjg.game.room.manager.RoomManager;
import com.jjg.game.sampledata.bean.RoomCfg;
import com.jjg.game.table.redblackwar.room.manager.RedBlackWarRoomGameController;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 红黑大战gm
 *
 * @author lm
 * @date 2025/7/30 17:08
 */
@Component
public class RedBlackWarListener implements GmListener {

    @Autowired
    private RoomManager roomManager;

    @Override
    public CommonResult<String> gm(PlayerController playerController, String[] gmOrders) {
        String cmd = gmOrders[0];
        if ("setCard".equalsIgnoreCase(cmd)) {
            AbstractGameController<? extends RoomCfg, ? extends GameDataVo<? extends RoomCfg>> gameController =
                    roomManager.getGameControllerByPlayerId(playerController.playerId());
            if (gameController instanceof RedBlackWarRoomGameController controller) {
                //1,2;2,3;1,4+12;
                String cardValue = gmOrders[1];
                String[] cardArr = StringUtils.split(cardValue, "+");
                if (cardArr.length < 2) {
                    return new CommonResult<>(Code.FAIL);
                }
                String red = cardArr[0];
                String black = cardArr[1];
                List<Card> redCardList = getCardList(red);
                List<Card> blackCardList = getCardList(black);
                if (redCardList == null || blackCardList == null || redCardList.size() != 3 || blackCardList.size() != 3) {
                    return new CommonResult<>(Code.FAIL);
                }
                controller.getGameDataVo().setBlack(blackCardList);
                controller.getGameDataVo().setRed(redCardList);
                return new CommonResult<>(Code.SUCCESS);
            }

        }
        return new CommonResult<>(Code.FAIL);
    }

    private List<Card> getCardList(String red) {
        String[] cardList = StringUtils.split(red, ";");
        List<Card> redCards = new ArrayList<>(3);
        for (String card : cardList) {
            String[] split = StringUtils.split(card, ",");
            if (split.length != 2) {
                return null;
            }
            redCards.add(new Card(Integer.parseInt(split[0]), Integer.parseInt(split[1])));
        }
        return redCards;
    }
}
