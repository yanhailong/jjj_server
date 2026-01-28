package com.jjg.game.slots.game.zeusVsHades;

import com.alibaba.fastjson.JSONObject;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.slots.controller.SlotsRoomController;
import com.jjg.game.slots.game.zeusVsHades.data.ZeusVsHadesGameRunInfo;
import com.jjg.game.slots.game.zeusVsHades.manager.ZeusVsHadesGameManager;
import com.jjg.game.slots.game.zeusVsHades.manager.ZeusVsHadesRoomGameManager;
import com.jjg.game.slots.game.zeusVsHades.manager.ZeusVsHadesSendMessageManager;
import com.jjg.game.slots.game.zeusVsHades.pb.ReqZeusVsHadesEnterGame;
import com.jjg.game.slots.game.zeusVsHades.pb.ReqZeusVsHadesFreeChooseOne;
import com.jjg.game.slots.game.zeusVsHades.pb.ReqZeusVsHadesPoolInfo;
import com.jjg.game.slots.game.zeusVsHades.pb.ReqZeusVsHadesStartGame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author lihaocao
 * @date 2025/12/2 17:37
 */
@Component
@MessageType(MessageConst.MessageTypeDef.ZEUS_VS_HADES)
public class ZeusVsHadesMessageHandler {
    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private ZeusVsHadesGameManager gameManager;
    @Autowired
    private ZeusVsHadesRoomGameManager roomGameManager;
    @Autowired
    private ZeusVsHadesSendMessageManager sendMessageManager;

    /**
     * 请求配置信息
     *
     * @param playerController
     * @param req
     */
    @Command(ZeusVsHadesConstant.MsgBean.REQ_CONFIG_INFO)
    public void reqConfigInfo(PlayerController playerController, ReqZeusVsHadesEnterGame req) {
        try {
            log.info("收到玩家请求配置 playerId={}", playerController.playerId());
            ZeusVsHadesGameRunInfo gameRunInfo;
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
    @Command(ZeusVsHadesConstant.MsgBean.REQ_START_GAME)
    public void reqStartGame(PlayerController playerController, ReqZeusVsHadesStartGame req) {
        try {
            log.info("收到玩家开始游戏 playerId={},req={}", playerController.playerId(), JSONObject.toJSONString(req));
            ZeusVsHadesGameRunInfo gameRunInfo;
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
     * 免费模式二选一
     *
     * @param playerController
     * @param req
     */
    @Command(ZeusVsHadesConstant.MsgBean.REQ_FREE_CHOOSE_ONE)
    public void reqFreeChooseOne(PlayerController playerController, ReqZeusVsHadesFreeChooseOne req) {
        try {
            log.info("收到二选一 playerId={},req={}", playerController.playerId(), JSONObject.toJSONString(req));
            ZeusVsHadesGameRunInfo gameRunInfo;
            if (playerController.getScene() == null) {
                gameRunInfo = gameManager.freeChooseOne(playerController, req.type);
            } else if (playerController.getScene() instanceof SlotsRoomController) {
                gameRunInfo = roomGameManager.freeChooseOne(playerController, req.type);
            } else {
                log.warn("playerController.getScene() is error, scene={}", playerController.getScene());
                return;
            }
            sendMessageManager.sendFreeChooseOneMessage(playerController, gameRunInfo);
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
    @Command(ZeusVsHadesConstant.MsgBean.REQ_POOL_INFO)
    public void reqGetPoolInfo(PlayerController playerController, ReqZeusVsHadesPoolInfo req) {
        try {
            log.info("收到获取奖池 playerId={},req={}", playerController.playerId(), JSONObject.toJSONString(req));
            ZeusVsHadesGameRunInfo gameRunInfo;
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
