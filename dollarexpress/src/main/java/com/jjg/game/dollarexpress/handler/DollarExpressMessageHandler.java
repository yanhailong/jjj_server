package com.jjg.game.dollarexpress.handler;

import com.alibaba.fastjson.JSONObject;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.dollarexpress.constant.DollarExpressConst;
import com.jjg.game.dollarexpress.data.GameRunInfo;
import com.jjg.game.dollarexpress.manager.DollarExpressManager;
import com.jjg.game.dollarexpress.manager.SendMessageManager;
import com.jjg.game.dollarexpress.pb.ReqChooseWare;
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
@MessageType(DollarExpressConst.MSGBEAN.TYPE)
public class DollarExpressMessageHandler {
    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private DollarExpressManager dollarExpressManager;
    @Autowired
    private SendMessageManager sendMessageManager;

    /**
     * 选择游戏场次进入
     * @param playerController
     * @param req
     */
    @Command(DollarExpressConst.MSGBEAN.REQ_CHOOSE_WARE)
    public void reqChooseWare(PlayerController playerController, ReqChooseWare req){
        try{
            log.info("收到玩家选择游戏场次 playerId={},req={}",playerController.playerId(), JSONObject.toJSONString(req));
            GameRunInfo gameRunInfo = dollarExpressManager.chooseWare(playerController, req.wareId);
            sendMessageManager.sendChooseWareMessage(playerController,gameRunInfo);
        }catch (Exception e){
            log.error("", e);
        }
    }

    /**
     * 开始游戏
     * @param playerController
     * @param req
     */
    @Command(DollarExpressConst.MSGBEAN.REQ_START_GAME)
    public void reqEnterGame(PlayerController playerController, ReqStartGame req){
        try{
            log.info("收到玩家开始游戏 playerId={},req={}",playerController.playerId(), JSONObject.toJSONString(req));
            GameRunInfo gameRunInfo = dollarExpressManager.startGame(playerController.playerId(), req.stakeVlue);
            sendMessageManager.sendStartGameMessage(playerController,gameRunInfo);
        }catch (Exception e){
            log.error("", e);
        }
    }
}
