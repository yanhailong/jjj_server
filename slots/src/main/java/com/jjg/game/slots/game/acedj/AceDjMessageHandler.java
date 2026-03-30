package com.jjg.game.slots.game.acedj;

import com.alibaba.fastjson.JSONObject;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.slots.controller.SlotsRoomController;
import com.jjg.game.slots.game.acedj.data.AceDjGameRunInfo;
import com.jjg.game.slots.game.acedj.manager.AceDjGameManager;
import com.jjg.game.slots.game.acedj.manager.AceDjRoomGameManager;
import com.jjg.game.slots.game.acedj.manager.AceDjSendMessageManager;
import com.jjg.game.slots.game.acedj.pb.ReqAceDjEnterGame;
import com.jjg.game.slots.game.acedj.pb.ReqAceDjPoolInfo;
import com.jjg.game.slots.game.acedj.pb.ReqAceDjStartGame;
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
public class AceDjMessageHandler {
    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private AceDjGameManager gameManager;
    @Autowired
    private AceDjRoomGameManager roomGameManager;
    @Autowired
    private AceDjSendMessageManager sendMessageManager;

    /**
     * 请求配置信息
     *
     * @param playerController
     * @param req
     */
    @Command(AceDjConstant.MsgBean.REQ_CONFIG_INFO)
    public void reqConfigInfo(PlayerController playerController, ReqAceDjEnterGame req) {
        try {
            log.info("收到玩家请求配置 playerId={}", playerController.playerId());
            AceDjGameRunInfo gameRunInfo;
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
    @Command(AceDjConstant.MsgBean.REQ_START_GAME)
    public void reqStartGame(PlayerController playerController, ReqAceDjStartGame req) {
        try {
            log.info("收到玩家开始游戏 playerId={},req={}", playerController.playerId(), JSONObject.toJSONString(req));
            AceDjGameRunInfo gameRunInfo;
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
     * 获取奖池
     *
     * @param playerController
     * @param req
     */
    @Command(AceDjConstant.MsgBean.REQ_POOL_INFO)
    public void reqGetPoolInfo(PlayerController playerController, ReqAceDjPoolInfo req) {
        try {
            log.info("收到获取奖池 playerId={},req={}", playerController.playerId(), JSONObject.toJSONString(req));
            AceDjGameRunInfo gameRunInfo;
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
