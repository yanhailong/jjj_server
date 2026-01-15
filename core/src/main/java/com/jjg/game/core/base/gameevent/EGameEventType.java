package com.jjg.game.core.base.gameevent;

import com.jjg.game.sampledata.GameDataManager;

import java.util.List;

/**
 * 游戏事件类型
 *
 * @author 2CL
 * useItemCount
 */
public enum EGameEventType {
    // 玩家升级
    PLAYER_LEVEL("playerLevel"),    // 玩家升级
    //玩家绑定手机号
    BIND_PHONE("bindPhone"),    // 玩家绑定手机号
    PLAYER_VIP_LEVEL("playerVipLevel"),    // 玩家vip等级
    //充值
    RECHARGE(""),
    // 产生有效流水
    EFFECTIVE_FLOWING(""),
    // 整点事件
    CLOCK_EVENT(""),
    // 货币变化事件（主要用于在对战类游戏中更新游戏数据）
    CURRENCY_CHANGE(""),
    ;

    // 配置表中的类型
    private final String configType;

    EGameEventType(String configType) {
        this.configType = configType;
    }

    public String getConfigType() {
        return configType;
    }

    public static EGameEventType gameEventType(String name) {
        for (EGameEventType value : values()) {
            if (value.getConfigType().equalsIgnoreCase(name)) {
                return value;
            }
        }
        return null;
    }
}
