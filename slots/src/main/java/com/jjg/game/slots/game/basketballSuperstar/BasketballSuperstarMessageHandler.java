package com.jjg.game.slots.game.basketballSuperstar;

import com.alibaba.fastjson.JSONObject;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.slots.game.basketballSuperstar.data.BasketballSuperstarGameRunInfo;
import com.jjg.game.slots.game.basketballSuperstar.manager.BasketballSuperstarGameManager;
import com.jjg.game.slots.game.basketballSuperstar.manager.BasketballSuperstarSendMessageManager;
import com.jjg.game.slots.game.basketballSuperstar.pb.ReqBasketballSuperstarEnterGame;
import com.jjg.game.slots.game.basketballSuperstar.pb.ReqBasketballSuperstarPoolInfo;
import com.jjg.game.slots.game.basketballSuperstar.pb.ReqBasketballSuperstarStartGame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author lihaocao
 * @date 2025/12/2 17:37
 */
@Component
@MessageType(MessageConst.MessageTypeDef.BASKETBALL_SUPERSTAR)
public class BasketballSuperstarMessageHandler {
    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private BasketballSuperstarGameManager gameManager;
    @Autowired
    private BasketballSuperstarSendMessageManager sendMessageManager;

    /**
     * 请求配置信息
     *
     * @param playerController
     * @param req
     */
    @Command(BasketballSuperstarConstant.MsgBean.REQ_CONFIG_INFO)
    public void reqConfigInfo(PlayerController playerController, ReqBasketballSuperstarEnterGame req) {
        try {
            log.info("收到玩家请求配置 playerId={}", playerController.playerId());
            BasketballSuperstarGameRunInfo gameRunInfo = gameManager.enterGame(playerController);
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
    @Command(BasketballSuperstarConstant.MsgBean.REQ_START_GAME)
    public void reqStartGame(PlayerController playerController, ReqBasketballSuperstarStartGame req) {
        try {
            log.info("收到玩家开始游戏 playerId={},req={}", playerController.playerId(), JSONObject.toJSONString(req));
            BasketballSuperstarGameRunInfo gameRunInfo = this.gameManager.playerStartGame(playerController, req.stakeVlue);
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
    @Command(BasketballSuperstarConstant.MsgBean.REQ_POOL_INFO)
    public void reqGetPoolInfo(PlayerController playerController, ReqBasketballSuperstarPoolInfo req) {
        try {
            log.info("收到获取奖池 playerId={},req={}", playerController.playerId(), JSONObject.toJSONString(req));
            BasketballSuperstarGameRunInfo gameRunInfo = gameManager.getPoolValue(playerController, req.stakeVlue);
            sendMessageManager.sendPoolValue(playerController, gameRunInfo);
        } catch (Exception e) {
            log.error("", e);
        }
    }

}
