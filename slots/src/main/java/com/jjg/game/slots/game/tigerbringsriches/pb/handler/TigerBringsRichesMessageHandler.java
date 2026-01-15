package com.jjg.game.slots.game.tigerbringsriches.pb.handler;

import com.alibaba.fastjson.JSONObject;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.slots.controller.SlotsRoomController;
import com.jjg.game.slots.game.tigerbringsriches.constant.TigerBringsRichesConstant;
import com.jjg.game.slots.game.tigerbringsriches.data.TigerBringsRichesGameRunInfo;
import com.jjg.game.slots.game.tigerbringsriches.manager.TigerBringsRichesGameManager;
import com.jjg.game.slots.game.tigerbringsriches.manager.TigerBringsRichesGameSendMessageManager;
import com.jjg.game.slots.game.tigerbringsriches.manager.TigerBringsRichesRoomGameManager;
import com.jjg.game.slots.game.tigerbringsriches.pb.req.ReqTigerBringsRichesEnterGame;
import com.jjg.game.slots.game.tigerbringsriches.pb.req.ReqTigerBringsRichesPoolValue;
import com.jjg.game.slots.game.tigerbringsriches.pb.req.ReqTigerBringsRichesStartGame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author 11
 * @date 2025/8/1 17:37
 */
@Component
@MessageType(MessageConst.MessageTypeDef.TIGER_BRINGS_RICHES)
public class TigerBringsRichesMessageHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final TigerBringsRichesGameManager gameManager;
    private final TigerBringsRichesRoomGameManager roomGameManager;
    private final TigerBringsRichesGameSendMessageManager sendMessageManager;

    public TigerBringsRichesMessageHandler(TigerBringsRichesGameManager pegasusUnbridleGameManager, TigerBringsRichesRoomGameManager pegasusUnbridleRoomGameManager, TigerBringsRichesGameSendMessageManager sendMessageManager) {
        this.gameManager = pegasusUnbridleGameManager;
        this.roomGameManager = pegasusUnbridleRoomGameManager;
        this.sendMessageManager = sendMessageManager;
    }

    /**
     * 请求配置信息
     *
     */
    @Command(TigerBringsRichesConstant.MsgBean.REQ_TIGER_BRINGS_RICHES_ENTER_GAME)
    public void reqPegasusUnbridleEnterGame(PlayerController playerController, ReqTigerBringsRichesEnterGame req) {
        try {
            log.info("收到玩家请求配置 playerId={}", playerController.playerId());
            TigerBringsRichesGameRunInfo gameRunInfo;
            if (playerController.getScene() == null) {
                gameRunInfo = gameManager.enterGame(playerController);
            } else if (playerController.getScene() instanceof SlotsRoomController) {
                gameRunInfo = roomGameManager.enterGame(playerController);
            } else {
                log.warn("playerController.getScene() is error, scene={}", playerController.getScene());
                return;
            }
            sendMessageManager.reqPegasusUnbridleEnterGame(playerController, gameRunInfo);
        } catch (Exception e) {
            log.error("", e);
        }
    }

    /**
     * 开始游戏
     *
     */
    @Command(TigerBringsRichesConstant.MsgBean.REQ_TIGER_BRINGS_RICHES_START_GAME)
    public void reqStartGame(PlayerController playerController, ReqTigerBringsRichesStartGame req) {
        try {
            log.info("收到玩家开始游戏 playerId={},req={}", playerController.playerId(), JSONObject.toJSONString(req));
            TigerBringsRichesGameRunInfo gameRunInfo;
            if (playerController.getScene() == null) {
                gameRunInfo = gameManager.playerStartGame(playerController, req.stakeValue);
            } else if (playerController.getScene() instanceof SlotsRoomController) {
                gameRunInfo = roomGameManager.playerStartGame(playerController, req.stakeValue);
            } else {
                log.warn("reqStartGame playerController.getScene() is error, scene={}", playerController.getScene());
                return;
            }
            sendMessageManager.reqPegasusUnbridleStartGame(playerController, gameRunInfo);
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
    @Command(TigerBringsRichesConstant.MsgBean.REQ_TIGER_BRINGS_RICHES_POOL_VALUE)
    public void reqPegasusUnbridlePoolValue(PlayerController playerController, ReqTigerBringsRichesPoolValue req) {
        try {
            TigerBringsRichesGameRunInfo gameRunInfo;
            if (playerController.getScene() == null) {
                gameRunInfo = gameManager.getPoolValue(playerController, req.stakeValue);
            } else if (playerController.getScene() instanceof SlotsRoomController) {
                gameRunInfo = roomGameManager.getPoolValue(playerController, req.stakeValue);
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
