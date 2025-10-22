package com.jjg.game.room.handler;

import com.alibaba.fastjson.JSON;
import com.jjg.game.common.concurrent.IProcessorHandler;
import com.jjg.game.core.data.Player;
import com.jjg.game.room.controller.AbstractGameController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * @author lm
 * @date 2025/9/29 10:37
 */
public record CurrentChangeEventHandler(Player player, AbstractGameController<?, ?> gameController,
                                        Map<Integer, Long> currencyMap) implements IProcessorHandler {
    private static final Logger log = LoggerFactory.getLogger(CurrentChangeEventHandler.class);

    @Override
    public void action() throws Exception {
        try {
            gameController.changeCurrency(player, currencyMap, "CurrentChange", "", true);
        } catch (Exception e) {
            log.error("货币变化时更新房间内货币失败 playerId:{} currencyMap:{} ", player.getId(), JSON.toJSONString(currencyMap), e);
        }
    }
}
