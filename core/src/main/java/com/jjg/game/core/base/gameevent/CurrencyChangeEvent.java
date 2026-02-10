package com.jjg.game.core.base.gameevent;

import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.data.Player;

import java.util.Map;

/**
 * 货币变化事件
 * @author lm
 * @date 2025/9/29 09:49
 */
public class CurrencyChangeEvent extends GameEvent {
    /**
     * 玩家id
     */
    private final Player player;
    /**
     * 货币变化列表
     */
    private final Map<Integer, Long> currencyMap;

    private final AddType addType;

    private final String desc;

    public CurrencyChangeEvent(EGameEventType gameEventType, Player player, Map<Integer, Long> currencyMap, AddType addType, String desc) {
        super(gameEventType);
        this.player = player;
        this.currencyMap = currencyMap;
        this.addType = addType;
        this.desc = desc;
    }

    public Map<Integer, Long> getCurrencyMap() {
        return currencyMap;
    }

    public Player getPlayer() {
        return player;
    }

    public AddType getAddType() {
        return addType;
    }

    public String getDesc() {
        return desc;
    }
}
