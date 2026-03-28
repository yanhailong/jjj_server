package com.jjg.game.slots.game.candyparty.pb.handler;

import com.alibaba.fastjson.JSONObject;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.slots.controller.SlotsRoomController;
import com.jjg.game.slots.game.candyparty.constant.CandyPartyConstant;
import com.jjg.game.slots.game.candyparty.data.CandyPartyGameRunInfo;
import com.jjg.game.slots.game.candyparty.manager.CandyPartyGameManager;
import com.jjg.game.slots.game.candyparty.manager.CandyPartyGameSendMessageManager;
import com.jjg.game.slots.game.candyparty.manager.CandyPartyRoomGameManager;
import com.jjg.game.slots.game.candyparty.pb.req.ReqCandyPartyEnterGame;
import com.jjg.game.slots.game.candyparty.pb.req.ReqCandyPartyPoolValue;
import com.jjg.game.slots.game.candyparty.pb.req.ReqCandyPartyStartGame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author 11
 * @date 2025/8/1 17:37
 */
@Component
@MessageType(MessageConst.MessageTypeDef.CANDY_PARTY)
public class CandyPartyMessageHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final CandyPartyGameManager gameManager;
    private final CandyPartyRoomGameManager roomGameManager;
    private final CandyPartyGameSendMessageManager sendMessageManager;

    public CandyPartyMessageHandler(CandyPartyGameManager gameManager, CandyPartyRoomGameManager roomGameManager, CandyPartyGameSendMessageManager sendMessageManager) {
        this.gameManager = gameManager;
        this.roomGameManager = roomGameManager;
        this.sendMessageManager = sendMessageManager;
    }

    /**
     * 请求配置信息
     *
     */
    @Command(CandyPartyConstant.MsgBean.REQ_CANDY_PARTY_ENTER_GAME)
    public void reqCaptainJackEnterGame(PlayerController playerController, ReqCandyPartyEnterGame req) {
        try {
            log.info("收到玩家请求配置 playerId={}", playerController.playerId());
            CandyPartyGameRunInfo gameRunInfo;
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
    @Command(CandyPartyConstant.MsgBean.REQ_CANDY_PARTY_START_GAME)
    public void reqStartGame(PlayerController playerController, ReqCandyPartyStartGame req) {
        try {
            log.info("收到玩家开始游戏 playerId={},req={}", playerController.playerId(), JSONObject.toJSONString(req));
            CandyPartyGameRunInfo gameRunInfo;
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
    @Command(CandyPartyConstant.MsgBean.REQ_CANDY_PARTY_POOL_VALUE)
    public void reqCaptainJackPoolValue(PlayerController playerController, ReqCandyPartyPoolValue req) {
        try {
            CandyPartyGameRunInfo gameRunInfo;
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
