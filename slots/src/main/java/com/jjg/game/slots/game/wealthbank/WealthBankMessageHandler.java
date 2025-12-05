package com.jjg.game.slots.game.wealthbank;

import com.alibaba.fastjson.JSONObject;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.slots.game.wealthbank.data.WealthBankGameRunInfo;
import com.jjg.game.slots.game.wealthbank.manager.WealthBankGameManager;
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
            WealthBankGameRunInfo gameRunInfo = gameManager.enterGame(playerController);
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
            WealthBankGameRunInfo gameRunInfo =
                    this.gameManager.playerStartGame(playerController, req.stakeVlue);
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
            WealthBankGameRunInfo gameRunInfo =
                    gameManager.playerChooseFreeGameType(playerController, req.status);
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
            WealthBankGameRunInfo gameRunInfo =
                    gameManager.playerInvest(playerController, req.areaId);
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
            WealthBankGameRunInfo gameRunInfo =
                    gameManager.getPoolValue(playerController, req.stakeVlue);
            sendMessageManager.sendPoolValue(playerController, gameRunInfo);
        } catch (Exception e) {
            log.error("[Wealth Bank] reqWealthBankPoolValue 异常", e);
        }
    }
}
