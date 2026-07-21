package com.jjg.game.poker.game.douxian.message.handler;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.poker.game.douxian.constant.DouXianConstant;
import com.jjg.game.poker.game.douxian.message.req.ReqDouXianCancelHosting;
import com.jjg.game.poker.game.douxian.message.req.ReqDouXianConcede;
import com.jjg.game.poker.game.douxian.message.req.ReqDouXianConfirmPlay;
import com.jjg.game.poker.game.douxian.message.req.ReqDouXianDiscard;
import com.jjg.game.poker.game.douxian.message.req.ReqDouXianGoReady;
import com.jjg.game.poker.game.douxian.message.req.ReqDouXianPlaceCard;
import com.jjg.game.poker.game.douxian.message.req.ReqDouXianRecharge;
import com.jjg.game.poker.game.douxian.room.DouXianGameController;
import com.jjg.game.room.controller.AbstractGameController;
import com.jjg.game.room.data.room.GameDataVo;
import com.jjg.game.room.manager.RoomManager;
import com.jjg.game.sampledata.bean.RoomCfg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 斗仙牌消息分发入口：按 {@link DouXianConstant.MsgBean} 里的协议号把客户端请求路由到
 * {@link DouXianGameController} 对应的 reqXxx 方法上，具体业务逻辑不在这里处理。
 */
@Component
@MessageType(value = MessageConst.MessageTypeDef.DOU_XIAN_TYPE)
public class DouXianMessageHandler {

    private static final Logger log = LoggerFactory.getLogger(DouXianMessageHandler.class);

    private final RoomManager roomManager;

    public DouXianMessageHandler(RoomManager roomManager) {
        this.roomManager = roomManager;
    }

    /**
     * 根据玩家当前所在房间找到斗仙牌控制器；找不到(玩家不在斗仙牌房间/已离开)时打印告警并丢弃请求，
     * 避免客户端乱序/重复请求时空指针。
     */
    private DouXianGameController getController(long playerId) {
        AbstractGameController<? extends RoomCfg, ? extends GameDataVo<? extends RoomCfg>> gameController =
                roomManager.getGameControllerByPlayerId(playerId);
        if (gameController instanceof DouXianGameController controller) {
            return controller;
        }
        log.warn("斗仙牌消息找不到对应的房间控制器，丢弃请求 playerId:{}", playerId);
        return null;
    }

    @Command(value = DouXianConstant.MsgBean.REQ_DOU_XIAN_PLACE_CARD)
    public void reqPlaceCard(PlayerController playerController, ReqDouXianPlaceCard req) {
        DouXianGameController controller = getController(playerController.playerId());
        if (controller != null) {
            controller.reqPlaceCard(playerController.playerId(), req);
        }
    }

    @Command(value = DouXianConstant.MsgBean.REQ_DOU_XIAN_CONFIRM_PLAY)
    public void reqConfirmPlay(PlayerController playerController, ReqDouXianConfirmPlay req) {
        DouXianGameController controller = getController(playerController.playerId());
        if (controller != null) {
            controller.reqConfirmPlay(playerController.playerId(), req);
        }
    }

    @Command(value = DouXianConstant.MsgBean.REQ_DOU_XIAN_DISCARD)
    public void reqDiscard(PlayerController playerController, ReqDouXianDiscard req) {
        DouXianGameController controller = getController(playerController.playerId());
        if (controller != null) {
            controller.reqDiscard(playerController.playerId(), req);
        }
    }

    @Command(value = DouXianConstant.MsgBean.REQ_DOU_XIAN_GO_READY)
    public void reqGoReady(PlayerController playerController, ReqDouXianGoReady req) {
        DouXianGameController controller = getController(playerController.playerId());
        if (controller != null) {
            controller.reqGoReady(playerController.playerId(), req);
        }
    }

    @Command(value = DouXianConstant.MsgBean.REQ_DOU_XIAN_CANCEL_HOSTING)
    public void reqCancelHosting(PlayerController playerController, ReqDouXianCancelHosting req) {
        DouXianGameController controller = getController(playerController.playerId());
        if (controller != null) {
            controller.reqCancelHosting(playerController.playerId(), req);
        }
    }

    @Command(value = DouXianConstant.MsgBean.REQ_DOU_XIAN_RECHARGE)
    public void reqRecharge(PlayerController playerController, ReqDouXianRecharge req) {
        DouXianGameController controller = getController(playerController.playerId());
        if (controller != null) {
            controller.reqRecharge(playerController.playerId(), req);
        }
    }

    @Command(value = DouXianConstant.MsgBean.REQ_DOU_XIAN_CONCEDE)
    public void reqConcede(PlayerController playerController, ReqDouXianConcede req) {
        DouXianGameController controller = getController(playerController.playerId());
        if (controller != null) {
            controller.reqConcede(playerController.playerId(), req);
        }
    }
}
