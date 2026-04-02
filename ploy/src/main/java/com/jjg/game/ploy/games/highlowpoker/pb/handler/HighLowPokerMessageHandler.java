package com.jjg.game.ploy.games.highlowpoker.pb.handler;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.ploy.games.highlowpoker.HighLowPokerController;
import com.jjg.game.ploy.games.highlowpoker.data.HighLowPokerConstant;
import com.jjg.game.ploy.games.highlowpoker.pb.req.ReqHighLowPokerChoose;
import com.jjg.game.ploy.games.highlowpoker.pb.req.ReqHighLowPokerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author lm
 * @date 2026/4/1 13:42
 */
@Component
@MessageType(MessageConst.MessageTypeDef.HIGH_LOW_POKER)
public class HighLowPokerMessageHandler {
    private static final Logger log = LoggerFactory.getLogger(HighLowPokerMessageHandler.class);
    private final HighLowPokerController highLowPokerController;

    public HighLowPokerMessageHandler(HighLowPokerController highLowPokerController) {
        this.highLowPokerController = highLowPokerController;
    }

    /**
     * 请求发牌
     *
     */
    @Command(HighLowPokerConstant.MsgBean.REQ_HIGH_LOW_POKER_CHOOSE)
    public void reqHighLowPokerChoose(PlayerController playerController, ReqHighLowPokerChoose req) {
        try {
            AbstractResponse res = highLowPokerController.choose(playerController, req);
            if (res != null) {
                playerController.send(res);
            }
        } catch (Exception e) {
            log.error("highLowPokerController.choose error", e);
        }
    }

    /**
     * 请求兑换
     *
     */
    @Command(HighLowPokerConstant.MsgBean.REQ_HIGH_LOW_POKER_EXCHANGE)
    public void reqHighLowPokerExchange(PlayerController playerController, ReqHighLowPokerExchange req) {
        try {
            AbstractResponse res = highLowPokerController.exchange(playerController, req);
            if (res != null) {
                playerController.send(res);
            }
        } catch (Exception e) {
            log.error("highLowPokerController.exchange error", e);
        }
    }

}
