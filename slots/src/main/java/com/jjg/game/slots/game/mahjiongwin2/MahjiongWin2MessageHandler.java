package com.jjg.game.slots.game.mahjiongwin2;

import com.alibaba.fastjson.JSONObject;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.slots.controller.SlotsRoomController;
import com.jjg.game.slots.game.mahjiongwin2.data.MahjiongWin2GameRunInfo;
import com.jjg.game.slots.game.mahjiongwin2.manager.MahjiongWin2GameManager;
import com.jjg.game.slots.game.mahjiongwin2.manager.MahjiongWin2RoomGameManager;
import com.jjg.game.slots.game.mahjiongwin2.manager.MahjiongWin2SendMessageManager;
import com.jjg.game.slots.game.mahjiongwin2.pb.ReqMahjiongwin2EnterGame;
import com.jjg.game.slots.game.mahjiongwin2.pb.ReqMahjiongwin2StartGame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author 11
 * @date 2025/8/1 17:37
 */
@Component
@MessageType(MessageConst.MessageTypeDef.MAHJIONG_WIN2_TYPE)
public class MahjiongWin2MessageHandler {
    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private MahjiongWin2GameManager gameManager;
    @Autowired
    private MahjiongWin2RoomGameManager roomGameManager;
    @Autowired
    private MahjiongWin2SendMessageManager sendMessageManager;

    /**
     * 请求配置信息
     *
     * @param playerController
     * @param req
     */
    @Command(MahjiongWin2Constant.MsgBean.REQ_CONFIG_INFO)
    public void reqConfigInfo(PlayerController playerController, ReqMahjiongwin2EnterGame req) {
        try {
            log.info("收到玩家请求配置 playerId={}", playerController.playerId());

            MahjiongWin2GameRunInfo gameRunInfo;
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
    @Command(MahjiongWin2Constant.MsgBean.REQ_START_GAME)
    public void reqStartGame(PlayerController playerController, ReqMahjiongwin2StartGame req) {
        try {
            log.info("收到玩家开始游戏 playerId={},req={}", playerController.playerId(), JSONObject.toJSONString(req));
            MahjiongWin2GameRunInfo gameRunInfo;
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
