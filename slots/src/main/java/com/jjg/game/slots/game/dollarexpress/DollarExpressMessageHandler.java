package com.jjg.game.slots.game.dollarexpress;

import com.alibaba.fastjson.JSONObject;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.core.data.FriendRoom;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.slots.controller.SlotsRoomController;
import com.jjg.game.slots.game.dollarexpress.data.DollarExpressGameRunInfo;
import com.jjg.game.slots.game.dollarexpress.manager.DollarExpressGameManager;
import com.jjg.game.slots.game.dollarexpress.manager.DollarExpressRoomGameManager;
import com.jjg.game.slots.game.dollarexpress.manager.DollarExpressSendMessageManager;
import com.jjg.game.slots.game.dollarexpress.pb.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author 11
 * @date 2025/6/12 17:15
 */
@Component
@MessageType(MessageConst.MessageTypeDef.DOLLAR_EXPRESS_TYPE)
public class DollarExpressMessageHandler {
    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private DollarExpressGameManager gameManager;
    @Autowired
    private DollarExpressRoomGameManager roomGameManager;
    @Autowired
    private DollarExpressSendMessageManager sendMessageManager;


    /**
     * 请求配置信息
     *
     * @param playerController
     * @param req
     */
    @Command(DollarExpressConstant.MsgBean.REQ_CONFIG_INFO)
    public void reqConfigInfo(PlayerController playerController, ReqConfigInfo req) {
        try {
            log.info("收到玩家请求配置 playerId={},req={}", playerController.playerId(), JSONObject.toJSONString(req));
            DollarExpressGameRunInfo gameRunInfo;
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
     *
     * @param playerController
     * @param req
     */
    @Command(DollarExpressConstant.MsgBean.REQ_START_GAME)
    public void reqStartGame(PlayerController playerController, ReqStartGame req) {
        try {
            log.info("收到玩家开始游戏 playerId={},req={}", playerController.playerId(), JSONObject.toJSONString(req));
            DollarExpressGameRunInfo gameRunInfo;
            if(playerController.getScene() == null){
                gameRunInfo = gameManager.playerStartGame(playerController, req.stakeVlue);
            }else if(playerController.getScene() instanceof SlotsRoomController){
                gameRunInfo = roomGameManager.playerStartGame(playerController, req.stakeVlue);
            }else {
                log.warn("playerController.getScene() is error, scene={}",playerController.getScene());
                return;
            }
            sendMessageManager.sendStartGameMessage(playerController, gameRunInfo);
        } catch (Exception e) {
            log.error("", e);
        }
    }

    /**
     * 选择免费游戏类型
     *
     * @param playerController
     * @param req
     */
    @Command(DollarExpressConstant.MsgBean.REQ_CHOOSE_FREE_MODEL)
    public void reqChooseFreeModel(PlayerController playerController, ReqChooseFreeModel req) {
        try {
            log.info("收到选择免费游戏类型 playerId={},req={}", playerController.playerId(), JSONObject.toJSONString(req));
            DollarExpressGameRunInfo gameRunInfo;
            if(playerController.getScene() == null){
                gameRunInfo = gameManager.playerChooseFreeGameType(playerController, req.status);
            }else if(playerController.getScene() instanceof SlotsRoomController){
                gameRunInfo = roomGameManager.playerChooseFreeGameType(playerController, req.status);
            }else {
                log.warn("playerController.getScene() is error, scene={}",playerController.getScene());
                return;
            }
            sendMessageManager.sendChooseOneMessage(playerController, gameRunInfo);
        } catch (Exception e) {
            log.error("", e);
        }
    }

    /**
     * 投资游戏
     *
     * @param playerController
     * @param req
     */
    @Command(DollarExpressConstant.MsgBean.REQ_INVEST_AREA)
    public void reqInvestArea(PlayerController playerController, ReqInvestArea req) {
        try {
            log.info("收到选择投资游戏 playerId={},req={}", playerController.playerId(), JSONObject.toJSONString(req));
            DollarExpressGameRunInfo gameRunInfo;
            if(playerController.getScene() == null){
                gameRunInfo = gameManager.playerInvest(playerController, req.areaId);
            }else if(playerController.getScene() instanceof SlotsRoomController){
                gameRunInfo = roomGameManager.playerInvest(playerController, req.areaId);
            }else {
                log.warn("playerController.getScene() is error, scene={}",playerController.getScene());
                return;
            }
            sendMessageManager.sendInvers(playerController, gameRunInfo);
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
    @Command(DollarExpressConstant.MsgBean.REQ_POOL_VALUE)
    public void reqPoolValue(PlayerController playerController, ReqPoolValue req) {
        try {
            log.info("收到获取奖池 playerId={},req={}", playerController.playerId(), JSONObject.toJSONString(req));
            DollarExpressGameRunInfo gameRunInfo;
            if(playerController.getScene() == null){
                gameRunInfo = gameManager.getPoolValue(playerController, req.stakeVlue);
            }else if(playerController.getScene() instanceof SlotsRoomController){
                gameRunInfo = roomGameManager.getPoolValue(playerController, req.stakeVlue);
            }else {
                log.warn("playerController.getScene() is error, scene={}",playerController.getScene());
                return;
            }
            sendMessageManager.sendPoolValue(playerController, gameRunInfo);
        } catch (Exception e) {
            log.error("", e);
        }
    }
}
