package com.jjg.game.slots.game.bountyduel;

import com.alibaba.fastjson.JSONObject;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.slots.controller.SlotsRoomController;
import com.jjg.game.slots.game.bountyduel.data.BountyDuelGameRunInfo;
import com.jjg.game.slots.game.bountyduel.manager.BountyDuelGameManager;
import com.jjg.game.slots.game.bountyduel.manager.BountyDuelRoomGameManager;
import com.jjg.game.slots.game.bountyduel.manager.BountyDuelSendMessageManager;
import com.jjg.game.slots.game.bountyduel.pb.ReqBountyDuelEnterGame;
import com.jjg.game.slots.game.bountyduel.pb.ReqBountyDuelStartGame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 赏金大对决消息入口。
 */
@Component
@MessageType(MessageConst.MessageTypeDef.BOUNTY_DUEL_TYPE)
public class BountyDuelMessageHandler {
    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private BountyDuelGameManager gameManager;
    @Autowired
    private BountyDuelRoomGameManager roomGameManager;
    @Autowired
    private BountyDuelSendMessageManager sendMessageManager;

    @Command(BountyDuelConstant.MsgBean.REQ_CONFIG_INFO)
    public void reqConfigInfo(PlayerController playerController, ReqBountyDuelEnterGame req) {
        try {
            log.info("收到赏金大对决配置请求 playerId={}", playerController.playerId());

            BountyDuelGameRunInfo gameRunInfo;
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

    @Command(BountyDuelConstant.MsgBean.REQ_START_GAME)
    public void reqStartGame(PlayerController playerController, ReqBountyDuelStartGame req) {
        try {
            log.info("收到赏金大对决开始游戏请求 playerId={}, req={}", playerController.playerId(), JSONObject.toJSONString(req));
            BountyDuelGameRunInfo gameRunInfo;
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
}
