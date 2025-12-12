package com.jjg.game.slots.game.christmasBashNight;

import com.alibaba.fastjson.JSONObject;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.slots.game.christmasBashNight.data.ChristmasBashNightGameRunInfo;
import com.jjg.game.slots.game.christmasBashNight.manager.ChristmasBashNightGameManager;
import com.jjg.game.slots.game.christmasBashNight.manager.ChristmasBashNightSendMessageManager;
import com.jjg.game.slots.game.christmasBashNight.pb.ReqChristmasBashNightEnterGame;
import com.jjg.game.slots.game.christmasBashNight.pb.ReqChristmasBashNightPoolInfo;
import com.jjg.game.slots.game.christmasBashNight.pb.ReqChristmasBashNightStartGame;
import com.jjg.game.slots.game.christmasBashNight.pb.ResChristmasBashNightPoolInfo;
import com.jjg.game.slots.game.dollarexpress.data.DollarExpressGameRunInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author lihaocao
 * @date 2025/12/2 17:37
 */
@Component
@MessageType(MessageConst.MessageTypeDef.CHRISTMAS_NIGHT_TYPE)
public class ChristmasBashNightMessageHandler {
    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private ChristmasBashNightGameManager gameManager;
    @Autowired
    private ChristmasBashNightSendMessageManager sendMessageManager;

    /**
     * 请求配置信息
     *
     * @param playerController
     * @param req
     */
    @Command(ChristmasBashNightConstant.MsgBean.REQ_CONFIG_INFO)
    public void reqConfigInfo(PlayerController playerController, ReqChristmasBashNightEnterGame req) {
        try {
            log.info("收到玩家请求配置 playerId={}", playerController.playerId());
            ChristmasBashNightGameRunInfo gameRunInfo = gameManager.enterGame(playerController);
            sendMessageManager.sendConfigMessage(playerController,gameRunInfo);
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
    @Command(ChristmasBashNightConstant.MsgBean.REQ_START_GAME)
    public void reqStartGame(PlayerController playerController, ReqChristmasBashNightStartGame req) {
        try {
            log.info("收到玩家开始游戏 playerId={},req={}", playerController.playerId(), JSONObject.toJSONString(req));
            ChristmasBashNightGameRunInfo gameRunInfo = this.gameManager.playerStartGame(playerController, req.stakeVlue);
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
    @Command(ChristmasBashNightConstant.MsgBean.REQ_POOL_INFO)
    public void reqGetPoolInfo(PlayerController playerController, ReqChristmasBashNightPoolInfo req) {
        try {
            log.info("收到获取奖池 playerId={},req={}", playerController.playerId(), JSONObject.toJSONString(req));
            ChristmasBashNightGameRunInfo gameRunInfo = gameManager.getPoolValue(playerController, req.stakeVlue);
            sendMessageManager.sendPoolValue(playerController, gameRunInfo);
        } catch (Exception e) {
            log.error("", e);
        }
    }

}
