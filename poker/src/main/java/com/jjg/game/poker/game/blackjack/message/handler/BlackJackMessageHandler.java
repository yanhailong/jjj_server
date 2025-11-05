package com.jjg.game.poker.game.blackjack.message.handler;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.poker.game.blackjack.constant.BlackJackConstant;
import com.jjg.game.poker.game.blackjack.message.req.ReqBlackJackContinuedDeposit;
import com.jjg.game.poker.game.blackjack.room.BlackJackGameController;
import com.jjg.game.room.controller.AbstractGameController;
import com.jjg.game.room.data.room.GameDataVo;
import com.jjg.game.room.manager.RoomManager;
import com.jjg.game.sampledata.bean.RoomCfg;
import org.springframework.stereotype.Component;

/**
 * @author lm
 * @date 2025/7/30 14:16
 */
@Component
@MessageType(value = MessageConst.MessageTypeDef.BLACK_JACK_TYPE)
public class BlackJackMessageHandler {

    private final RoomManager roomManager;

    public BlackJackMessageHandler(RoomManager roomManager) {
        this.roomManager = roomManager;
    }

    @Command(value = BlackJackConstant.MsgBean.REQ_BLACKJACK_CONTINUED_DEPOSIT)
    public void reqBlackJackContinuedDeposit(PlayerController playerController, ReqBlackJackContinuedDeposit req) {
        AbstractGameController<? extends RoomCfg, ? extends GameDataVo<? extends RoomCfg>> gameController =
                roomManager.getGameControllerByPlayerId(playerController.playerId());
        if (gameController instanceof BlackJackGameController controller) {
            controller.reqBlackJackContinuedDeposit(playerController.playerId(), req);
        }
    }

}
