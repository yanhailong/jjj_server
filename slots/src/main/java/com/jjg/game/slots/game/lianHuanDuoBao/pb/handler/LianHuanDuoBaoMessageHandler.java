package com.jjg.game.slots.game.lianHuanDuoBao.pb.handler;

import com.alibaba.fastjson.JSONObject;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.slots.controller.SlotsRoomController;
import com.jjg.game.slots.game.lianHuanDuoBao.constant.LianHuanDuoBaoConstant;
import com.jjg.game.slots.game.lianHuanDuoBao.data.LianHuanDuoBaoGameRunInfo;
import com.jjg.game.slots.game.lianHuanDuoBao.manager.LianHuanDuoBaoGameManager;
import com.jjg.game.slots.game.lianHuanDuoBao.manager.LianHuanDuoBaoGameSendMessageManager;
import com.jjg.game.slots.game.lianHuanDuoBao.manager.LianHuanDuoBaoRoomGameManager;
import com.jjg.game.slots.game.lianHuanDuoBao.pb.req.ReqLianHuanDuoBaoBonusStart;
import com.jjg.game.slots.game.lianHuanDuoBao.pb.req.ReqLianHuanDuoBaoEnterGame;
import com.jjg.game.slots.game.lianHuanDuoBao.pb.req.ReqLianHuanDuoBaoPoolValue;
import com.jjg.game.slots.game.lianHuanDuoBao.pb.req.ReqLianHuanDuoBaoStartGame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author lm
 * @date 2026/6/2
 */
//TODO 连环夺宝配表完成后取消注释即可启用
//@Component
//@MessageType(MessageConst.MessageTypeDef.LIAN_HUAN_DUO_BAO_TYPE)
public class LianHuanDuoBaoMessageHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final LianHuanDuoBaoGameManager gameManager;
    private final LianHuanDuoBaoRoomGameManager roomGameManager;
    private final LianHuanDuoBaoGameSendMessageManager sendMessageManager;

    public LianHuanDuoBaoMessageHandler(LianHuanDuoBaoGameManager gameManager,
                                        LianHuanDuoBaoRoomGameManager roomGameManager,
                                        LianHuanDuoBaoGameSendMessageManager sendMessageManager) {
        this.gameManager = gameManager;
        this.roomGameManager = roomGameManager;
        this.sendMessageManager = sendMessageManager;
    }

    @Command(LianHuanDuoBaoConstant.MsgBean.REQ_ENTER_GAME)
    public void reqEnterGame(PlayerController playerController, ReqLianHuanDuoBaoEnterGame req) {
        try {
            log.info("收到玩家进入连环夺宝 playerId={}", playerController.playerId());
            LianHuanDuoBaoGameRunInfo gameRunInfo;
            if (playerController.getScene() == null) {
                gameRunInfo = gameManager.enterGame(playerController);
            } else if (playerController.getScene() instanceof SlotsRoomController) {
                gameRunInfo = roomGameManager.enterGame(playerController);
            } else {
                log.warn("playerController.getScene() is error, scene={}", playerController.getScene());
                return;
            }
            sendMessageManager.reqEnterGame(playerController, gameRunInfo);
        } catch (Exception e) {
            log.error("", e);
        }
    }

    @Command(LianHuanDuoBaoConstant.MsgBean.REQ_START_GAME)
    public void reqStartGame(PlayerController playerController, ReqLianHuanDuoBaoStartGame req) {
        try {
            log.info("收到玩家开始连环夺宝 playerId={},req={}", playerController.playerId(), JSONObject.toJSONString(req));
            LianHuanDuoBaoGameRunInfo gameRunInfo;
            if (playerController.getScene() == null) {
                gameRunInfo = gameManager.playerStartGame(playerController, req.stakeValue);
            } else if (playerController.getScene() instanceof SlotsRoomController) {
                gameRunInfo = roomGameManager.playerStartGame(playerController, req.stakeValue);
            } else {
                log.warn("playerController.getScene() is error, scene={}", playerController.getScene());
                return;
            }
            sendMessageManager.reqStartGame(playerController, gameRunInfo);
        } catch (Exception e) {
            log.error("", e);
        }
    }

    @Command(LianHuanDuoBaoConstant.MsgBean.REQ_POOL_VALUE)
    public void reqPoolValue(PlayerController playerController, ReqLianHuanDuoBaoPoolValue req) {
        try {
            LianHuanDuoBaoGameRunInfo gameRunInfo;
            if (playerController.getScene() == null) {
                gameRunInfo = gameManager.getPoolValue(playerController, req.stakeValue);
            } else if (playerController.getScene() instanceof SlotsRoomController) {
                gameRunInfo = roomGameManager.getPoolValue(playerController, req.stakeValue);
            } else {
                log.warn("playerController.getScene() is error, scene={}", playerController.getScene());
                return;
            }
            sendMessageManager.sendPoolMessage(playerController, gameRunInfo);
        } catch (Exception e) {
            log.error("", e);
        }
    }

    @Command(LianHuanDuoBaoConstant.MsgBean.REQ_BONUS_START)
    public void reqBonusStart(PlayerController playerController, ReqLianHuanDuoBaoBonusStart req) {
        try {
            //bonus 小游戏入口（后续阶段实现具体逻辑：龙珠掉落 → 聚宝盆抽奖 → 清空聚宝盆 → 回到第一关）
            log.info("收到玩家开始 bonus 小游戏 playerId={}", playerController.playerId());
            //TODO 阶段 3 实现
        } catch (Exception e) {
            log.error("", e);
        }
    }
}
