package com.jjg.game.slots.game.wealthbank;

import com.alibaba.fastjson.JSONObject;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.slots.controller.SlotsRoomController;
import com.jjg.game.slots.game.thor.data.ThorGameRunInfo;
import com.jjg.game.slots.game.wealthbank.data.WealthBankGameRunInfo;
import com.jjg.game.slots.game.wealthbank.manager.WealthBankGameManager;
import com.jjg.game.slots.game.wealthbank.manager.WealthBankRoomGameManager;
import com.jjg.game.slots.game.wealthbank.manager.WealthBankSendMessageManager;
import com.jjg.game.slots.game.wealthbank.pb.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author 11
 * @date 2025/6/12 17:15
 */
@Component
@MessageType(MessageConst.MessageTypeDef.WEALTH_BANK)
public class WealthBankMessageHandler {
    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private WealthBankGameManager gameManager;
    @Autowired
    private WealthBankRoomGameManager roomGameManager;
    @Autowired
    private WealthBankSendMessageManager sendMessageManager;


    /**
     * 请求配置信息
     *
     * @param playerController
     * @param req
     */
    @Command(WealthBankConstant.MsgBean.REQ_WEALTH_BANK_CONFIG_INFO)
    public void reqWealthBankConfigInfo(PlayerController playerController, ReqWealthBankConfigInfo req) {
        try {
            log.info("[Wealth Bank] 收到玩家请求配置 playerId={},req={}",
                    playerController.playerId(), JSONObject.toJSONString(req));
            WealthBankGameRunInfo gameRunInfo;
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
            log.error("[Wealth Bank] reqWealthBankConfigInfo 异常", e);
        }
    }

    @Command(WealthBankConstant.MsgBean.REQ_WEALTH_BANK_START_GAME)
    public void reqWealthBankStartGame(PlayerController playerController, ReqWealthBankStartGame req) {
        try {
            log.info("[Wealth Bank] 收到玩家开始游戏 playerId={},req={}",
                    playerController.playerId(), JSONObject.toJSONString(req));
            WealthBankGameRunInfo gameRunInfo;
            if(playerController.getScene() == null){
                System.out.println(11);
                gameRunInfo = gameManager.playerStartGame(playerController, req.stakeVlue);
            }else if(playerController.getScene() instanceof SlotsRoomController){
                System.out.println(22);
                gameRunInfo = roomGameManager.playerStartGame(playerController, req.stakeVlue);
            }else {
                log.warn("playerController.getScene() is error, scene={}",playerController.getScene());
                return;
            }
            sendMessageManager.sendStartGameMessage(playerController, gameRunInfo);
        } catch (Exception e) {
            log.error("[Wealth Bank] reqWealthBankStartGame 异常", e);
        }
    }

    @Command(WealthBankConstant.MsgBean.REQ_WEALTH_BANK_CHOOSE_FREE_MODEL)
    public void reqWealthBankChooseFreeModel(PlayerController playerController, ReqWealthBankChooseFreeModel req) {
        try {
            log.info("[Wealth Bank] 收到选择免费游戏类型 playerId={},req={}",
                    playerController.playerId(), JSONObject.toJSONString(req));
            WealthBankGameRunInfo gameRunInfo;
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
            log.error("[Wealth Bank] reqWealthBankChooseFreeModel 异常", e);
        }
    }

    @Command(WealthBankConstant.MsgBean.REQ_WEALTH_BANK_INVEST_AREA)
    public void reqWealthBankInvestArea(PlayerController playerController, ReqWealthBankInvestArea req) {
        try {
            log.info("[Wealth Bank] 收到选择投资游戏 playerId={},req={}",
                    playerController.playerId(), JSONObject.toJSONString(req));
            WealthBankGameRunInfo gameRunInfo;
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
            log.error("[Wealth Bank] reqWealthBankInvestArea 异常", e);
        }
    }

    @Command(WealthBankConstant.MsgBean.REQ_WEALTH_BANK_POOL_VALUE)
    public void reqWealthBankPoolValue(PlayerController playerController, ReqWealthBankPoolValue req) {
        try {
            log.info("[Wealth Bank] 收到获取奖池 playerId={},req={}",
                    playerController.playerId(), JSONObject.toJSONString(req));
            WealthBankGameRunInfo gameRunInfo;
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
            log.error("[Wealth Bank] reqWealthBankPoolValue 异常", e);
        }
    }
}
