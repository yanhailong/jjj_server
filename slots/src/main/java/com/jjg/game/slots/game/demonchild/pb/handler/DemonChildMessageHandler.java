package com.jjg.game.slots.game.demonchild.pb.handler;

import com.alibaba.fastjson.JSONObject;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.slots.controller.SlotsRoomController;
import com.jjg.game.slots.game.demonchild.constant.DemonChildConstant;
import com.jjg.game.slots.game.demonchild.data.DemonChildGameRunInfo;
import com.jjg.game.slots.game.demonchild.manager.DemonChildGameManager;
import com.jjg.game.slots.game.demonchild.manager.DemonChildGameSendMessageManager;
import com.jjg.game.slots.game.demonchild.manager.DemonChildRoomGameManager;
import com.jjg.game.slots.game.demonchild.pb.req.ReqDemonChildEnterGame;
import com.jjg.game.slots.game.demonchild.pb.req.ReqDemonChildPoolValue;
import com.jjg.game.slots.game.demonchild.pb.req.ReqDemonChildStartGame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author 11
 * @date 2025/8/1 17:37
 */
@Component
@MessageType(MessageConst.MessageTypeDef.CAPTAIN_JACK)
public class DemonChildMessageHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final DemonChildGameManager gameManager;
    private final DemonChildRoomGameManager roomGameManager;
    private final DemonChildGameSendMessageManager sendMessageManager;

    public DemonChildMessageHandler(DemonChildGameManager gameManager, DemonChildRoomGameManager roomGameManager, DemonChildGameSendMessageManager sendMessageManager) {
        this.gameManager = gameManager;
        this.roomGameManager = roomGameManager;
        this.sendMessageManager = sendMessageManager;
    }

    /**
     * 请求配置信息
     *
     */
    @Command(DemonChildConstant.MsgBean.REQ_DEMON_CHILD_ENTER_GAME)
    public void reqDemonChildEnterGame(PlayerController playerController, ReqDemonChildEnterGame req) {
        try {
            log.info("收到玩家请求配置 playerId={}", playerController.playerId());
            DemonChildGameRunInfo gameRunInfo;
            if (playerController.getScene() == null) {
                gameRunInfo = gameManager.enterGame(playerController);
            } else if (playerController.getScene() instanceof SlotsRoomController) {
                gameRunInfo = roomGameManager.enterGame(playerController);
            } else {
                log.warn("playerController.getScene() is error, scene={}", playerController.getScene());
                return;
            }
            sendMessageManager.reqDemonChildEnterGame(playerController,gameRunInfo);
        } catch (Exception e) {
            log.error("", e);
        }
    }

    /**
     * 开始游戏
     *
     */
    @Command(DemonChildConstant.MsgBean.REQ_DEMON_CHILD_START_GAME)
    public void reqStartGame(PlayerController playerController, ReqDemonChildStartGame req) {
        try {
            log.info("收到玩家开始游戏 playerId={},req={}", playerController.playerId(), JSONObject.toJSONString(req));
            DemonChildGameRunInfo gameRunInfo;
            if (playerController.getScene() == null) {
                gameRunInfo = gameManager.playerStartGame(playerController,req.stakeValue);
            } else if (playerController.getScene() instanceof SlotsRoomController) {
                gameRunInfo = roomGameManager.playerStartGame(playerController,req.stakeValue);
            } else {
                log.warn("playerController.getScene() is error, scene={}", playerController.getScene());
                return;
            }
            sendMessageManager.reqDemonChildStartGame(playerController, gameRunInfo);
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
    @Command(DemonChildConstant.MsgBean.REQ_DEMON_CHILD_POOL_VALUE)
    public void reqDemonChildPoolValue(PlayerController playerController, ReqDemonChildPoolValue req) {
        try {
            DemonChildGameRunInfo gameRunInfo;
            if (playerController.getScene() == null) {
                gameRunInfo = gameManager.getPoolValue(DemonChildGameRunInfo.class,playerController,req.stakeValue);
            } else if (playerController.getScene() instanceof SlotsRoomController) {
                gameRunInfo = roomGameManager.getPoolValue(DemonChildGameRunInfo.class,playerController,req.stakeValue);
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
