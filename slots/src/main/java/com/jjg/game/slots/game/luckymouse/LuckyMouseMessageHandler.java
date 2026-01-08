package com.jjg.game.slots.game.luckymouse;

import com.alibaba.fastjson.JSONObject;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.slots.controller.SlotsRoomController;
import com.jjg.game.slots.game.luckymouse.data.LuckyMouseGameRunInfo;
import com.jjg.game.slots.game.luckymouse.manager.LuckyMouseGameManager;
import com.jjg.game.slots.game.luckymouse.manager.LuckyMouseRoomGameManager;
import com.jjg.game.slots.game.luckymouse.manager.LuckyMouseSendMessageManager;
import com.jjg.game.slots.game.luckymouse.pb.ReqLuckyMouseEnterGame;
import com.jjg.game.slots.game.luckymouse.pb.ReqLuckyMouseStartGame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@MessageType(MessageConst.MessageTypeDef.LUCKY_MOUSE)
public class LuckyMouseMessageHandler {
    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private LuckyMouseGameManager gameManager;
    @Autowired
    private LuckyMouseRoomGameManager roomGameManager;
    @Autowired
    private LuckyMouseSendMessageManager sendMessageManager;

    /**
     * 请求配置信息
     *
     * @param playerController
     * @param req
     */
    @Command(LuckyMouseConstant.MsgBean.REQ_CONFIG_INFO)
    public void reqConfigInfo(PlayerController playerController, ReqLuckyMouseEnterGame req) {
        try {
            log.info("收到玩家请求配置 playerId={}", playerController.playerId());
            LuckyMouseGameRunInfo gameRunInfo;
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
    @Command(LuckyMouseConstant.MsgBean.REQ_START_GAME)
    public void reqStartGame(PlayerController playerController, ReqLuckyMouseStartGame req) {
        try {
            log.info("收到玩家开始游戏 playerId={},req={}", playerController.playerId(), JSONObject.toJSONString(req));
            LuckyMouseGameRunInfo gameRunInfo;
            if (playerController.getScene() == null) {
//                gameRunInfo = gameManager.playerStartGame(playerController, req.stakeVlue);
            } else if (playerController.getScene() instanceof SlotsRoomController) {
//                gameRunInfo = roomGameManager.playerStartGame(playerController, req.stakeVlue);
            } else {
                log.warn("playerController.getScene() is error, scene={}", playerController.getScene());
                return;
            }
//            sendMessageManager.sendStartGameMessage(playerController, gameRunInfo);
        } catch (Exception e) {
            log.error("", e);
        }
    }
}
