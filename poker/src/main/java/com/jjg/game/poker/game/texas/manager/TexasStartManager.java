package com.jjg.game.poker.game.texas.manager;

import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.listener.GmListener;
import com.jjg.game.poker.game.texas.room.TexasGameController;
import com.jjg.game.poker.game.texas.room.data.TexasGameDataVo;
import com.jjg.game.room.controller.AbstractGameController;
import com.jjg.game.room.data.room.GameDataVo;
import com.jjg.game.room.listener.IRoomStartListener;
import com.jjg.game.room.manager.RoomManager;
import com.jjg.game.sampledata.bean.RoomCfg;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * @author 11
 * @date 2025/6/27 17:53
 */
@Component
public class TexasStartManager implements IRoomStartListener, GmListener {
    private Logger log = LoggerFactory.getLogger(getClass());
    @Autowired
    private RoomManager roomManager;

    @Override
    public void start() {
        log.info("正在启动德州游戏...");
    }

    @Override
    public void shutdown() {
        log.info("正在关闭德州游戏...");
    }

    @Override
    public CommonResult<String> gm(PlayerController playerController, String[] gmOrders) {
        String cmd = gmOrders[0];
        if ("setTexasCard".equalsIgnoreCase(cmd)) {
            AbstractGameController<? extends RoomCfg, ? extends GameDataVo<? extends RoomCfg>> gameController =
                    roomManager.getGameControllerByPlayerId(playerController.playerId());
            if (gameController instanceof TexasGameController controller) {
                String cardValue = gmOrders[1];
                String[] cardArr = StringUtils.split(cardValue, ",");
                if (cardArr.length < 1) {
                    return new CommonResult<>(Code.FAIL);
                }
                TexasGameDataVo gameDataVo = controller.getGameDataVo();
                if (cardArr.length == 1) {
                    gameDataVo.setTempCard(null);
                } else {
                    gameDataVo.setTempCard(Arrays.stream(cardArr).map(Integer::parseInt).collect(Collectors.toList()));
                }
                return new CommonResult<>(Code.SUCCESS);
            }
        }
        return null;
    }
}
