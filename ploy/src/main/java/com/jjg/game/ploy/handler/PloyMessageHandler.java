package com.jjg.game.ploy.handler;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.listener.GmListener;
import com.jjg.game.ploy.constant.PloyConstant;
import com.jjg.game.ploy.constant.PloyGameType;
import com.jjg.game.ploy.pb.ReqPloyBet;
import com.jjg.game.ploy.pb.ReqPloyEnterGame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author 11
 * @date 2026/3/19
 */
@Component
@MessageType(MessageConst.MessageTypeDef.PLOY_COMMON)
public class PloyMessageHandler implements GmListener {
    private final Logger log = LoggerFactory.getLogger(PloyMessageHandler.class);

    /**
     * 进入游戏
     *
     * @param playerController
     * @param req
     */
    @Command(PloyConstant.MsgBean.REQ_PLOY_ENTER_GAME)
    public void reqPloyEnterGame(PlayerController playerController, ReqPloyEnterGame req) {
        try {
            PloyGameType ployGameType = PloyGameType.fromType(req.gameType);
            System.out.println(ployGameType);
            if (ployGameType == null || ployGameType.getController() == null || req.roomCfgId < 1) {
                log.warn("未找到对应的游戏类型，进入游戏失败 playerId = {},gameType = {}", playerController.playerId(), req.gameType);
                return;
            }

            playerController.send(ployGameType.getController().enterGame(playerController, req.gameType, req.roomCfgId));
        } catch (Exception e) {
            log.error("", e);
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
        PloyGameType ployGameType = PloyGameType.fromType(playerController.getPlayer().getGameType());
        if (ployGameType == null || ployGameType.getController() == null) {
            log.warn("未找到对应的游戏类型，下注失败 playerId = {},gameType = {}", playerController.playerId(), playerController.getPlayer().getGameType());
            return;
        }
        playerController.send(ployGameType.getController().bet(playerController, req.value));
    }

    @Override
    public CommonResult<String> gm(PlayerController playerController, String[] gmOrders) {
        CommonResult<String> res = new CommonResult<>(Code.SUCCESS);
        try {
            if ("ployEnterGame".equalsIgnoreCase(gmOrders[0])) {
                log.debug("收到选择 ployEnterGame 的gm命令 playerId = {},gmOrders = {}", playerController.playerId(), gmOrders);

                ReqPloyEnterGame req = new ReqPloyEnterGame();
                req.gameType = Integer.parseInt(gmOrders[1]);
                req.roomCfgId = Integer.parseInt(gmOrders[2]);

                reqPloyEnterGame(playerController, req);
                res.code = Code.SUCCESS;
            } else if ("ployBet".equalsIgnoreCase(gmOrders[0])) {
                Integer gameType = Integer.parseInt(gmOrders[1]);
                Long betValue = Long.parseLong(gmOrders[2]);
                if (gameType < 1 || betValue < 1) {
                    res.code = Code.PARAM_ERROR;
                    return res;
                }

                ReqPloyBet req = new ReqPloyBet();
                req.value = betValue;
                reqPloyBet(playerController, req);
                res.code = Code.SUCCESS;
            } else {
                res.code = Code.NOT_FOUND;
            }
        } catch (Exception e) {
            log.error("", e);
            res.code = Code.EXCEPTION;
        }
        return res;
    }
}
