package com.jjg.game.ploy.games.airraid.handler;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.listener.GmListener;
import com.jjg.game.ploy.games.airraid.AirRaidPloyController;
import com.jjg.game.ploy.games.airraid.data.AirRaidConstant;
import com.jjg.game.ploy.games.airraid.pb.ReqAirRaidAutoCashOut;
import com.jjg.game.ploy.games.airraid.pb.ReqAirRaidCashOut;
import com.jjg.game.ploy.games.airraid.pb.ResAirRaidAutoCashOut;
import com.jjg.game.ploy.games.airraid.pb.cluster.BetSync;
import com.jjg.game.ploy.games.airraid.pb.cluster.CashOutSync;
import com.jjg.game.ploy.games.airraid.pb.cluster.GameStateSync;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 空袭游戏消息处理器 — 处理兑现等空袭专属请求
 *
 * @author 11
 * @date 2026/3/27
 */
@Component
@MessageType(MessageConst.MessageTypeDef.PLOY_AIR_RAID)
public class AirRaidMessageHandler implements GmListener {
    private final Logger log = LoggerFactory.getLogger(AirRaidMessageHandler.class);

    @Autowired
    private AirRaidPloyController airRaidPloyController;

    /**
     * 游戏状态同步
     */
    @Command(AirRaidConstant.MsgBean.GAME_STATE_SYNC)
    public void onGameStateSync(GameStateSync msg) {
        airRaidPloyController.onGameStateSync(msg);
    }

    /**
     * 下注同步
     */
    @Command(AirRaidConstant.MsgBean.BET_SYNC)
    public void onBetSync(BetSync msg) {
        airRaidPloyController.onBetSync(msg);
    }

    /**
     * 兑现同步
     */
    @Command(AirRaidConstant.MsgBean.CASH_OUT_SYNC)
    public void onCashOutSync(CashOutSync msg) {
        airRaidPloyController.onCashOutSync(msg);
    }


    //--------------------------------------------------玩家消息-----------------------------------------


    /**
     * 兑现请求
     *
     * @param playerController
     * @param req
     */
    @Command(AirRaidConstant.MsgBean.REQ_AIR_RAID_CASH_OUT)
    public void reqCashOut(PlayerController playerController, ReqAirRaidCashOut req) {
        try {
            playerController.send(airRaidPloyController.cashOut(playerController, req.betIndex));
        } catch (Exception e) {
            log.error("AirRaid 兑现请求异常 playerId={}", playerController.playerId(), e);
        }
    }

    /**
     * 自动兑现请求 — 仅保存设置；真正触发由飞行开始时按注单上的快照调度
     *
     * @param playerController
     * @param req
     */
    @Command(AirRaidConstant.MsgBean.REQ_AIR_RAID_AUTO_CASH_OUT)
    public void reqAutoCashOut(PlayerController playerController, ReqAirRaidAutoCashOut req) {
        try {
            int code = airRaidPloyController.updateAutoCashOutConfig(
                    playerController.playerId(), req.betIndex, req.open, req.crashMultiplier);
            playerController.send(new ResAirRaidAutoCashOut(code));
        } catch (Exception e) {
            log.error("AirRaid 自动兑现请求异常 playerId={}", playerController.playerId(), e);
            playerController.send(new ResAirRaidAutoCashOut(Code.EXCEPTION));
        }
    }

    @Override
    public CommonResult<String> gm(PlayerController playerController, String[] gmOrders) {
        CommonResult<String> res = new CommonResult<>(Code.SUCCESS);
        try {
            if ("airRaidCashOut".equalsIgnoreCase(gmOrders[0])) {
                log.debug("收到 airRaidCashOut 的gm命令 playerId={}, gmOrders={}", playerController.playerId(), gmOrders);

                int betIndex = Integer.parseInt(gmOrders[1]);
                ReqAirRaidCashOut req = new ReqAirRaidCashOut();
                req.betIndex = betIndex;

                reqCashOut(playerController, req);
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
