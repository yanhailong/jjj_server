package com.jjg.game.slots.game.mahjiongwin;

import com.alibaba.fastjson.JSONObject;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.slots.game.mahjiongwin.data.MahjiongWinGameRunInfo;
import com.jjg.game.slots.game.mahjiongwin.manager.MahjiongWinGameManager;
import com.jjg.game.slots.game.mahjiongwin.manager.MahjiongWinSendMessageManager;
import com.jjg.game.slots.game.mahjiongwin.pb.ReqMahjiongwinEnterGame;
import com.jjg.game.slots.game.mahjiongwin.pb.ReqMahjiongwinStartGame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author 11
 * @date 2025/8/1 17:37
 */
@Component
@MessageType(MessageConst.MessageTypeDef.MAHJIONG_WIN_TYPE)
public class MahjiongWinMessageHandler {
    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private MahjiongWinGameManager gameManager;
    @Autowired
    private MahjiongWinSendMessageManager sendMessageManager;

    /**
     * 请求配置信息
     *
     * @param playerController
     * @param req
     */
    @Command(MahjiongWinConstant.MsgBean.REQ_CONFIG_INFO)
    public void reqConfigInfo(PlayerController playerController, ReqMahjiongwinEnterGame req) {
        try {
            log.info("收到玩家请求配置 playerId={}", playerController.playerId());
            sendMessageManager.sendConfigMessage(playerController);
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
    @Command(MahjiongWinConstant.MsgBean.REQ_START_GAME)
    public void reqStartGame(PlayerController playerController, ReqMahjiongwinStartGame req) {
        try {
            log.info("收到玩家开始游戏 playerId={},req={}", playerController.playerId(), JSONObject.toJSONString(req));
            MahjiongWinGameRunInfo gameRunInfo = this.gameManager.playerStartGame(playerController, req.stakeVlue);
            sendMessageManager.sendStartGameMessage(playerController, gameRunInfo);
        } catch (Exception e) {
            log.error("", e);
        }
    }
}
