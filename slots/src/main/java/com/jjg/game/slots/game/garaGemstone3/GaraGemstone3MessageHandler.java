package com.jjg.game.slots.game.garaGemstone3;

import com.alibaba.fastjson.JSONObject;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.slots.controller.SlotsRoomController;
import com.jjg.game.slots.game.garaGemstone3.data.GaraGemstone3GameRunInfo;
import com.jjg.game.slots.game.garaGemstone3.manager.GaraGemstone3GameManager;
import com.jjg.game.slots.game.garaGemstone3.manager.GaraGemstone3RoomGameManager;
import com.jjg.game.slots.game.garaGemstone3.manager.GaraGemstone3SendMessageManager;
import com.jjg.game.slots.game.garaGemstone3.pb.ReqGaraGemstone3EnterGame;
import com.jjg.game.slots.game.garaGemstone3.pb.ReqGaraGemstone3PoolValue;
import com.jjg.game.slots.game.garaGemstone3.pb.ReqGaraGemstone3StartGame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@MessageType(MessageConst.MessageTypeDef.GARA_GEMSTONE_3)
public class GaraGemstone3MessageHandler {
    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private GaraGemstone3GameManager gameManager;
    @Autowired
    private GaraGemstone3RoomGameManager roomGameManager;
    @Autowired
    private GaraGemstone3SendMessageManager sendMessageManager;

    /**
     * 请求配置信息
     *
     * @param playerController
     * @param req
     */
    @Command(GaraGemstone3Constant.MsgBean.REQ_GARA_GEMSTONE_3_CONFIG_INFO)
    public void reqConfigInfo(PlayerController playerController, ReqGaraGemstone3EnterGame req) {
        try {
            log.info("收到玩家请求配置 playerId={}", playerController.playerId());
            GaraGemstone3GameRunInfo gameRunInfo;
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
    @Command(GaraGemstone3Constant.MsgBean.REQ_GARA_GEMSTONE_3_START_GAME)
    public void reqStartGame(PlayerController playerController, ReqGaraGemstone3StartGame req) {
        try {
            log.info("收到玩家开始游戏 playerId={},req={}", playerController.playerId(), JSONObject.toJSONString(req));
            GaraGemstone3GameRunInfo gameRunInfo;
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
    @Command(GaraGemstone3Constant.MsgBean.REQ_GARA_GEMSTONE_3_POOL_INFO)
    public void reqGaraGemstone3PoolValue(PlayerController playerController, ReqGaraGemstone3PoolValue req) {
        try {
            GaraGemstone3GameRunInfo gameRunInfo;
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
