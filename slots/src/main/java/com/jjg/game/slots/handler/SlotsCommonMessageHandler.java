package com.jjg.game.slots.handler;

import com.alibaba.fastjson.JSONObject;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.slots.constant.SlotsConst;
import com.jjg.game.slots.controller.SlotsRoomController;
import com.jjg.game.slots.manager.AbstractSlotsGameManager;
import com.jjg.game.slots.manager.SlotsFactoryManager;
import com.jjg.game.slots.pb.ReqSlotsRoomPool;
import com.jjg.game.slots.pb.ReqSlotsStatus;
import com.jjg.game.slots.pb.ResSlotsRoomPool;
import com.jjg.game.slots.manager.SlotsRoomManager;
import com.jjg.game.slots.pb.ResSlotsStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@MessageType(MessageConst.MessageTypeDef.SLOTS_COMMON)
public class SlotsCommonMessageHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private SlotsRoomManager slotsRoomManager;
    @Autowired
    private SlotsFactoryManager slotsFactoryManager;

    @Command(SlotsConst.SlotsCommon.REQ_SLOTS_ROOM_POOL)
    public void reqSlotsRoomPool(PlayerController playerController, ReqSlotsRoomPool req) {
        try {
            ResSlotsRoomPool res = new ResSlotsRoomPool(Code.SUCCESS);
            res.value = slotsRoomManager.getPoolValue(playerController.roomId());
            playerController.send(res);
        } catch (Exception e) {
            log.error("玩家退出房间异常 msg: {}", e.getMessage(), e);
        }
    }

    @Command(SlotsConst.SlotsCommon.REQ_SLOTS_STATUS)
    public void reqSlotsStatus(PlayerController playerController, ReqSlotsStatus req) {
        ResSlotsStatus res = new ResSlotsStatus(Code.SUCCESS);
        try {
            AbstractSlotsGameManager gameManager = slotsFactoryManager.getGameManager(playerController.getPlayer().getGameType(), playerController.getPlayer().getRoomCfgId());
            if (gameManager == null) {
                log.warn("gameManager is error, playerId = {},gameType = {},roomCfgId = {}", playerController.playerId(), playerController.getPlayer().getGameType(), playerController.getPlayer().getRoomCfgId());
                res.code = Code.FAIL;
                playerController.send(res);
                return;
            }
            res.code = gameManager.gameStatus(playerController);
        } catch (Exception e) {
            log.error("获取slots游戏状态异常 msg: {}", e.getMessage(), e);
            res.code = Code.EXCEPTION;
        }
        playerController.send(res);
    }
}
