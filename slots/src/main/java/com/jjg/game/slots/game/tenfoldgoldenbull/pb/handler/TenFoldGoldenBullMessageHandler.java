package com.jjg.game.slots.game.tenfoldgoldenbull.pb.handler;

import com.alibaba.fastjson.JSONObject;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.slots.controller.SlotsRoomController;
import com.jjg.game.slots.game.tenfoldgoldenbull.constant.TenFoldGoldenBullConstant;
import com.jjg.game.slots.game.tenfoldgoldenbull.data.TenFoldGoldenBullGameRunInfo;
import com.jjg.game.slots.game.tenfoldgoldenbull.manager.TenFoldGoldenBullGameManager;
import com.jjg.game.slots.game.tenfoldgoldenbull.manager.TenFoldGoldenBullSendMessageManager;
import com.jjg.game.slots.game.tenfoldgoldenbull.manager.TenFoldGoldenBullRoomGameManager;
import com.jjg.game.slots.game.tenfoldgoldenbull.pb.req.ReqTenFoldGoldenBullEnterGame;
import com.jjg.game.slots.game.tenfoldgoldenbull.pb.req.ReqTenFoldGoldenBullPoolValue;
import com.jjg.game.slots.game.tenfoldgoldenbull.pb.req.ReqTenFoldGoldenBullStartGame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author 11
 * @date 2025/8/1 17:37
 */
@Component
@MessageType(MessageConst.MessageTypeDef.TENFOLD_GOLDEN_BULL)
public class TenFoldGoldenBullMessageHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final TenFoldGoldenBullGameManager gameManager;
    private final TenFoldGoldenBullRoomGameManager roomGameManager;
    private final TenFoldGoldenBullSendMessageManager sendMessageManager;

    public TenFoldGoldenBullMessageHandler(TenFoldGoldenBullGameManager pegasusUnbridleGameManager, TenFoldGoldenBullRoomGameManager pegasusUnbridleRoomGameManager, TenFoldGoldenBullSendMessageManager sendMessageManager) {
        this.gameManager = pegasusUnbridleGameManager;
        this.roomGameManager = pegasusUnbridleRoomGameManager;
        this.sendMessageManager = sendMessageManager;
    }

    /**
     * 请求配置信息
     *
     */
    @Command(TenFoldGoldenBullConstant.MsgBean.REQ_TEN_FOLD_GOLDEN_BULL_ENTER_GAME)
    public void reqPegasusUnbridleEnterGame(PlayerController playerController, ReqTenFoldGoldenBullEnterGame req) {
        try {
            log.info("收到玩家请求配置 playerId={}", playerController.playerId());
            TenFoldGoldenBullGameRunInfo gameRunInfo;
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
    @Command(TenFoldGoldenBullConstant.MsgBean.REQ_TEN_FOLD_GOLDEN_BULL_START_GAME)
    public void reqStartGame(PlayerController playerController, ReqTenFoldGoldenBullStartGame req) {
        try {
            log.info("收到玩家开始游戏 playerId={},req={}", playerController.playerId(), JSONObject.toJSONString(req));
            TenFoldGoldenBullGameRunInfo gameRunInfo;
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
    @Command(TenFoldGoldenBullConstant.MsgBean.REQ_TEN_FOLD_GOLDEN_BULL_POOL_VALUE)
    public void reqPegasusUnbridlePoolValue(PlayerController playerController, ReqTenFoldGoldenBullPoolValue req) {
        try {
            TenFoldGoldenBullGameRunInfo gameRunInfo;
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
