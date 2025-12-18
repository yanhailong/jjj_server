package com.jjg.game.slots.game.pegasusunbridle.pb.handler;

import com.alibaba.fastjson.JSONObject;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.slots.game.pegasusunbridle.constant.PegasusUnbridleConstant;
import com.jjg.game.slots.game.pegasusunbridle.data.PegasusUnbridleGameRunInfo;
import com.jjg.game.slots.game.pegasusunbridle.manager.PegasusUnbridleGameManager;
import com.jjg.game.slots.game.pegasusunbridle.manager.PegasusUnbridleGameSendMessageManager;
import com.jjg.game.slots.game.pegasusunbridle.pb.req.ReqPegasusUnbridleEnterGame;
import com.jjg.game.slots.game.pegasusunbridle.pb.req.ReqPegasusUnbridlePoolValue;
import com.jjg.game.slots.game.pegasusunbridle.pb.req.ReqPegasusUnbridleStartGame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author 11
 * @date 2025/8/1 17:37
 */
@Component
@MessageType(MessageConst.MessageTypeDef.CAPTAIN_JACK)
public class PegasusUnbridleMessageHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final PegasusUnbridleGameManager gameManager;
    private final PegasusUnbridleGameSendMessageManager sendMessageManager;

    public PegasusUnbridleMessageHandler(PegasusUnbridleGameManager gameManager, PegasusUnbridleGameSendMessageManager sendMessageManager) {
        this.gameManager = gameManager;
        this.sendMessageManager = sendMessageManager;
    }

    /**
     * 请求配置信息
     *
     */
    @Command(PegasusUnbridleConstant.MsgBean.REQ_CAPTAIN_JACK_ENTER_GAME)
    public void reqPegasusUnbridleEnterGame(PlayerController playerController, ReqPegasusUnbridleEnterGame req) {
        try {
            log.info("收到玩家请求配置 playerId={}", playerController.playerId());
            PegasusUnbridleGameRunInfo gameRunInfo = gameManager.enterGame(playerController);
            sendMessageManager.reqPegasusUnbridleEnterGame(playerController,gameRunInfo);
        } catch (Exception e) {
            log.error("", e);
        }
    }

    /**
     * 开始游戏
     *
     */
    @Command(PegasusUnbridleConstant.MsgBean.REQ_CAPTAIN_JACK_START_GAME)
    public void reqStartGame(PlayerController playerController, ReqPegasusUnbridleStartGame req) {
        try {
            log.info("收到玩家开始游戏 playerId={},req={}", playerController.playerId(), JSONObject.toJSONString(req));
            PegasusUnbridleGameRunInfo gameRunInfo = this.gameManager.playerStartGame(playerController, req.stakeValue);
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
    @Command(PegasusUnbridleConstant.MsgBean.REQ_CAPTAIN_JACK_POOL_VALUE)
    public void reqPegasusUnbridlePoolValue(PlayerController playerController, ReqPegasusUnbridlePoolValue req) {
        try {
            PegasusUnbridleGameRunInfo gameRunInfo = this.gameManager.getPoolValue(playerController, req.stakeValue);
            sendMessageManager.sendPoolMessage(playerController, gameRunInfo);
        } catch (Exception e) {
            log.error("", e);
        }
    }


}
