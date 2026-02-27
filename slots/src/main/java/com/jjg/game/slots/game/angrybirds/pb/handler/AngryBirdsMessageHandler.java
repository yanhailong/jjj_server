package com.jjg.game.slots.game.angrybirds.pb.handler;

import com.alibaba.fastjson.JSONObject;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.slots.controller.SlotsRoomController;
import com.jjg.game.slots.game.angrybirds.constant.AngryBirdsConstant;
import com.jjg.game.slots.game.angrybirds.data.AngryBirdsGameRunInfo;
import com.jjg.game.slots.game.angrybirds.manager.AngryBirdsGameManager;
import com.jjg.game.slots.game.angrybirds.manager.AngryBirdsGameSendMessageManager;
import com.jjg.game.slots.game.angrybirds.manager.AngryBirdsRoomGameManager;
import com.jjg.game.slots.game.angrybirds.pb.req.ReqAngryBirdsEnterGame;
import com.jjg.game.slots.game.angrybirds.pb.req.ReqAngryBirdsPoolValue;
import com.jjg.game.slots.game.angrybirds.pb.req.ReqAngryBirdsStartGame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author 11
 * @date 2025/8/1 17:37
 */
@Component
@MessageType(MessageConst.MessageTypeDef.CAPTAIN_JACK)
public class AngryBirdsMessageHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final AngryBirdsGameManager gameManager;
    private final AngryBirdsRoomGameManager roomGameManager;
    private final AngryBirdsGameSendMessageManager sendMessageManager;

    public AngryBirdsMessageHandler(AngryBirdsGameManager gameManager, AngryBirdsRoomGameManager roomGameManager, AngryBirdsGameSendMessageManager sendMessageManager) {
        this.gameManager = gameManager;
        this.roomGameManager = roomGameManager;
        this.sendMessageManager = sendMessageManager;
    }

    /**
     * 请求配置信息
     *
     */
    @Command(AngryBirdsConstant.MsgBean.REQ_ANGRY_BIRDS_ENTER_GAME)
    public void reqAngryBirdsEnterGame(PlayerController playerController, ReqAngryBirdsEnterGame req) {
        try {
            log.info("收到玩家请求配置 playerId={}", playerController.playerId());
            AngryBirdsGameRunInfo gameRunInfo;
            if (playerController.getScene() == null) {
                gameRunInfo = gameManager.enterGame(playerController);
            } else if (playerController.getScene() instanceof SlotsRoomController) {
                gameRunInfo = roomGameManager.enterGame(playerController);
            } else {
                log.warn("playerController.getScene() is error, scene={}", playerController.getScene());
                return;
            }
            sendMessageManager.reqAngryBirdsEnterGame(playerController, gameRunInfo);
        } catch (Exception e) {
            log.error("", e);
        }
    }

    /**
     * 开始游戏
     *
     */
    @Command(AngryBirdsConstant.MsgBean.REQ_ANGRY_BIRDS_START_GAME)
    public void reqAngryBirdsStartGame(PlayerController playerController, ReqAngryBirdsStartGame req) {
        try {
            log.info("收到玩家开始游戏 playerId={},req={}", playerController.playerId(), JSONObject.toJSONString(req));
            AngryBirdsGameRunInfo gameRunInfo;
            if (playerController.getScene() == null) {
                gameRunInfo = gameManager.playerStartGame(playerController, req.stakeValue);
            } else if (playerController.getScene() instanceof SlotsRoomController) {
                gameRunInfo = roomGameManager.playerStartGame(playerController, req.stakeValue);
            } else {
                log.warn("playerController.getScene() is error, scene={}", playerController.getScene());
                return;
            }
            sendMessageManager.reqAngryBirdsStartGame(playerController, gameRunInfo);
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
    @Command(AngryBirdsConstant.MsgBean.REQ_ANGRY_BIRDS_POOL_VALUE)
    public void reqAngryBirdsPoolValue(PlayerController playerController, ReqAngryBirdsPoolValue req) {
        try {
            AngryBirdsGameRunInfo gameRunInfo;
            if (playerController.getScene() == null) {
                gameRunInfo = gameManager.getPoolValue(playerController, req.stakeValue);
            } else if (playerController.getScene() instanceof SlotsRoomController) {
                gameRunInfo = roomGameManager.getPoolValue(playerController, req.stakeValue);
            } else {
                log.warn("playerController.getScene() is error, scene={}", playerController.getScene());
                return;
            }
            sendMessageManager.reqAngryBirdsPoolValue(playerController, gameRunInfo);
        } catch (Exception e) {
            log.error("", e);
        }
    }

}
