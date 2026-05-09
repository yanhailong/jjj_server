package com.jjg.game.ploy.handler;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.listener.ChooseWareListener;
import com.jjg.game.core.listener.GmListener;
import com.jjg.game.core.pb.ReqChooseWare;
import com.jjg.game.core.pb.ResChooseWare;
import com.jjg.game.ploy.constant.PloyConstant;
import com.jjg.game.ploy.constant.PloyGameType;
import com.jjg.game.ploy.controller.AbstractPloyController;
import com.jjg.game.ploy.pb.ReqPloyBet;
import com.jjg.game.ploy.pb.ReqPloyConfig;
import com.jjg.game.ploy.pb.ReqPloyRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author 11
 * @date 2026/3/19
 */
@Component
@MessageType(MessageConst.MessageTypeDef.PLOY_COMMON)
public class PloyMessageHandler implements GmListener, ChooseWareListener {
    private final Logger log = LoggerFactory.getLogger(PloyMessageHandler.class);

    /**
     * 进入游戏时获取配置
     *
     * @param playerController
     * @param req
     */
    @Command(PloyConstant.MsgBean.REQ_PLOY_CONFIG)
    public void ployConfig(PlayerController playerController, ReqPloyConfig req) {
        Object scene = playerController.getScene();
        if (scene instanceof AbstractPloyController<?> ployController) {
            AbstractMessage res = ployController.ployConfig(playerController);
            if (res != null) {
                playerController.send(res);
            }
        } else {
            log.warn("未找到playerController的 scene，获取配置失败 playerId = {},scene = {}", playerController.playerId(), scene);
        }
    }

    /**
     * 下注
     *
     * @param playerController
     * @param req
     */
    @Command(PloyConstant.MsgBean.REQ_PLOY_BET)
    public void reqPloyBet(PlayerController playerController, ReqPloyBet req) {
        Object scene = playerController.getScene();
        if (scene instanceof AbstractPloyController<?> ployController) {
            AbstractMessage res = ployController.bet(playerController, req.bet, req.value);
            if (res != null) {
                playerController.send(res);
            }
        } else {
            log.warn("未找到playerController的 scene，下注失败 playerId = {},scene = {}", playerController.playerId(), scene);
        }
    }

    /**
     * 进入游戏
     *
     * @param playerController
     * @param req
     */
    @Command(PloyConstant.MsgBean.REQ_PLOY_RECORD)
    public void reqPloyRecord(PlayerController playerController, ReqPloyRecord req) {
        try {
            Object scene = playerController.getScene();
            if (scene instanceof AbstractPloyController<?> ployController) {
                AbstractMessage res = ployController.reqPloyRecord(playerController, req);
                if (res != null) {
                    playerController.send(res);
                }
            } else {
                log.warn("未找到playerController的 scene，请求记录失败 playerId = {},subScene = {}", playerController.playerId(), scene);
            }
        } catch (Exception e) {
            log.error("reqPloyRecord error", e);
        }
    }


    @Override
    public CommonResult<String> gm(PlayerController playerController, String[] gmOrders) {
        CommonResult<String> res = new CommonResult<>(Code.SUCCESS);
        try {
            res.code = Code.NOT_FOUND;
        } catch (Exception e) {
            log.error("", e);
            res.code = Code.EXCEPTION;
        }
        return res;
    }

    @Override
    public void onChooseWare(PlayerController playerController, ReqChooseWare req) {
        ResChooseWare res = new ResChooseWare(Code.SUCCESS);
        if (req.gameType == playerController.getPlayer().getGameType() && req.wareId == playerController.getPlayer().getRoomCfgId()) {
            playerController.send(res);
            return;
        }

        log.warn("玩家选择场次信息错误 playerId = {},playerGameType = {},playerWareId = {},reqGameType = {},reqRoomCfgId = {}"
                , playerController.playerId(), playerController.getPlayer().getGameType(), playerController.getPlayer().getRoomCfgId(),
                req.gameType, req.wareId);
        res.code = Code.FAIL;
        playerController.send(res);
    }
}
