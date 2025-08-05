package com.jjg.game.poker.game.common.message.handler;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.poker.game.common.BasePokerGameController;
import com.jjg.game.poker.game.common.BasePokerGameDataVo;
import com.jjg.game.poker.game.common.constant.PokerConstant;
import com.jjg.game.poker.game.common.message.req.ReqPokerRoomBaseInfo;
import com.jjg.game.poker.game.common.message.req.ReqSampleCardOperation;
import com.jjg.game.room.controller.AbstractGameController;
import com.jjg.game.room.data.room.GameDataVo;
import com.jjg.game.room.manager.RoomManager;
import com.jjg.game.room.sample.bean.RoomCfg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author lm
 * @date 2025/7/26 14:01
 */
@Component
@MessageType(value = MessageConst.MessageTypeDef.POKER_GENERAL_TYPE,isGroupMessage = true)
public class BasePokerHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());
    @Autowired
    private RoomManager roomManager;



    @Command(value = PokerConstant.MsgBean.REQ_SAMPLE_CARD_OPERATION)
    public void ReqSampleCardOperation(PlayerController playerController, ReqSampleCardOperation msg) {
        AbstractGameController<? extends RoomCfg, ? extends GameDataVo<? extends RoomCfg>> gameController =
                roomManager.getGameControllerByPlayerId(playerController.playerId());
        if (gameController instanceof BasePokerGameController<? extends BasePokerGameDataVo> basePokerGameController) {
            basePokerGameController.sampleCardOperation(playerController.playerId(), msg);
        }
    }


    @Command(value = PokerConstant.MsgBean.REQ_ROOM_BASE_INFO)
    public void reqRoomBaseInfo(PlayerController playerController, ReqPokerRoomBaseInfo msg) {
        AbstractGameController<? extends RoomCfg, ? extends GameDataVo<? extends RoomCfg>> gameController =
                roomManager.getGameControllerByPlayerId(playerController.playerId());
        if (gameController instanceof BasePokerGameController<? extends BasePokerGameDataVo> basePokerGameController) {
            basePokerGameController.respRoomInitInfo(playerController);
        }
    }
}
