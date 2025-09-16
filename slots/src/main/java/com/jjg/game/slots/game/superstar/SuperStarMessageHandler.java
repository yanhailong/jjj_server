package com.jjg.game.slots.game.superstar;

import com.alibaba.fastjson.JSONObject;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.slots.game.dollarexpress.pb.*;
import com.jjg.game.slots.game.superstar.data.SuperStarGameRunInfo;
import com.jjg.game.slots.game.superstar.manager.SuperStarGameManager;
import com.jjg.game.slots.game.superstar.manager.SuperStarSendMessageManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 */
@Component
@MessageType(MessageConst.MessageTypeDef.SUPER_STAR_TYPE)
public class SuperStarMessageHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private SuperStarGameManager gameManager;
    @Autowired
    private SuperStarSendMessageManager sendMessageManager;


    /**
     * 请求配置信息
     *
     * @param playerController
     * @param req
     */
    @Command(SuperStarConstant.MsgBean.REQ_CONFIG_INFO)
    public void reqConfigInfo(PlayerController playerController, ReqConfigInfo req) {
        try {
            log.info("收到玩家请求配置 playerId={},req={}", playerController.playerId(), JSONObject.toJSONString(req));
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
    @Command(SuperStarConstant.MsgBean.REQ_START_GAME)
    public void reqStartGame(PlayerController playerController, ReqStartGame req) {
        try {
            log.info("收到玩家开始游戏 playerId={},req={}", playerController.playerId(), JSONObject.toJSONString(req));
            SuperStarGameRunInfo gameRunInfo = this.gameManager.playerStartGame(playerController, req.stakeVlue);
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
    @Command(SuperStarConstant.MsgBean.REQ_POOL_VALUE)
    public void reqPoolValue(PlayerController playerController, ReqPoolValue req) {
        try {
            log.info("收到获取奖池 playerId={},req={}", playerController.playerId(), JSONObject.toJSONString(req));
            SuperStarGameRunInfo gameRunInfo = gameManager.getPoolValue(playerController, req.stakeVlue);
            sendMessageManager.sendPoolValue(playerController, gameRunInfo);
        } catch (Exception e) {
            log.error("", e);
        }
    }
}
