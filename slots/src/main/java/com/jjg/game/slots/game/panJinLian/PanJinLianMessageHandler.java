package com.jjg.game.slots.game.panJinLian;

import com.alibaba.fastjson.JSONObject;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.slots.controller.SlotsRoomController;
import com.jjg.game.slots.game.panJinLian.data.PanJinLianGameRunInfo;
import com.jjg.game.slots.game.panJinLian.manager.PanJinLianGameManager;
import com.jjg.game.slots.game.panJinLian.manager.PanJinLianRoomGameManager;
import com.jjg.game.slots.game.panJinLian.manager.PanJinLianSendMessageManager;
import com.jjg.game.slots.game.panJinLian.pb.ReqPanJinLianEnterGame;
import com.jjg.game.slots.game.panJinLian.pb.ReqPanJinLianPoolInfo;
import com.jjg.game.slots.game.panJinLian.pb.ReqPanJinLianStartGame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author 11
 * @date 2026/3/2
 */
@Component
@MessageType(MessageConst.MessageTypeDef.PAN_JIN_LIAN)
public class PanJinLianMessageHandler {
    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private PanJinLianGameManager gameManager;
    @Autowired
    private PanJinLianRoomGameManager roomGameManager;
    @Autowired
    private PanJinLianSendMessageManager sendMessageManager;

    /**
     * 请求配置信息
     *
     * @param playerController
     * @param req
     */
    @Command(PanJinLianConstant.MsgBean.REQ_CONFIG_INFO)
    public void reqConfigInfo(PlayerController playerController, ReqPanJinLianEnterGame req) {
        try {
            log.info("收到玩家请求配置 playerId={},req={}", playerController.playerId(), JSONObject.toJSONString(req));
            PanJinLianGameRunInfo gameRunInfo;
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
    @Command(PanJinLianConstant.MsgBean.REQ_START_GAME)
    public void reqStartGame(PlayerController playerController, ReqPanJinLianStartGame req) {
        try {
            log.info("收到玩家开始游戏 playerId={},req={}", playerController.playerId(), JSONObject.toJSONString(req));
            PanJinLianGameRunInfo gameRunInfo;
            if (playerController.getScene() == null) {
                gameRunInfo = gameManager.playerStartGame(playerController, req.stakeValue);
            } else if (playerController.getScene() instanceof SlotsRoomController) {
                gameRunInfo = roomGameManager.playerStartGame(playerController, req.stakeValue);
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
    @Command(PanJinLianConstant.MsgBean.REQ_POOL_INFO)
    public void reqPoolValue(PlayerController playerController, ReqPanJinLianPoolInfo req) {
        try {
//            log.info("收到获取奖池 playerId={},req={}", playerController.playerId(), JSONObject.toJSONString(req));
            PanJinLianGameRunInfo gameRunInfo;
            if (playerController.getScene() == null) {
                gameRunInfo = gameManager.getPoolValue(playerController, req.stakeValue);
            } else if (playerController.getScene() instanceof SlotsRoomController) {
                gameRunInfo = roomGameManager.getPoolValue(playerController, req.stakeValue);
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
