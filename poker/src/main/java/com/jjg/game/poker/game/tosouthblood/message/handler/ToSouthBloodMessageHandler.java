package com.jjg.game.poker.game.tosouthblood.message.handler;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.poker.game.tosouthblood.constant.ToSouthBloodConstant;
import com.jjg.game.poker.game.tosouthblood.message.req.ReqToSouthBloodChangeTable;
import com.jjg.game.poker.game.tosouthblood.message.req.ReqToSouthBloodGoReady;
import com.jjg.game.poker.game.tosouthblood.message.req.ReqTurnAction;
import com.jjg.game.poker.game.tosouthblood.room.ToSouthBloodGameController;
import com.jjg.game.room.controller.AbstractGameController;
import com.jjg.game.room.data.room.GameDataVo;
import com.jjg.game.room.manager.RoomManager;
import com.jjg.game.sampledata.bean.RoomCfg;
import org.springframework.stereotype.Component;

@Component
@MessageType(value = MessageConst.MessageTypeDef.TO_SOUTH_BLOOD)
public class ToSouthBloodMessageHandler {

    private final RoomManager roomManager;

    public ToSouthBloodMessageHandler(RoomManager roomManager) {
        this.roomManager = roomManager;
    }

    @Command(value = ToSouthBloodConstant.MsgBean.REQ_TURN_ACTION)
    public void reqTurnAction(PlayerController playerController, ReqTurnAction reqTurnAction) {
        AbstractGameController<? extends RoomCfg, ? extends GameDataVo<? extends RoomCfg>> gameController =
                roomManager.getGameControllerByPlayerId(playerController.playerId());
        if (gameController instanceof ToSouthBloodGameController controller) {
            controller.turnAction(playerController.playerId(), reqTurnAction);
        }
    }

    @Command(value = ToSouthBloodConstant.MsgBean.REQ_CHANGE_TABLE)
    public void reqToSouthBloodChangeTable(PlayerController playerController, ReqToSouthBloodChangeTable changeTable) {
        AbstractGameController<? extends RoomCfg, ? extends GameDataVo<? extends RoomCfg>> gameController =
                roomManager.getGameControllerByPlayerId(playerController.playerId());
        if (gameController instanceof ToSouthBloodGameController controller) {
            controller.reqChangeTable(playerController, controller);
        }
    }

    @Command(value = ToSouthBloodConstant.MsgBean.REQ_GO_READY)
    public void reqToSouthBloodGoReady(PlayerController playerController, ReqToSouthBloodGoReady req) {
        AbstractGameController<? extends RoomCfg, ? extends GameDataVo<? extends RoomCfg>> gameController =
                roomManager.getGameControllerByPlayerId(playerController.playerId());
        if (gameController instanceof ToSouthBloodGameController controller) {
            controller.reqToSouthBloodGoReady(playerController.playerId(), req);
        }
    }
}
