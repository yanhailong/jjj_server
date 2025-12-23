package com.jjg.game.slots.game.goldsnakefortune;

import com.alibaba.fastjson.JSONObject;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.slots.controller.SlotsRoomController;
import com.jjg.game.slots.game.goldsnakefortune.data.GoldSnakeFortuneGameRunInfo;
import com.jjg.game.slots.game.goldsnakefortune.manager.GoldSnakeFortuneGameManager;
import com.jjg.game.slots.game.goldsnakefortune.manager.GoldSnakeFortuneRoomGameManager;
import com.jjg.game.slots.game.goldsnakefortune.manager.GoldSnakeFortuneSendMessageManager;
import com.jjg.game.slots.game.goldsnakefortune.pb.ReqGoldSnakeFortuneEnterGame;
import com.jjg.game.slots.game.goldsnakefortune.pb.ReqGoldSnakeFortunePool;
import com.jjg.game.slots.game.goldsnakefortune.pb.ReqGoldSnakeFortuneStartGame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author 11
 * @date 2025/6/12 17:15
 */
@Component
@MessageType(MessageConst.MessageTypeDef.GOLD_SNAKE_FORTUNE)
public class GoldSnakeFortuneMessageHandler {
    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private GoldSnakeFortuneGameManager gameManager;
    @Autowired
    private GoldSnakeFortuneRoomGameManager roomGameManager;
    @Autowired
    private GoldSnakeFortuneSendMessageManager sendMessageManager;


    /**
     * 请求配置信息
     *
     * @param playerController
     * @param req
     */
    @Command(GoldSnakeFortuneConstant.MsgBean.REQ_ENTER_GAME)
    public void reqConfigInfo(PlayerController playerController, ReqGoldSnakeFortuneEnterGame req) {
        try {
            log.info("收到玩家请求配置 playerId={},req={}", playerController.playerId(), JSONObject.toJSONString(req));
            GoldSnakeFortuneGameRunInfo gameRunInfo;
            if (playerController.getScene() == null) {
                gameRunInfo = gameManager.enterGame(playerController);
            } else if (playerController.getScene() instanceof SlotsRoomController) {
                gameRunInfo = roomGameManager.enterGame(playerController);
            } else {
                log.warn("playerController.getScene() is error, scene={}", playerController.getScene());
                return;
            }
            sendMessageManager.sendConfigMessage(playerController);
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
    @Command(GoldSnakeFortuneConstant.MsgBean.REQ_START_GAME)
    public void reqStartGame(PlayerController playerController, ReqGoldSnakeFortuneStartGame req) {
        try {
            log.info("收到玩家开始游戏 playerId={},req={}", playerController.playerId(), JSONObject.toJSONString(req));
            GoldSnakeFortuneGameRunInfo gameRunInfo;
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

    /**
     * 奖池
     *
     * @param playerController
     * @param req
     */
    @Command(GoldSnakeFortuneConstant.MsgBean.REQ_POOL_VALUE)
    public void reqPoolValue(PlayerController playerController, ReqGoldSnakeFortunePool req) {
        try {
            log.info("收到获取奖池 playerId={},req={}", playerController.playerId(), JSONObject.toJSONString(req));
            GoldSnakeFortuneGameRunInfo gameRunInfo;
            if (playerController.getScene() == null) {
                gameRunInfo = gameManager.getPoolValue(GoldSnakeFortuneGameRunInfo.class, playerController, req.stakeVlue);
            } else if (playerController.getScene() instanceof SlotsRoomController) {
                gameRunInfo = roomGameManager.getPoolValue(GoldSnakeFortuneGameRunInfo.class, playerController, req.stakeVlue);
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
