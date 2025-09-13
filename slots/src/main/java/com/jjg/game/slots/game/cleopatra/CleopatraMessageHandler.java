package com.jjg.game.slots.game.cleopatra;

import com.alibaba.fastjson.JSONObject;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.listener.GmListener;
import com.jjg.game.slots.game.cleopatra.data.CleopatraGameRunInfo;
import com.jjg.game.slots.game.cleopatra.manager.CleopatraGameManager;
import com.jjg.game.slots.game.cleopatra.manager.CleopatraSendMessageManager;
import com.jjg.game.slots.game.cleopatra.pb.ReqCleopatraEnterGame;
import com.jjg.game.slots.game.cleopatra.pb.ReqCleopatraPool;
import com.jjg.game.slots.game.cleopatra.pb.ReqCleopatraStartGame;
import com.jjg.game.slots.game.dollarexpress.DollarExpressConstant;
import com.jjg.game.slots.game.dollarexpress.data.DollarExpressGameRunInfo;
import com.jjg.game.slots.game.dollarexpress.pb.ReqPoolValue;
import com.jjg.game.slots.game.mahjiongwin.MahjiongWinConstant;
import com.jjg.game.slots.game.mahjiongwin.data.MahjiongWinGameRunInfo;
import com.jjg.game.slots.game.mahjiongwin.manager.MahjiongWinGameManager;
import com.jjg.game.slots.game.mahjiongwin.manager.MahjiongWinSendMessageManager;
import com.jjg.game.slots.game.mahjiongwin.pb.ReqMahjiongwinEnterGame;
import com.jjg.game.slots.game.mahjiongwin.pb.ReqMahjiongwinStartGame;
import com.jjg.game.slots.handler.SlotsMessageHandler;
import com.jjg.game.slots.manager.AbstractSlotsGameManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * @author 11
 * @date 2025/8/26 20:58
 */
@Component
@MessageType(MessageConst.MessageTypeDef.CLEOPATRA)
public class CleopatraMessageHandler extends SlotsMessageHandler {
    @Autowired
    private CleopatraGameManager gameManager;
    @Autowired
    private CleopatraSendMessageManager sendMessageManager;

    /**
     * 请求配置信息
     *
     * @param playerController
     * @param req
     */
    @Command(CleopatraConstant.MsgBean.REQ_CONFIG_INFO)
    public void reqConfigInfo(PlayerController playerController, ReqCleopatraEnterGame req) {
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
    @Command(CleopatraConstant.MsgBean.REQ_START_GAME)
    public void reqStartGame(PlayerController playerController, ReqCleopatraStartGame req) {
        try {
            log.info("收到玩家开始游戏 playerId={},req={}", playerController.playerId(), JSONObject.toJSONString(req));
            CleopatraGameRunInfo gameRunInfo = this.gameManager.playerStartGame(playerController, req.stakeVlue);
            sendMessageManager.sendStartGameMessage(playerController, gameRunInfo);
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
    @Command(CleopatraConstant.MsgBean.REQ_POOL_VALUE)
    public void reqPoolValue(PlayerController playerController, ReqCleopatraPool req) {
        try {
            log.info("收到获取奖池 playerId={},req={}", playerController.playerId(), JSONObject.toJSONString(req));
            DollarExpressGameRunInfo gameRunInfo = gameManager.getPoolValue(playerController, req.stakeVlue);
            sendMessageManager.sendPoolValue(playerController, gameRunInfo);
        } catch (Exception e) {
            log.error("", e);
        }
    }

    @Override
    protected AbstractSlotsGameManager getGameManager() {
        return this.gameManager;
    }

    @Override
    protected Map<Integer, Integer> getGenerateMap(int count) {
        Map<Integer, Integer> countMap = new HashMap<>();
        for (int i = 1; i <= 1; i++) {
            countMap.put(i, count);
        }
        return countMap;
    }
}
