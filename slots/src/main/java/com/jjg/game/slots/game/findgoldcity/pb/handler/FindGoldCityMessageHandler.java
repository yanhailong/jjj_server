package com.jjg.game.slots.game.findgoldcity.pb.handler;

import com.alibaba.fastjson.JSONObject;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.slots.controller.SlotsRoomController;
import com.jjg.game.slots.game.findgoldcity.constant.FindGoldCityConstant;
import com.jjg.game.slots.game.findgoldcity.data.FindGoldCityGameRunInfo;
import com.jjg.game.slots.game.findgoldcity.manager.FindGoldCityGameManager;
import com.jjg.game.slots.game.findgoldcity.manager.FindGoldCityGameSendMessageManager;
import com.jjg.game.slots.game.findgoldcity.manager.FindGoldCityRoomGameManager;
import com.jjg.game.slots.game.findgoldcity.pb.req.ReqFindGoldCityEnterGame;
import com.jjg.game.slots.game.findgoldcity.pb.req.ReqFindGoldCityPoolValue;
import com.jjg.game.slots.game.findgoldcity.pb.req.ReqFindGoldCityStartGame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author 11
 * @date 2025/8/1 17:37
 */
@Component
@MessageType(MessageConst.MessageTypeDef.FIND_GOLD_CITY)
public class FindGoldCityMessageHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final FindGoldCityGameManager gameManager;
    private final FindGoldCityRoomGameManager roomGameManager;
    private final FindGoldCityGameSendMessageManager sendMessageManager;

    public FindGoldCityMessageHandler(FindGoldCityGameManager pegasusUnbridleGameManager, FindGoldCityRoomGameManager pegasusUnbridleRoomGameManager, FindGoldCityGameSendMessageManager sendMessageManager) {
        this.gameManager = pegasusUnbridleGameManager;
        this.roomGameManager = pegasusUnbridleRoomGameManager;
        this.sendMessageManager = sendMessageManager;
    }

    /**
     * 请求配置信息
     *
     */
    @Command(FindGoldCityConstant.MsgBean.REQ_FIND_GOLD_CITY_ENTER_GAME)
    public void reqFindGoldCityEnterGame(PlayerController playerController, ReqFindGoldCityEnterGame req) {
        try {
            log.info("收到玩家请求配置 getPlayerId={}", playerController.playerId());
            FindGoldCityGameRunInfo gameRunInfo;
            if (playerController.getScene() == null) {
                gameRunInfo = gameManager.enterGame(playerController);
            } else if (playerController.getScene() instanceof SlotsRoomController) {
                gameRunInfo = roomGameManager.enterGame(playerController);
            } else {
                log.warn("playerController.getScene() is error, scene={}", playerController.getScene());
                return;
            }
            sendMessageManager.reqFindGoldCityEnterGame(playerController, gameRunInfo);
        } catch (Exception e) {
            log.error("", e);
        }
    }

    /**
     * 开始游戏
     *
     */
    @Command(FindGoldCityConstant.MsgBean.REQ_FIND_GOLD_CITY_START_GAME)
    public void reqFindGoldCityStartGame(PlayerController playerController, ReqFindGoldCityStartGame req) {
        try {
            log.info("收到玩家开始游戏 getPlayerId={},req={}", playerController.playerId(), JSONObject.toJSONString(req));
            FindGoldCityGameRunInfo gameRunInfo;
            if (playerController.getScene() == null) {
                gameRunInfo = gameManager.playerStartGame(playerController, req.stakeValue);
            } else if (playerController.getScene() instanceof SlotsRoomController) {
                gameRunInfo = roomGameManager.playerStartGame(playerController, req.stakeValue);
            } else {
                log.warn("reqStartGame playerController.getScene() is error, scene={}", playerController.getScene());
                return;
            }
            sendMessageManager.reqFindGoldCityStartGame(playerController, gameRunInfo);
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
    @Command(FindGoldCityConstant.MsgBean.REQ_FIND_GOLD_CITY_POOL_VALUE)
    public void reqFindGoldCityPoolValue(PlayerController playerController, ReqFindGoldCityPoolValue req) {
        try {
            FindGoldCityGameRunInfo gameRunInfo;
            if (playerController.getScene() == null) {
                gameRunInfo = gameManager.getPoolValue(playerController, req.stakeValue);
            } else if (playerController.getScene() instanceof SlotsRoomController) {
                gameRunInfo = roomGameManager.getPoolValue(playerController, req.stakeValue);
            } else {
                log.warn("playerController.getScene() is error, scene={}", playerController.getScene());
                return;
            }
            sendMessageManager.reqFindGoldCityPoolValue(playerController, gameRunInfo);
        } catch (Exception e) {
            log.error("", e);
        }
    }
}
