package com.jjg.game.core.base.gameevent;

/**
 * 游戏事件类型
 *
 * @author 2CL
 */
public enum EGameEventType {
    // 玩家升级
    PLAYER_LEVEL(true, "levelID"),
    ;

    // 事件是否由玩家产生
    final boolean isRelatedPlayer;

    // 绑定事件产生变化的属性，例如：玩家升级，绑定玩家的等级，LevelId. 如果
    final String bindProperties;

    EGameEventType(boolean isRelatedPlayer, String bindProperties) {
        this.isRelatedPlayer = isRelatedPlayer;
        this.bindProperties = bindProperties;
    }

    public boolean isRelatedPlayer() {
        return isRelatedPlayer;
    }

    public String getBindProperties() {
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
