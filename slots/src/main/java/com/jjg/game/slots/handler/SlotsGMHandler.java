package com.jjg.game.slots.handler;

import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.listener.GmListener;
import com.jjg.game.slots.game.dollarexpress.data.TestLibData;
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
        CommonResult<String> res = new CommonResult<>();
        try {
            if ("libType".equalsIgnoreCase(gmOrders[0])) {
                log.debug("收到选择libtype 的gm命令 playerId = {},gmOrders = {}", playerController.playerId(), gmOrders);
                TestLibData testLibData = new TestLibData();

                int libType = Integer.parseInt(gmOrders[1]);
                if(libType < 1 || libType > 6) {
                    log.debug("libType不合法 playerId = {},libType = {}", playerController.playerId(),libType);
                    res.code = Code.PARAM_ERROR;
                }else {
                    testLibData.setLibType(libType);
                    slotsFactoryManager.getGameManager(playerController.getPlayer().getGameType()).addTestIconData(playerController,testLibData);
                }
            }else {
                res.code = Code.NOT_FOUND;
            }
        } catch (Exception e) {
            log.error("", e);
            res.code = Code.EXCEPTION;
        }
        return res;
    }
}
