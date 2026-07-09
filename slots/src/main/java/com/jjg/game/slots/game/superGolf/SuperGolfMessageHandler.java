package com.jjg.game.slots.game.superGolf;

import com.alibaba.fastjson.JSONObject;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.slots.controller.SlotsRoomController;
import com.jjg.game.slots.game.superGolf.data.SuperGolfGameRunInfo;
import com.jjg.game.slots.game.superGolf.manager.SuperGolfGameManager;
import com.jjg.game.slots.game.superGolf.manager.SuperGolfRoomGameManager;
import com.jjg.game.slots.game.superGolf.manager.SuperGolfSendMessageManager;
import com.jjg.game.slots.game.superGolf.pb.ReqSuperGolfEnterGame;
import com.jjg.game.slots.game.superGolf.pb.ReqSuperGolfPoolValue;
import com.jjg.game.slots.game.superGolf.pb.ReqSuperGolfStartGame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@MessageType(MessageConst.MessageTypeDef.SUPER_GOLF_TYPE)
public class SuperGolfMessageHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private SuperGolfGameManager gameManager;
    @Autowired
    private SuperGolfRoomGameManager roomGameManager;
    @Autowired
    private SuperGolfSendMessageManager sendMessageManager;

    @Command(SuperGolfConstant.MsgBean.REQ_CONFIG_INFO)
    public void reqConfigInfo(PlayerController playerController, ReqSuperGolfEnterGame req) {
        try {
            log.info("收到玩家请求配置 playerId={}", playerController.playerId());
            SuperGolfGameRunInfo gameRunInfo;
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

    @Command(SuperGolfConstant.MsgBean.REQ_START_GAME)
    public void reqStartGame(PlayerController playerController, ReqSuperGolfStartGame req) {
        try {
            log.info("收到玩家开始游戏 playerId={},req={}", playerController.playerId(), JSONObject.toJSONString(req));
            SuperGolfGameRunInfo gameRunInfo;
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

    @Command(SuperGolfConstant.MsgBean.REQ_POOL_INFO)
    public void reqPoolValue(PlayerController playerController, ReqSuperGolfPoolValue req) {
        try {
            log.info("收到玩家奖池请求 playerId={},req={}", playerController.playerId(), JSONObject.toJSONString(req));
            SuperGolfGameRunInfo gameRunInfo;
            if (playerController.getScene() == null) {
                gameRunInfo = gameManager.getPoolValue(playerController, req.stakeVlue);
            } else if (playerController.getScene() instanceof SlotsRoomController) {
                gameRunInfo = roomGameManager.getPoolValue(playerController, req.stakeVlue);
            } else {
                log.warn("playerController.getScene() is error, scene={}", playerController.getScene());
                return;
            }
            sendMessageManager.sendPoolValue(playerController, gameRunInfo);
        } catch (Exception e) {
            log.error("", e);
        }
    }
}
