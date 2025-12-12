package com.jjg.game.slots.game.thor;

import com.alibaba.fastjson.JSONObject;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.slots.game.thor.data.ThorGameRunInfo;
import com.jjg.game.slots.game.thor.manager.ThorGameManager;
import com.jjg.game.slots.game.thor.manager.ThorSendMessageManager;
import com.jjg.game.slots.game.thor.pb.ReqThorFreeChooseOne;
import com.jjg.game.slots.game.thor.pb.ReqThorEnterGame;
import com.jjg.game.slots.game.thor.pb.ReqThorPoolValue;
import com.jjg.game.slots.game.thor.pb.ReqThorStartGame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author 11
 * @date 2025/12/1 18:00
 */
@Component
@MessageType(MessageConst.MessageTypeDef.THOR)
public class ThorMessageHandler {
    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private ThorGameManager gameManager;
    @Autowired
    private ThorSendMessageManager sendMessageManager;

    /**
     * 请求配置信息
     *
     * @param playerController
     * @param req
     */
    @Command(ThorConstant.MsgBean.REQ_ENTER_GAME)
    public void reqConfigInfo(PlayerController playerController, ReqThorEnterGame req) {
        try {
            log.info("收到玩家请求配置 playerId={}", playerController.playerId());
            ThorGameRunInfo gameRunInfo = gameManager.enterGame(playerController);
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
    @Command(ThorConstant.MsgBean.REQ_START_GAME)
    public void reqStartGame(PlayerController playerController, ReqThorStartGame req) {
        try {
            log.info("收到玩家开始游戏 playerId={},req={}", playerController.playerId(), JSONObject.toJSONString(req));
            ThorGameRunInfo gameRunInfo = this.gameManager.playerStartGame(playerController, req.stakeVlue);
            sendMessageManager.sendStartGameMessage(playerController, gameRunInfo);
        } catch (Exception e) {
            log.error("", e);
        }
    }

    /**
     * 免费模式二选一
     *
     * @param playerController
     * @param req
     */
    @Command(ThorConstant.MsgBean.REQ_FREE_CHOOSE_ONE)
    public void reqFreeChooseOne(PlayerController playerController, ReqThorFreeChooseOne req) {
        try {
            log.info("收到二选一 playerId={},req={}", playerController.playerId(), JSONObject.toJSONString(req));
            ThorGameRunInfo gameRunInfo = this.gameManager.freeChooseOne(playerController, req.type);
            sendMessageManager.sendFreeChooseOneMessage(playerController, gameRunInfo);
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
    @Command(ThorConstant.MsgBean.REQ_POOL_VALUE)
    public void reqFreeChooseOne(PlayerController playerController, ReqThorPoolValue req) {
        try {
            ThorGameRunInfo gameRunInfo = this.gameManager.getPoolValue(playerController, req.stakeVlue);
            sendMessageManager.sendPoolMessage(playerController, gameRunInfo);
        } catch (Exception e) {
            log.error("", e);
        }
    }
}
