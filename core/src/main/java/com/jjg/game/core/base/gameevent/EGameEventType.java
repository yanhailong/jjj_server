package com.jjg.game.core.base.gameevent;

import java.util.List;

/**
 * 游戏事件类型
 *
 * @author 2CL
 */
public enum EGameEventType {
    // 玩家升级
    PLAYER_LEVEL(true, "levelID"),    // 玩家升级
    // 玩家升级
    PLAYER_VIPLEVEL(true, "VIPlevelID"),    // 玩家VIP升级
    //充值
    RECHARGE(true),
    // 产生有效流水
    EFFECTIVE_FLOWING(true, "effectiveFlowing"),
    // 个人有效下注 不计算好友房
    PLAYER_BET(true, "bet"),
    // 个人有效下注 计算所有游戏
    PLAYER_BETALL(true, "bet"),
    // 个人有效下注 计算配置范围内的游戏
    PLAY_GAME(true, "bet", "gameID"),
    // 个人有效下注 计算不在配置范围内的游戏
    NOTPLAY_GAME(true, "bet", "gameID"),
    // 个人有效下注 计算不在配置范围内的游戏类型  Warehouse表中gameType字段值
    PLAY_GAMETYPE(true, "bet", "gametype"),
    // 个人有效下注 计算不在配置范围内的房间类型  Warehouse表中roomType字段值
    PLAY_ROOMTYPE(true, "bet", "roomtype"),
    // 整点事件
    CLOCK_EVENT(false),
    // 货币变化事件（主要用于在对战类游戏中更新游戏数据）
    CURRENCY_CHANGE(false),
    ;

    // 事件是否由玩家产生
    final boolean isRelatedPlayer;

    // 绑定事件产生变化的属性，例如：玩家升级，绑定玩家的等级，LevelId. 如果
    final List<String> bindProperties;

    EGameEventType(boolean isRelatedPlayer, String... bindProperties) {
        this.isRelatedPlayer = isRelatedPlayer;
        this.bindProperties = List.of(bindProperties);
    }

    public boolean isRelatedPlayer() {
        return isRelatedPlayer;
    }

    public List<String> getBindProperties() {
        return bindProperties;
    }

    public static EGameEventType gameEventType(String name) {
        for (EGameEventType value : values()) {
            if (value.name().equalsIgnoreCase(name)) {
                return value;
            }
        }
        return null;
    }
}
