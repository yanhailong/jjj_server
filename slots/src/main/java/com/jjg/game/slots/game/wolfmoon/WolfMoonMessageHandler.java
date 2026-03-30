package com.jjg.game.slots.game.wolfmoon;

import com.alibaba.fastjson.JSONObject;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.slots.controller.SlotsRoomController;
import com.jjg.game.slots.game.wolfmoon.data.WolfMoonGameRunInfo;
import com.jjg.game.slots.game.wolfmoon.manager.WolfMoonGameManager;
import com.jjg.game.slots.game.wolfmoon.manager.WolfMoonRoomGameManager;
import com.jjg.game.slots.game.wolfmoon.manager.WolfMoonSendMessageManager;
import com.jjg.game.slots.game.wolfmoon.pb.req.ReqWolfMoonConfigInfo;
import com.jjg.game.slots.game.wolfmoon.pb.req.ReqWolfMoonFreeChooseOne;
import com.jjg.game.slots.game.wolfmoon.pb.req.ReqWolfMoonPoolValue;
import com.jjg.game.slots.game.wolfmoon.pb.req.ReqWolfMoonStartGame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author 11
 * @date 2025/2/27 15:43
 */
@Component
@MessageType(MessageConst.MessageTypeDef.WOLF_MOON)
public class WolfMoonMessageHandler {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final WolfMoonGameManager gameManager;
    private final WolfMoonRoomGameManager roomGameManager;
    private final WolfMoonSendMessageManager sendMessageManager;

    public WolfMoonMessageHandler(WolfMoonGameManager gameManager, WolfMoonRoomGameManager roomGameManager, WolfMoonSendMessageManager sendMessageManager) {
        this.gameManager = gameManager;
        this.roomGameManager = roomGameManager;
        this.sendMessageManager = sendMessageManager;
    }

    /**
     * 请求配置信息
     */
    @Command(WolfMoonConstant.MsgBean.REQ_ENTER_GAME)
    public void reqConfigInfo(PlayerController playerController, ReqWolfMoonConfigInfo req) {
        try {
            log.info("收到玩家请求配置 playerId={},req={}", playerController.playerId(), JSONObject.toJSONString(req));
            WolfMoonGameRunInfo gameRunInfo;
            if(playerController.getScene() == null){
                gameRunInfo = gameManager.enterGame(playerController);
            }else if(playerController.getScene() instanceof SlotsRoomController){
                gameRunInfo = roomGameManager.enterGame(playerController);
            }else {
                log.warn("playerController.getScene() is error, scene={}",playerController.getScene());
                return;
            }
            sendMessageManager.sendConfigMessage(playerController, gameRunInfo);
        } catch (Exception e) {
            log.error("", e);
        }
    }

    /**
     * 开始游戏
     */
    @Command(WolfMoonConstant.MsgBean.REQ_START_GAME)
    public void reqStartGame(PlayerController playerController, ReqWolfMoonStartGame req) {
        try {
            WolfMoonGameRunInfo gameRunInfo;
            if(playerController.getScene() == null){
                gameRunInfo = gameManager.playerStartGame(playerController, req.stakeValue);
            }else if(playerController.getScene() instanceof SlotsRoomController){
                gameRunInfo = roomGameManager.playerStartGame(playerController, req.stakeValue);
            }else {
                log.warn("playerController.getScene() is error, scene={}",playerController.getScene());
                return;
            }
            log.info("收到玩家开始游戏 playerId={},req={}, gameRunInfo = {}", playerController.playerId(), JSONObject.toJSONString(req), JSONObject.toJSONString(gameRunInfo));
            sendMessageManager.sendStartGameMessage(playerController, gameRunInfo);
        } catch (Exception e) {
            log.error("", e);
        }
    }

    /**
     * 奖池
     */
    @Command(WolfMoonConstant.MsgBean.REQ_POOL_VALUE)
    public void reqPoolValue(PlayerController playerController, ReqWolfMoonPoolValue req) {
        try {
            WolfMoonGameRunInfo gameRunInfo;
            if(playerController.getScene() == null){
                gameRunInfo = gameManager.getPoolValue(playerController, req.stakeValue);
            }else if(playerController.getScene() instanceof SlotsRoomController){
                gameRunInfo = roomGameManager.getPoolValue(playerController, req.stakeValue);
            }else {
                log.warn("playerController.getScene() is error, scene={}",playerController.getScene());
                return;
            }
            sendMessageManager.sendPoolValue(playerController, gameRunInfo);
        } catch (Exception e) {
            log.error("", e);
        }
    }

    /**
     * 免费游戏选择
     */
    @Command(WolfMoonConstant.MsgBean.REQ_FREE_CHOOSE_ONE)
    public void reqFreeChooseOne(PlayerController playerController, ReqWolfMoonFreeChooseOne req) {
        try {
            log.info("收到玩家选择免费游戏类型 playerId={}, freeGameType={}", playerController.playerId(), req.freeGameType);
//            WolfMoonGameRunInfo gameRunInfo;
//            if(playerController.getScene() == null){
//                gameRunInfo = gameManager.chooseFreeGameType(playerController, req.freeGameType);
//            }else if(playerController.getScene() instanceof SlotsRoomController){
//                gameRunInfo = roomGameManager.chooseFreeGameType(playerController, req.freeGameType);
//            }else {
//                log.warn("playerController.getScene() is error, scene={}",playerController.getScene());
//                return;
//            }
//            sendMessageManager.sendFreeChooseOneMessage(playerController, gameRunInfo);
        } catch (Exception e) {
            log.error("", e);
        }
    }
}
