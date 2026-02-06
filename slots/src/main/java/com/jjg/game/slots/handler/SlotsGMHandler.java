package com.jjg.game.slots.handler;

import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.listener.GmListener;
import com.jjg.game.slots.manager.SlotsFactoryManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


/**
 * @author 11
 * @date 2025/9/12 15:59
 */
@Component
public class SlotsGMHandler implements GmListener {
    protected final Logger log = LoggerFactory.getLogger(getClass());


    @Autowired
    private SlotsFactoryManager slotsFactoryManager;

    @Override
    public CommonResult<String> gm(PlayerController playerController, String[] gmOrders) {
        CommonResult<String> res = new CommonResult<>(Code.SUCCESS);
        try {
            if ("libType".equalsIgnoreCase(gmOrders[0])) {
                log.debug("收到选择libtype 的gm命令 playerId = {},gmOrders = {}", playerController.playerId(), gmOrders);
                slotsFactoryManager.getGameManager(playerController.getPlayer().getGameType(), playerController.getPlayer().getRoomCfgId()).addTestIconDataLibType(playerController, Integer.parseInt(gmOrders[1]));
            } else if ("setIcons".equalsIgnoreCase(gmOrders[0])) {
                log.debug("收到setIcons 的gm命令 playerId = {},gmOrders = {}", playerController.playerId(), gmOrders);
                slotsFactoryManager.getGameManager(playerController.getPlayer().getGameType(), playerController.getPlayer().getRoomCfgId()).addTestIconDataIcons(playerController, gmOrders[1]);
            } else if ("setLib".equalsIgnoreCase(gmOrders[0])) {
                log.debug("收到setLib 的gm命令 playerId = {},gmOrders = {}", playerController.playerId(), gmOrders);
                slotsFactoryManager.getGameManager(playerController.getPlayer().getGameType(), playerController.getPlayer().getRoomCfgId()).addTestLibs(playerController, gmOrders[1]);
            } else if ("pool".equalsIgnoreCase(gmOrders[0])) {
                log.debug("收到pool 的gm命令 playerId = {},gmOrders = {}", playerController.playerId(), gmOrders);
                boolean change = slotsFactoryManager.getGameManager(playerController.getPlayer().getGameType(), playerController.getPlayer().getRoomCfgId()).gmChangePool(playerController, Integer.parseInt(gmOrders[1]), Long.parseLong(gmOrders[2]));
                if (!change) {
                    res.code = Code.FAIL;
                    return res;
                }
            } else if ("contribt".equalsIgnoreCase(gmOrders[0])) {  //玩家贡献金额
                log.debug("收到contribt 的gm命令 playerId = {},gmOrders = {}", playerController.playerId(), gmOrders);
                boolean change = slotsFactoryManager.getGameManager(playerController.getPlayer().getGameType(), playerController.getPlayer().getRoomCfgId()).gmChangeContribtGold(playerController, Long.parseLong(gmOrders[1]));
                if (!change) {
                    res.code = Code.FAIL;
                    return res;
                }
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
