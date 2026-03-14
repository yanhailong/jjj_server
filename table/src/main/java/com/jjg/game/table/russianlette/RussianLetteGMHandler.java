package com.jjg.game.table.russianlette;

import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.listener.GmListener;
import com.jjg.game.room.controller.AbstractGameController;
import com.jjg.game.room.data.room.GameDataVo;
import com.jjg.game.room.manager.RoomManager;
import com.jjg.game.sampledata.bean.RoomCfg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 俄罗斯转盘 GM 命令处理器
 * <p>
 * 支持命令：
 * <ul>
 *   <li>{@code lotteryDraw <number>} — 指定下一局开奖结果（0-36），仅生效一局</li>
 * </ul>
 *
 * @author lhc
 */
@Component
public class RussianLetteGMHandler implements GmListener {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private RoomManager roomManager;

    @Override
    public CommonResult<String> gm(PlayerController playerController, String[] gmOrders) {
        CommonResult<String> res = new CommonResult<>(Code.SUCCESS);
        try {
            if ("lotteryDraw".equalsIgnoreCase(gmOrders[0])) {
                int diceData = Integer.parseInt(gmOrders[1]);
                if (diceData < 0 || diceData > 37) {
                    log.warn("lotteryDraw: 参数越界 diceData={}, 合法范围 [0,36]", diceData);
                    res.code = Code.PARAM_ERROR;
                    return res;
                }
                AbstractGameController<? extends RoomCfg, ? extends GameDataVo<? extends RoomCfg>> gc =
                        roomManager.getGameControllerByPlayerId(playerController.playerId());
                if (gc instanceof RussianLetteGameController rgc) {
                    rgc.getGameDataVo().setTestDiceData(diceData);
                    log.info("lotteryDraw: playerId={} 设置下一局开奖 diceData={}",
                            playerController.playerId(), diceData);
                } else {
                    log.warn("lotteryDraw: playerId={} 不在俄罗斯转盘房间中", playerController.playerId());
                    res.code = Code.FAIL;
                }
            } else {
                res.code = Code.NOT_FOUND;
            }
        } catch (Exception e) {
            log.error("俄罗斯转盘 GM 命令异常", e);
            res.code = Code.EXCEPTION;
        }
        return res;
    }
}
