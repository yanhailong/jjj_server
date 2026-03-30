package com.jjg.game.slots.game.moneyrabbit;

import com.alibaba.fastjson.JSONObject;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.slots.controller.SlotsRoomController;
import com.jjg.game.slots.game.moneyrabbit.data.MoneyRabbitGameRunInfo;
import com.jjg.game.slots.game.moneyrabbit.manager.MoneyRabbitGameManager;
import com.jjg.game.slots.game.moneyrabbit.manager.MoneyRabbitRoomGameManager;
import com.jjg.game.slots.game.moneyrabbit.manager.MoneyRabbitSendMessageManager;
import com.jjg.game.slots.game.moneyrabbit.pb.ReqMoneyRabbitEnterGame;
import com.jjg.game.slots.game.moneyrabbit.pb.ReqMoneyRabbitPool;
import com.jjg.game.slots.game.moneyrabbit.pb.ReqMoneyRabbitStartGame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@MessageType(MessageConst.MessageTypeDef.MONEY_RABBIT)
public class MoneyRabbitMessageHandler {
    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private MoneyRabbitGameManager gameManager;
    @Autowired
    private MoneyRabbitRoomGameManager roomGameManager;
    @Autowired
    private MoneyRabbitSendMessageManager sendMessageManager;


    /**
     * 请求配置信息
     *
     * @param playerController
     * @param req
     */
    @Command(MoneyRabbitConstant.MsgBean.REQ_ENTER_GAME)
    public void reqConfigInfo(PlayerController playerController, ReqMoneyRabbitEnterGame req) {
        try {
            log.info("收到玩家请求配置 playerId={},req={}", playerController.playerId(), JSONObject.toJSONString(req));
            MoneyRabbitGameRunInfo gameRunInfo;
            if (playerController.getScene() == null) {
                gameRunInfo = gameManager.enterGame(playerController);
            } else if (playerController.getScene() instanceof SlotsRoomController) {
                gameRunInfo = roomGameManager.enterGame(playerController);
            } else {
                log.warn("playerController.getScene() is error, scene={}", playerController.getScene());
                return;
            }
            sendMessageManager.sendConfigMessage(playerController, gameRunInfo);
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
    @Command(MoneyRabbitConstant.MsgBean.REQ_START_GAME)
    public void reqStartGame(PlayerController playerController, ReqMoneyRabbitStartGame req) {
        try {
            log.info("收到玩家开始游戏 playerId={},req={}", playerController.playerId(), JSONObject.toJSONString(req));
            MoneyRabbitGameRunInfo gameRunInfo;
            if (playerController.getScene() == null) {
                gameRunInfo = gameManager.playerStartGame(playerController, req.stakeVlue);
            } else if (playerController.getScene() instanceof SlotsRoomController) {
                gameRunInfo = roomGameManager.playerStartGame(playerController, req.stakeVlue);
            } else {
                log.warn("playerController.getScene() is error, scene={}", playerController.getScene());
                return;
            }
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
    @Command(MoneyRabbitConstant.MsgBean.REQ_POOL_VALUE)
    public void reqPoolValue(PlayerController playerController, ReqMoneyRabbitPool req) {
        try {
            log.info("收到获取奖池 playerId={},req={}", playerController.playerId(), JSONObject.toJSONString(req));
            MoneyRabbitGameRunInfo gameRunInfo;
            if (playerController.getScene() == null) {
                gameRunInfo = gameManager.getPoolValue(playerController, req.stakeVlue);
            } else if (playerController.getScene() instanceof SlotsRoomController) {
                gameRunInfo = roomGameManager.getPoolValue(playerController, req.stakeVlue);
            } else {
                log.warn("playerController.getScene() is error, scene={}", playerController.getScene());
                return;
            }
            sendMessageManager.sendPoolValue(playerController, gameRunInfo);
        } catch (Exception e) {
            log.error("", e);
        }
    }
}
