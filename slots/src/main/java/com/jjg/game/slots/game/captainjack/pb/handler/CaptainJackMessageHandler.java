package com.jjg.game.slots.game.captainjack.pb.handler;

import com.alibaba.fastjson.JSONObject;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.slots.controller.SlotsRoomController;
import com.jjg.game.slots.game.captainjack.constant.CaptainJackConstant;
import com.jjg.game.slots.game.captainjack.data.CaptainJackGameRunInfo;
import com.jjg.game.slots.game.captainjack.manager.CaptainJackGameManager;
import com.jjg.game.slots.game.captainjack.manager.CaptainJackGameSendMessageManager;
import com.jjg.game.slots.game.captainjack.manager.CaptainJackRoomGameManager;
import com.jjg.game.slots.game.captainjack.pb.req.ReqCaptainJackEnterGame;
import com.jjg.game.slots.game.captainjack.pb.req.ReqCaptainJackPoolValue;
import com.jjg.game.slots.game.captainjack.pb.req.ReqCaptainJackStartGame;
import com.jjg.game.slots.game.captainjack.pb.req.ReqCaptainJackTreasureHunting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author 11
 * @date 2025/8/1 17:37
 */
@Component
@MessageType(MessageConst.MessageTypeDef.CAPTAIN_JACK)
public class CaptainJackMessageHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final CaptainJackGameManager gameManager;
    private final CaptainJackRoomGameManager roomGameManager;
    private final CaptainJackGameSendMessageManager sendMessageManager;

    public CaptainJackMessageHandler(CaptainJackGameManager gameManager, CaptainJackRoomGameManager roomGameManager, CaptainJackGameSendMessageManager sendMessageManager) {
        this.gameManager = gameManager;
        this.roomGameManager = roomGameManager;
        this.sendMessageManager = sendMessageManager;
    }

    /**
     * 请求配置信息
     *
     */
    @Command(CaptainJackConstant.MsgBean.REQ_CAPTAIN_JACK_ENTER_GAME)
    public void reqCaptainJackEnterGame(PlayerController playerController, ReqCaptainJackEnterGame req) {
        try {
            log.info("收到玩家请求配置 playerId={}", playerController.playerId());
            CaptainJackGameRunInfo gameRunInfo;
            if (playerController.getScene() == null) {
                gameRunInfo = gameManager.enterGame(playerController);
            } else if (playerController.getScene() instanceof SlotsRoomController) {
                gameRunInfo = roomGameManager.enterGame(playerController);
            } else {
                log.warn("playerController.getScene() is error, scene={}", playerController.getScene());
                return;
            }
            sendMessageManager.reqCaptainJackEnterGame(playerController,gameRunInfo);
        } catch (Exception e) {
            log.error("", e);
        }
    }

    /**
     * 开始游戏
     *
     */
    @Command(CaptainJackConstant.MsgBean.REQ_CAPTAIN_JACK_START_GAME)
    public void reqStartGame(PlayerController playerController, ReqCaptainJackStartGame req) {
        try {
            log.info("收到玩家开始游戏 playerId={},req={}", playerController.playerId(), JSONObject.toJSONString(req));
            CaptainJackGameRunInfo gameRunInfo;
            if (playerController.getScene() == null) {
                gameRunInfo = gameManager.playerStartGame(playerController,req.stakeValue);
            } else if (playerController.getScene() instanceof SlotsRoomController) {
                gameRunInfo = roomGameManager.playerStartGame(playerController,req.stakeValue);
            } else {
                log.warn("playerController.getScene() is error, scene={}", playerController.getScene());
                return;
            }
            sendMessageManager.reqCaptainJackStartGame(playerController, gameRunInfo);
        } catch (Exception e) {
            log.error("", e);
        }
    }
    /**
     * 奖池
     *
     * @param playerController
     * @param req
     */
    @Command(CaptainJackConstant.MsgBean.REQ_CAPTAIN_JACK_POOL_VALUE)
    public void reqCaptainJackPoolValue(PlayerController playerController, ReqCaptainJackPoolValue req) {
        try {
            CaptainJackGameRunInfo gameRunInfo;
            if (playerController.getScene() == null) {
                gameRunInfo = gameManager.getPoolValue(playerController,req.stakeValue);
            } else if (playerController.getScene() instanceof SlotsRoomController) {
                gameRunInfo = roomGameManager.getPoolValue(playerController,req.stakeValue);
            } else {
                log.warn("playerController.getScene() is error, scene={}", playerController.getScene());
                return;
            }
            sendMessageManager.sendPoolMessage(playerController, gameRunInfo);
        } catch (Exception e) {
            log.error("", e);
        }
    }

    /**
     * 请求探宝
     *
     */
    @Command(CaptainJackConstant.MsgBean.REQ_CAPTAIN_JACK_TREASURE_HUNTING)
    public void reqCaptainJackTreasureHunting(PlayerController playerController, ReqCaptainJackTreasureHunting req) {
        try {
            CaptainJackGameRunInfo gameRunInfo;
            if (playerController.getScene() == null) {
                gameRunInfo = gameManager.treasureHunting(playerController);
            } else if (playerController.getScene() instanceof SlotsRoomController) {
                gameRunInfo = roomGameManager.treasureHunting(playerController);
            } else {
                log.warn("playerController.getScene() is error, scene={}", playerController.getScene());
                return;
            }
            sendMessageManager.sendTreasureHunting(playerController, gameRunInfo);
        } catch (Exception e) {
            log.error("", e);
        }
    }

}
