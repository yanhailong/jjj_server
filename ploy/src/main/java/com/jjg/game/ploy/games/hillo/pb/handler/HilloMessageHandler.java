package com.jjg.game.ploy.games.hillo.pb.handler;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.ploy.games.hillo.HilloController;
import com.jjg.game.ploy.games.hillo.data.HilloConstant;
import com.jjg.game.ploy.games.hillo.pb.req.ReqHilloAutoBet;
import com.jjg.game.ploy.games.hillo.pb.req.ReqHilloCancelAuto;
import com.jjg.game.ploy.games.hillo.pb.req.ReqHilloChoose;
import com.jjg.game.ploy.games.hillo.pb.req.ReqHilloExchange;
import com.jjg.game.ploy.games.hillo.pb.req.ReqHilloSkip;
import com.jjg.game.ploy.games.hillo.pb.res.ResHilloAutoBetStatus;
import com.jjg.game.ploy.games.hillo.pb.res.ResHilloChoose;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
@MessageType(MessageConst.MessageTypeDef.HILLO)
public class HilloMessageHandler {
    private static final Logger log = LoggerFactory.getLogger(HilloMessageHandler.class);
    private final HilloController hilloController;

    public HilloMessageHandler(HilloController hilloController) {
        this.hilloController = hilloController;
    }

    @Command(HilloConstant.MsgBean.REQ_HILLO_CHOOSE)
    public void reqHilloChoose(PlayerController playerController, ReqHilloChoose req) {
        handle(playerController, "choose", () -> hilloController.choose(playerController, req), () -> new ResHilloChoose(Code.FAIL));
    }

    @Command(HilloConstant.MsgBean.REQ_HILLO_EXCHANGE)
    public void reqHilloExchange(PlayerController playerController, ReqHilloExchange req) {
        handle(playerController, "exchange", () -> hilloController.exchange(playerController, req), () -> new ResHilloChoose(Code.FAIL));
    }

    @Command(HilloConstant.MsgBean.REQ_HILLO_SKIP)
    public void reqHilloSkip(PlayerController playerController, ReqHilloSkip req) {
        handle(playerController, "skip", () -> hilloController.skip(playerController, req), () -> new ResHilloChoose(Code.FAIL));
    }

    @Command(HilloConstant.MsgBean.REQ_HILLO_AUTO_BET)
    public void reqHilloAutoBet(PlayerController playerController, ReqHilloAutoBet req) {
        handle(playerController, "startAutoBet", () -> hilloController.startAutoBet(playerController, req), () -> new ResHilloAutoBetStatus(Code.FAIL));
    }

    @Command(HilloConstant.MsgBean.REQ_HILLO_CANCEL_AUTO)
    public void reqHilloCancelAuto(PlayerController playerController, ReqHilloCancelAuto req) {
        handle(playerController, "cancelAutoBet", () -> hilloController.cancelAutoBet(playerController, req), () -> new ResHilloAutoBetStatus(Code.FAIL));
    }

    private void handle(PlayerController playerController, String actionName,
                        Supplier<AbstractResponse> action, Supplier<AbstractResponse> failResponse) {
        try {
            AbstractResponse res = action.get();
            playerController.send(res != null ? res : failResponse.get());
        } catch (Exception e) {
            log.error("hilloController.{} error", actionName, e);
            playerController.send(failResponse.get());
        }
    }
}
