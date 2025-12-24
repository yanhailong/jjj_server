package com.jjg.game.slots.game.steamAge;

import com.alibaba.fastjson.JSONObject;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.slots.controller.SlotsRoomController;
import com.jjg.game.slots.game.steamAge.data.SteamAgeGameRunInfo;
import com.jjg.game.slots.game.steamAge.manager.SteamAgeGameManager;
import com.jjg.game.slots.game.steamAge.manager.SteamAgeRoomGameManager;
import com.jjg.game.slots.game.steamAge.manager.SteamAgeSendMessageManager;
import com.jjg.game.slots.game.steamAge.pb.ReqSteamAgeEnterGame;
import com.jjg.game.slots.game.steamAge.pb.ReqSteamAgePoolInfo;
import com.jjg.game.slots.game.steamAge.pb.ReqSteamAgeStartGame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author lihaocao
 * @date 2025/12/2 17:37
 */
@Component
@MessageType(MessageConst.MessageTypeDef.STEAM_AGE)
public class SteamAgeMessageHandler {
    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private SteamAgeGameManager gameManager;
    @Autowired
    private SteamAgeRoomGameManager roomGameManager;
    @Autowired
    private SteamAgeSendMessageManager sendMessageManager;

    /**
     * 请求配置信息
     *
     * @param playerController
     * @param req
     */
    @Command(SteamAgeConstant.MsgBean.REQ_CONFIG_INFO)
    public void reqConfigInfo(PlayerController playerController, ReqSteamAgeEnterGame req) {
        try {
            log.info("收到玩家请求配置 playerId={}", playerController.playerId());
            SteamAgeGameRunInfo gameRunInfo;
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
    @Command(SteamAgeConstant.MsgBean.REQ_START_GAME)
    public void reqStartGame(PlayerController playerController, ReqSteamAgeStartGame req) {
        try {
            log.info("收到玩家开始游戏 playerId={},req={}", playerController.playerId(), JSONObject.toJSONString(req));
            SteamAgeGameRunInfo gameRunInfo;
            if (playerController.getScene() == null) {
                gameRunInfo = this.gameManager.playerStartGame(playerController, req.stakeVlue);
            } else if (playerController.getScene() instanceof SlotsRoomController) {
                gameRunInfo = this.roomGameManager.playerStartGame(playerController, req.stakeVlue);
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
     * 获取奖池
     *
     * @param playerController
     * @param req
     */
    @Command(SteamAgeConstant.MsgBean.REQ_POOL_INFO)
    public void reqGetPoolInfo(PlayerController playerController, ReqSteamAgePoolInfo req) {
        try {
            log.info("收到获取奖池 playerId={},req={}", playerController.playerId(), JSONObject.toJSONString(req));
            SteamAgeGameRunInfo gameRunInfo;
            if (playerController.getScene() == null) {
                gameRunInfo = gameManager.getPoolValue(SteamAgeGameRunInfo.class, playerController, req.stakeVlue);
            } else if (playerController.getScene() instanceof SlotsRoomController) {
                gameRunInfo = roomGameManager.getPoolValue(SteamAgeGameRunInfo.class, playerController, req.stakeVlue);
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
