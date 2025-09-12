package com.jjg.game.slots.handler;

import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.listener.GmListener;
import com.jjg.game.slots.game.dollarexpress.data.TestLibData;
import com.jjg.game.slots.manager.AbstractSlotsGameManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * @author 11
 * @date 2025/9/12 15:59
 */
public abstract class SlotsMessageHandler implements GmListener {
    protected Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public CommonResult<String> gm(PlayerController playerController, String[] gmOrders) {
        CommonResult<String> res = new CommonResult<>(Code.SUCCESS);
        try{
            if ("libType".equalsIgnoreCase(gmOrders[0])) {
                log.debug("收到选择libtype 的gm命令 playerId = {},gmOrders = {}", playerController.playerId(), gmOrders);
                TestLibData testLibData = new TestLibData();

                int libType = Integer.parseInt(gmOrders[1]);
                if(libType < 1 || libType > 10) {
                    log.debug("libType不合法 playerId = {},libType = {}", playerController.playerId(),libType);
                    res.code = Code.PARAM_ERROR;
                }else {
                    testLibData.setLibType(libType);
                    getGameManager().addTestIconData(playerController, testLibData);
                }
            }else if ("adminGenerateLib".equals(gmOrders[0])) {
                log.debug("收到生成结果库的gm命令 playerId = {},gmOrders = {}", playerController.playerId(), gmOrders);
                int count = Integer.parseInt(gmOrders[1]);
                if (count > 100000) {
                    log.debug("数字太大，请重新输入 playerId = {},gmOrders = {}", playerController.playerId(), gmOrders);
                    res.code = Code.FAIL;
                    return res;
                }
                boolean success = getGameManager().addGenerateLibEvent(getGenerateMap(count));
                if (!success) {
                    res.code = Code.FAIL;
                }
            }else {
                res.code = Code.NOT_FOUND;
            }
        }catch (Exception e) {
            log.error("",e);
            res.code = Code.EXCEPTION;
        }
        return res;
    }

    protected abstract AbstractSlotsGameManager getGameManager();
    protected abstract Map<Integer,Integer> getGenerateMap(int count);
}
