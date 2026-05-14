package com.jjg.game.ploy.games.hillo.pb.handler;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.ploy.games.hillo.HilloController;
import com.jjg.game.ploy.games.hillo.data.HilloConstant;
import com.jjg.game.ploy.games.hillo.pb.req.ReqHilloAutoBet;
import com.jjg.game.ploy.games.hillo.pb.req.ReqHilloCancelAuto;
import com.jjg.game.ploy.games.hillo.pb.req.ReqHilloChoose;
import com.jjg.game.ploy.games.hillo.pb.req.ReqHilloExchange;
import com.jjg.game.ploy.games.hillo.pb.req.ReqHilloSkip;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

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
        try {
            AbstractResponse res = hilloController.choose(playerController, req);
            if (res != null) {
                playerController.send(res);
            }
        } catch (Exception e) {
            log.error("hilloController.choose error", e);
        }
    }

    @Command(HilloConstant.MsgBean.REQ_HILLO_EXCHANGE)
    public void reqHilloExchange(PlayerController playerController, ReqHilloExchange req) {
        try {
            AbstractResponse res = hilloController.exchange(playerController, req);
            if (res != null) {
                playerController.send(res);
            }
        } catch (Exception e) {
            log.error("hilloController.exchange error", e);
        }
    }

    @Command(HilloConstant.MsgBean.REQ_HILLO_SKIP)
    public void reqHilloSkip(PlayerController playerController, ReqHilloSkip req) {
        try {
            AbstractResponse res = hilloController.skip(playerController, req);
            if (res != null) {
                playerController.send(res);
            }
        } catch (Exception e) {
            log.error("hilloController.skip error", e);
        }
    }

    @Command(HilloConstant.MsgBean.REQ_HILLO_AUTO_BET)
    public void reqHilloAutoBet(PlayerController playerController, ReqHilloAutoBet req) {
        try {
            AbstractResponse res = hilloController.startAutoBet(playerController, req);
            if (res != null) {
                playerController.send(res);
            }
        } catch (Exception e) {
            log.error("hilloController.startAutoBet error", e);
        }
    }

    @Command(HilloConstant.MsgBean.REQ_HILLO_CANCEL_AUTO)
    public void reqHilloCancelAuto(PlayerController playerController, ReqHilloCancelAuto req) {
        try {
            AbstractResponse res = hilloController.cancelAutoBet(playerController, req);
            if (res != null) {
                playerController.send(res);
            }
        } catch (Exception e) {
            log.error("hilloController.cancelAutoBet error", e);
        }
    }
}
