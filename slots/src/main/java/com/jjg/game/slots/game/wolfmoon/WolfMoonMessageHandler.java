package com.jjg.game.slots.game.wolfmoon;

import com.alibaba.fastjson.JSONObject;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.slots.controller.SlotsRoomController;
import com.jjg.game.slots.game.wolfmoon.data.WolfMoonGameRunInfo;
import com.jjg.game.slots.game.wolfmoon.manager.WolfMoonGameManager;
import com.jjg.game.slots.game.wolfmoon.manager.WolfMoonRoomGameManager;
import com.jjg.game.slots.game.wolfmoon.manager.WolfMoonSendMessageManager;
import com.jjg.game.slots.game.wolfmoon.pb.ReqWolfMoonEnterGame;
import com.jjg.game.slots.game.wolfmoon.pb.ReqWolfMoonFreeChooseOne;
import com.jjg.game.slots.game.wolfmoon.pb.ReqWolfMoonStartGame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@MessageType(MessageConst.MessageTypeDef.WOLF_MOON)
public class WolfMoonMessageHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private WolfMoonGameManager gameManager;
    @Autowired
    private WolfMoonRoomGameManager roomGameManager;
    @Autowired
    private WolfMoonSendMessageManager sendMessageManager;

    @Command(WolfMoonConstant.MsgBean.REQ_CONFIG_INFO)
    public void reqConfigInfo(PlayerController playerController, ReqWolfMoonEnterGame req) {
        try {
            WolfMoonGameRunInfo gameRunInfo;
            if (playerController.getScene() == null) {
                gameRunInfo = gameManager.enterGame(playerController);
            } else if (playerController.getScene() instanceof SlotsRoomController) {
                gameRunInfo = roomGameManager.enterGame(playerController);
            } else {
                log.warn("playerController.getScene() is error, scene={}", playerController.getScene());
                return;
            }
            sendMessageManager.sendConfigMessage(playerController, gameRunInfo);
        } catch (Exception e) {
            log.error("", e);
        }
    }

    @Command(WolfMoonConstant.MsgBean.REQ_START_GAME)
    public void reqStartGame(PlayerController playerController, ReqWolfMoonStartGame req) {
        try {
            log.info("收到玩家开始游戏 playerId={},req={}", playerController.playerId(), JSONObject.toJSONString(req));
            WolfMoonGameRunInfo gameRunInfo;
            if (playerController.getScene() == null) {
                gameRunInfo = gameManager.playerStartGame(playerController, req.stakeVlue);
            } else if (playerController.getScene() instanceof SlotsRoomController) {
                gameRunInfo = roomGameManager.playerStartGame(playerController, req.stakeVlue);
            } else {
                log.warn("playerController.getScene() is error, scene={}", playerController.getScene());
                return;
            }
            sendMessageManager.sendStartGameMessage(playerController, gameRunInfo);
        } catch (Exception e) {
            log.error("", e);
        }
    }

    @Command(WolfMoonConstant.MsgBean.REQ_FREE_CHOOSE_ONE)
    public void reqFreeChooseOne(PlayerController playerController, ReqWolfMoonFreeChooseOne req) {
        try {
            WolfMoonGameRunInfo gameRunInfo;
            if (playerController.getScene() == null) {
                gameRunInfo = gameManager.freeChooseOne(playerController, req.type);
            } else if (playerController.getScene() instanceof SlotsRoomController) {
                gameRunInfo = roomGameManager.freeChooseOne(playerController, req.type);
            } else {
                log.warn("playerController.getScene() is error, scene={}", playerController.getScene());
                return;
            }
            sendMessageManager.sendFreeChooseOneMessage(playerController, gameRunInfo);
        } catch (Exception e) {
            log.error("", e);
        }
    }
}
