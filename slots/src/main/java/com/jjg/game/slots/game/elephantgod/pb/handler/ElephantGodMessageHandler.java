package com.jjg.game.slots.game.elephantgod.pb.handler;

import com.alibaba.fastjson.JSONObject;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.slots.controller.SlotsRoomController;
import com.jjg.game.slots.game.captainjack.constant.CaptainJackConstant;
import com.jjg.game.slots.game.captainjack.data.CaptainJackGameRunInfo;
import com.jjg.game.slots.game.captainjack.pb.req.ReqCaptainJackPoolValue;
import com.jjg.game.slots.game.elephantgod.ElephantGodConstant;
import com.jjg.game.slots.game.elephantgod.data.ElephantGodGameRunInfo;
import com.jjg.game.slots.game.elephantgod.manager.ElephantGodGameManager;
import com.jjg.game.slots.game.elephantgod.manager.ElephantGodRoomGameManager;
import com.jjg.game.slots.game.elephantgod.manager.ElephantGodSendMessageManager;
import com.jjg.game.slots.game.elephantgod.pb.req.ReqElephantGodEnterGame;
import com.jjg.game.slots.game.elephantgod.pb.req.ReqElephantGodPoolValue;
import com.jjg.game.slots.game.elephantgod.pb.req.ReqElephantGodStartGame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@MessageType(MessageConst.MessageTypeDef.ELEPHANT_GOD)
public class ElephantGodMessageHandler {
    private Logger log = LoggerFactory.getLogger(getClass());

    private final ElephantGodGameManager gameManager;
    private final ElephantGodRoomGameManager roomGameManager;
    private final ElephantGodSendMessageManager sendMessageManager;

    public ElephantGodMessageHandler(ElephantGodGameManager gameManager, ElephantGodRoomGameManager roomGameManager,
                                     ElephantGodSendMessageManager sendMessageManager) {
        this.gameManager = gameManager;
        this.roomGameManager = roomGameManager;
        this.sendMessageManager = sendMessageManager;
    }

    /**
     * 请求配置信息
     *
     * @param playerController
     * @param req
     */
    @Command(ElephantGodConstant.MsgBean.REQ_CONFIG_INFO)
    public void reqConfigInfo(PlayerController playerController, ReqElephantGodEnterGame req) {
        try {
            log.info("收到玩家请求配置 playerId={}", playerController.playerId());
            ElephantGodGameRunInfo gameRunInfo;
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

    /**
     * 开始游戏
     *
     * @param playerController
     * @param req
     */
    @Command(ElephantGodConstant.MsgBean.REQ_START_GAME)
    public void reqStartGame(PlayerController playerController, ReqElephantGodStartGame req) {
        try {
            log.info("收到玩家开始游戏 playerId={},req={}", playerController.playerId(), JSONObject.toJSONString(req));
            ElephantGodGameRunInfo gameRunInfo;
            if (playerController.getScene() == null) {
                gameRunInfo = gameManager.playerStartGame(playerController, req.stakeValue);
            } else if (playerController.getScene() instanceof SlotsRoomController) {
                gameRunInfo = roomGameManager.playerStartGame(playerController, req.stakeValue);
            } else {
                log.warn("playerController.getScene() is error, scene={}", playerController.getScene());
                return;
            }
            sendMessageManager.sendStartGameMessage(playerController, gameRunInfo);
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
    @Command(ElephantGodConstant.MsgBean.REQ_POOL_INFO)
    public void reqCaptainJackPoolValue(PlayerController playerController, ReqElephantGodPoolValue req) {
        try {
            ElephantGodGameRunInfo gameRunInfo;
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

}
