package com.jjg.game.social.constant;

/**
 * 聊天频道类型。
 * <p>
 * 新增频道(如自定义活动频道)只需在此登记一个枚举值, 再实现 {@code ChatChannel} 并注册到
 * {@code ChatChannelRegistry} 即可接入, 无需改动收发协议 —— 这是聊天系统扩展性的核心。
 *
 * @author 11
 * @date 2026/6/9
 */
public enum ChatChannelType {
    //世界频道 (全服)
    WORLD(1),
    //系统消息 (仅服务端下发)
    SYSTEM(2),
    //好友私聊 (一对一)
    PRIVATE(3),
    //联盟频道 (预留, 依赖联盟系统)
    ALLIANCE(4),
    //房间频道 (预留, 依赖 slots 房间, 后续由游戏节点接入)
    ROOM(5),
    ;

    private final int code;

    ChatChannelType(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static ChatChannelType of(int code) {
        for (ChatChannelType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        return null;
    }
}
