package com.jjg.game.slots.game.wealthgod;


import com.alibaba.fastjson.JSONObject;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.slots.controller.SlotsRoomController;
import com.jjg.game.slots.game.wealthgod.data.WealthGodGameRunInfo;
import com.jjg.game.slots.game.wealthgod.manager.WealthGodGameManager;
import com.jjg.game.slots.game.wealthgod.manager.WealthGodRoomGameManager;
import com.jjg.game.slots.game.wealthgod.manager.WealthGodSendMessageManager;
import com.jjg.game.slots.game.wealthgod.pb.req.ReqWealthGodConfigInfo;
import com.jjg.game.slots.game.wealthgod.pb.req.ReqWealthGodPoolValue;
import com.jjg.game.slots.game.wealthgod.pb.req.ReqWealthGodStartGame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 财神
 */
@Component
@MessageType(MessageConst.MessageTypeDef.WEALTH_GOD)
public class WealthGodMessageHandler {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final WealthGodGameManager gameManager;
    private final WealthGodRoomGameManager roomGameManager;
    private final WealthGodSendMessageManager sendMessageManager;

    public WealthGodMessageHandler(WealthGodGameManager gameManager, WealthGodRoomGameManager roomGameManager, WealthGodSendMessageManager sendMessageManager) {
        this.gameManager = gameManager;
        this.roomGameManager = roomGameManager;
        this.sendMessageManager = sendMessageManager;
    }

    /**
     * 请求配置信息
     */
    @Command(WealthGodConstant.MsgBean.REQ_CONFIG_INFO)
    public void reqConfigInfo(PlayerController playerController, ReqWealthGodConfigInfo req) {
        try {
            log.info("收到玩家请求配置 playerId={},req={}", playerController.playerId(), JSONObject.toJSONString(req));
            WealthGodGameRunInfo gameRunInfo;
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
    @Command(WealthGodConstant.MsgBean.REQ_START_GAME)
    public void reqStartGame(PlayerController playerController, ReqWealthGodStartGame req) {
        try {
            WealthGodGameRunInfo gameRunInfo;
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
    @Command(WealthGodConstant.MsgBean.REQ_POOL_VALUE)
    public void reqPoolValue(PlayerController playerController, ReqWealthGodPoolValue req) {
        try {
//            log.info("收到获取奖池 playerId={},req={}", playerController.playerId(), JSONObject.toJSONString(req));
            long poolValue;
            if(playerController.getScene() == null){
                poolValue = gameManager.getPoolValue(playerController);
            }else if(playerController.getScene() instanceof SlotsRoomController){
                poolValue = roomGameManager.getPoolValue(playerController);
            }else {
                log.warn("playerController.getScene() is error, scene={}",playerController.getScene());
                return;
            }
            sendMessageManager.sendPoolValue(playerController, poolValue);
        } catch (Exception e) {
            log.error("", e);
        }
    }

}
