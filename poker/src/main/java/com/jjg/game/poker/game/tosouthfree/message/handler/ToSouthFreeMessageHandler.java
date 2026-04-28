package com.jjg.game.poker.game.tosouthfree.message.handler;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.poker.game.tosouthfree.constant.ToSouthFreeConstant;
import com.jjg.game.poker.game.tosouthfree.message.req.ReqToSouthFreeChangeTable;
import com.jjg.game.poker.game.tosouthfree.message.req.ReqToSouthFreeGoReady;
import com.jjg.game.poker.game.tosouthfree.message.req.ReqToSouthFreeTurnAction;
import com.jjg.game.poker.game.tosouthfree.message.resp.RespToSouthFreeChangTable;
import com.jjg.game.poker.game.tosouthfree.room.ToSouthFreeGameController;
import com.jjg.game.room.controller.AbstractGameController;
import com.jjg.game.room.data.room.GameDataVo;
import com.jjg.game.room.manager.RoomManager;
import com.jjg.game.sampledata.bean.RoomCfg;
import org.springframework.stereotype.Component;

@Component
@MessageType(value = MessageConst.MessageTypeDef.TO_SOUTH_FREE)
public class ToSouthFreeMessageHandler {

    private final RoomManager roomManager;

    public ToSouthFreeMessageHandler(RoomManager roomManager) {
        this.roomManager = roomManager;
    }

    @Command(value = ToSouthFreeConstant.MsgBean.REQ_TURN_ACTION)
    public void reqTurnAction(PlayerController playerController, ReqToSouthFreeTurnAction reqTurnAction) {
        AbstractGameController<? extends RoomCfg, ? extends GameDataVo<? extends RoomCfg>> gameController =
                roomManager.getGameControllerByPlayerId(playerController.playerId());
        if (gameController instanceof ToSouthFreeGameController controller) {
            controller.turnAction(playerController.playerId(), reqTurnAction);
        }
    }

    @Command(value = ToSouthFreeConstant.MsgBean.REQ_CHANGE_TABLE)
    public void reqToSouthFreeChangeTable(PlayerController playerController, ReqToSouthFreeChangeTable changeTable) {
        AbstractGameController<? extends RoomCfg, ? extends GameDataVo<? extends RoomCfg>> gameController =
                roomManager.getGameControllerByPlayerId(playerController.playerId());
        boolean changed = false;
        if (gameController instanceof ToSouthFreeGameController controller) {
            changed = controller.reqChangeTable(playerController, controller);
        }
        RespToSouthFreeChangTable res = new RespToSouthFreeChangTable(changed ? Code.SUCCESS : Code.NO_VACANT_ROOM);
        playerController.send(res);
    }

    @Command(value = ToSouthFreeConstant.MsgBean.REQ_GO_READY)
    public void reqToSouthFreeGoReady(PlayerController playerController, ReqToSouthFreeGoReady req) {
        AbstractGameController<? extends RoomCfg, ? extends GameDataVo<? extends RoomCfg>> gameController =
                roomManager.getGameControllerByPlayerId(playerController.playerId());
        if (gameController instanceof ToSouthFreeGameController controller) {
            controller.reqToSouthFreeGoReady(playerController.playerId(), req);
        }
    }
}
