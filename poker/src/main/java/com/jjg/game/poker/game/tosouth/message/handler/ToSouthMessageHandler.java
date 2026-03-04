package com.jjg.game.poker.game.tosouth.message.handler;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.poker.game.texas.constant.TexasConstant;
import com.jjg.game.poker.game.texas.message.req.ReqTexasChangeTable;
import com.jjg.game.poker.game.texas.room.TexasGameController;
import com.jjg.game.poker.game.tosouth.constant.ToSouthConstant;
import com.jjg.game.poker.game.tosouth.message.req.ReqToSouthChangeTable;
import com.jjg.game.poker.game.tosouth.message.req.ReqTurnAction;
import com.jjg.game.poker.game.tosouth.room.ToSouthGameController;
import com.jjg.game.room.controller.AbstractGameController;
import com.jjg.game.room.data.room.GameDataVo;
import com.jjg.game.room.manager.RoomManager;
import com.jjg.game.sampledata.bean.RoomCfg;
import org.springframework.stereotype.Component;

@Component
@MessageType(value = MessageConst.MessageTypeDef.TO_SOUTH)
public class ToSouthMessageHandler {

    private final RoomManager roomManager;

    public ToSouthMessageHandler(RoomManager roomManager) {
        this.roomManager = roomManager;
    }

    @Command(value = ToSouthConstant.MsgBean.REQ_TURN_ACTION)
    public void reqTurnAction(PlayerController playerController, ReqTurnAction reqTurnAction) {
        AbstractGameController<? extends RoomCfg, ? extends GameDataVo<? extends RoomCfg>> gameController =
                roomManager.getGameControllerByPlayerId(playerController.playerId());
        if (gameController instanceof ToSouthGameController controller) {
            controller.turnAction(playerController.playerId(), reqTurnAction);
        }
    }

    @Command(value = ToSouthConstant.MsgBean.REQ_CHANGE_TABLE)
    public void reqToSouthChangeTable(PlayerController playerController, ReqToSouthChangeTable changeTable) {
        AbstractGameController<? extends RoomCfg, ? extends GameDataVo<? extends RoomCfg>> gameController =
                roomManager.getGameControllerByPlayerId(playerController.playerId());
        if (gameController instanceof ToSouthGameController controller) {
            controller.reqChangeTable(playerController, controller);
        }
    }
}
