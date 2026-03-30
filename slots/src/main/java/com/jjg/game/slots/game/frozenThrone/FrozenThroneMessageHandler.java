package com.jjg.game.slots.game.frozenThrone;

import com.alibaba.fastjson.JSONObject;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.slots.controller.SlotsRoomController;
import com.jjg.game.slots.game.frozenThrone.data.FrozenThroneGameRunInfo;
import com.jjg.game.slots.game.frozenThrone.manager.FrozenThroneGameManager;
import com.jjg.game.slots.game.frozenThrone.manager.FrozenThroneRoomGameManager;
import com.jjg.game.slots.game.frozenThrone.manager.FrozenThroneSendMessageManager;
import com.jjg.game.slots.game.frozenThrone.pb.ReqFrozenThroneEnterGame;
import com.jjg.game.slots.game.frozenThrone.pb.ReqFrozenThronePoolInfo;
import com.jjg.game.slots.game.frozenThrone.pb.ReqFrozenThroneStartGame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author lihaocao
 * @date 2025/12/2 17:37
 */
@Component
@MessageType(MessageConst.MessageTypeDef.FROZEN_THRONE)
public class FrozenThroneMessageHandler {
    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private FrozenThroneGameManager gameManager;
    @Autowired
    private FrozenThroneRoomGameManager roomGameManager;
    @Autowired
    private FrozenThroneSendMessageManager sendMessageManager;

    /**
     * 请求配置信息
     *
     * @param playerController
     * @param req
     */
    @Command(FrozenThroneConstant.MsgBean.REQ_CONFIG_INFO)
    public void reqConfigInfo(PlayerController playerController, ReqFrozenThroneEnterGame req) {
        try {
            log.info("收到玩家请求配置 playerId={}", playerController.playerId());
            FrozenThroneGameRunInfo gameRunInfo;
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
    @Command(FrozenThroneConstant.MsgBean.REQ_START_GAME)
    public void reqStartGame(PlayerController playerController, ReqFrozenThroneStartGame req) {
        try {
            log.info("收到玩家开始游戏 playerId={},req={}", playerController.playerId(), JSONObject.toJSONString(req));
            FrozenThroneGameRunInfo gameRunInfo;
            if (playerController.getScene() == null) {
                gameRunInfo = this.gameManager.playerStartGame(playerController, req.stakeVlue);
            } else if (playerController.getScene() instanceof SlotsRoomController) {
                gameRunInfo = this.roomGameManager.playerStartGame(playerController, req.stakeVlue);
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
     * 获取奖池
     *
     * @param playerController
     * @param req
     */
    @Command(FrozenThroneConstant.MsgBean.REQ_POOL_INFO)
    public void reqGetPoolInfo(PlayerController playerController, ReqFrozenThronePoolInfo req) {
        try {
            log.info("收到获取奖池 playerId={},req={}", playerController.playerId(), JSONObject.toJSONString(req));
            FrozenThroneGameRunInfo gameRunInfo;
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
