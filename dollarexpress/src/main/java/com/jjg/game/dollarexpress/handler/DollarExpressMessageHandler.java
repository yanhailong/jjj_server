package com.jjg.game.dollarexpress.handler;

import com.alibaba.fastjson.JSONObject;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.dollarexpress.constant.DollarExpressConst;
import com.jjg.game.dollarexpress.data.GameRunInfo;
import com.jjg.game.dollarexpress.manager.DollarExpressManager;
import com.jjg.game.dollarexpress.manager.DollarExpressSendMessageManager;
import com.jjg.game.dollarexpress.pb.ReqChooseFreeModel;
import com.jjg.game.dollarexpress.pb.ReqInvestArea;
import com.jjg.game.dollarexpress.pb.ReqStartGame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author 11
 * @date 2025/6/12 17:15
 */
@Component
@MessageType(DollarExpressConst.MsgBean.TYPE)
public class DollarExpressMessageHandler {
    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private DollarExpressManager dollarExpressManager;
    @Autowired
    private DollarExpressSendMessageManager dollarExpressSendMessageManager;

    /**
     * 开始游戏
     * @param playerController
     * @param req
     */
    @Command(DollarExpressConst.MsgBean.REQ_START_GAME)
    public void reqStartGame(PlayerController playerController, ReqStartGame req){
        try{
            log.info("收到玩家开始游戏 playerId={},req={}",playerController.playerId(), JSONObject.toJSONString(req));
            GameRunInfo gameRunInfo = dollarExpressManager.startGame(playerController.playerId(), req.stakeVlue);
            dollarExpressSendMessageManager.sendStartGameMessage(playerController,gameRunInfo);
        }catch (Exception e){
            log.error("", e);
        }
    }

    /**
     * 选择免费游戏类型
     * @param playerController
     * @param req
     */
    @Command(DollarExpressConst.MsgBean.REQ_CHOOSE_FREE_MODEL)
    public void reqChooseFreeModel(PlayerController playerController, ReqChooseFreeModel req){
        try{
            log.info("收到选择免费游戏类型 playerId={},req={}",playerController.playerId(), JSONObject.toJSONString(req));
            GameRunInfo gameRunInfo = dollarExpressManager.chooseFreeGameType(playerController.playerId(), req.type);
            dollarExpressSendMessageManager.sendChooseFreeTypeMessage(playerController,gameRunInfo);
        }catch (Exception e){
            log.error("", e);
        }
    }

    /**
     * 投资游戏
     * @param playerController
     * @param req
     */
    @Command(DollarExpressConst.MsgBean.REQ_INVEST_AREA)
    public void reqInvestArea(PlayerController playerController, ReqInvestArea req){
        try{
            log.info("收到选择投资游戏 playerId={},req={}",playerController.playerId(), JSONObject.toJSONString(req));
            GameRunInfo gameRunInfo = dollarExpressManager.investArea(playerController.playerId(), req.areaId1, req.areaId2, req.areaId3);
            dollarExpressSendMessageManager.sendChooseFreeTypeMessage(playerController,gameRunInfo);
        }catch (Exception e){
            log.error("", e);
        }
    }
}
