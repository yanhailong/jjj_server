package com.jjg.game.slots.game.hotfootball;

import com.alibaba.fastjson.JSONObject;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.slots.controller.SlotsRoomController;
import com.jjg.game.slots.game.hotfootball.data.HotFootballGameRunInfo;
import com.jjg.game.slots.game.hotfootball.manager.HotFootballGameManager;
import com.jjg.game.slots.game.hotfootball.manager.HotFootballRoomGameManager;
import com.jjg.game.slots.game.hotfootball.manager.HotFootballSendMessageManager;
import com.jjg.game.slots.game.hotfootball.pb.ReqHotFootballEnterGame;
import com.jjg.game.slots.game.hotfootball.pb.ReqHotFootballStartGame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author 11
 * @date 2025/8/1 17:37
 */
@Component
@MessageType(MessageConst.MessageTypeDef.HOT_FOOTBALL_TYPE)
public class HotFootballMessageHandler {
    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private HotFootballGameManager gameManager;
    @Autowired
    private HotFootballRoomGameManager roomGameManager;
    @Autowired
    private HotFootballSendMessageManager sendMessageManager;

    /**
     * 请求配置信息
     *
     * @param playerController
     * @param req
     */
    @Command(HotFootballConstant.MsgBean.REQ_CONFIG_INFO)
    public void reqConfigInfo(PlayerController playerController, ReqHotFootballEnterGame req) {
        try {
            log.info("收到玩家请求配置 playerId={}", playerController.playerId());

            HotFootballGameRunInfo gameRunInfo;
            if(playerController.getScene() == null){
                gameRunInfo = gameManager.enterGame(playerController);
            }else if(playerController.getScene() instanceof SlotsRoomController){
                gameRunInfo = roomGameManager.enterGame(playerController);
            }else {
                log.warn("playerController.getScene() is error, scene={}",playerController.getScene());
                return;
            }

            sendMessageManager.sendConfigMessage(playerController,gameRunInfo);
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
    @Command(HotFootballConstant.MsgBean.REQ_START_GAME)
    public void reqStartGame(PlayerController playerController, ReqHotFootballStartGame req) {
        try {
            log.info("收到玩家开始游戏 playerId={},req={}", playerController.playerId(), JSONObject.toJSONString(req));
            HotFootballGameRunInfo gameRunInfo;
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
}
