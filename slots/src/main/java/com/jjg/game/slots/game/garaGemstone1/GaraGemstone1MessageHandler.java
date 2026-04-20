package com.jjg.game.slots.game.garaGemstone1;

import com.alibaba.fastjson.JSONObject;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.slots.controller.SlotsRoomController;
import com.jjg.game.slots.game.garaGemstone1.data.GaraGemstone1GameRunInfo;
import com.jjg.game.slots.game.garaGemstone1.manager.GaraGemstone1GameManager;
import com.jjg.game.slots.game.garaGemstone1.manager.GaraGemstone1RoomGameManager;
import com.jjg.game.slots.game.garaGemstone1.manager.GaraGemstone1SendMessageManager;
import com.jjg.game.slots.game.garaGemstone1.pb.ReqGaraGemstone1EnterGame;
import com.jjg.game.slots.game.garaGemstone1.pb.ReqGaraGemstone1PoolValue;
import com.jjg.game.slots.game.garaGemstone1.pb.ReqGaraGemstone1StartGame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@MessageType(MessageConst.MessageTypeDef.GARA_GEMSTONE_1)
public class GaraGemstone1MessageHandler {
    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private GaraGemstone1GameManager gameManager;
    @Autowired
    private GaraGemstone1RoomGameManager roomGameManager;
    @Autowired
    private GaraGemstone1SendMessageManager sendMessageManager;

    /**
     * 请求配置信息
     *
     * @param playerController
     * @param req
     */
    @Command(GaraGemstone1Constant.MsgBean.REQ_LUCKY_MOUSE_CONFIG_INFO)
    public void reqConfigInfo(PlayerController playerController, ReqGaraGemstone1EnterGame req) {
        try {
            log.info("收到玩家请求配置 playerId={}", playerController.playerId());
            GaraGemstone1GameRunInfo gameRunInfo;
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
    @Command(GaraGemstone1Constant.MsgBean.REQ_LUCKY_MOUSE_START_GAME)
    public void reqStartGame(PlayerController playerController, ReqGaraGemstone1StartGame req) {
        try {
            log.info("收到玩家开始游戏 playerId={},req={}", playerController.playerId(), JSONObject.toJSONString(req));
            GaraGemstone1GameRunInfo gameRunInfo;
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
    @Command(GaraGemstone1Constant.MsgBean.REQ_LUCKY_MOUSE_POOL_INFO)
    public void reqGaraGemstone1PoolValue(PlayerController playerController, ReqGaraGemstone1PoolValue req) {
        try {
            GaraGemstone1GameRunInfo gameRunInfo;
            if (playerController.getScene() == null) {
                gameRunInfo = gameManager.getPoolValue(playerController,req.stakeValue);
            } else if (playerController.getScene() instanceof SlotsRoomController) {
                gameRunInfo = roomGameManager.getPoolValue(playerController,req.stakeValue);
            } else {
                log.warn("playerController.getScene() is error, scene={}", playerController.getScene());
                return;
            }
            sendMessageManager.sendPoolValueMessage(playerController, gameRunInfo);
        } catch (Exception e) {
            log.error("", e);
        }
    }
}
