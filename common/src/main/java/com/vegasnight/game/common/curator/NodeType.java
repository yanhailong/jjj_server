package com.vegasnight.game.common.curator;

/**
 * @since 1.0
 */
public enum NodeType {
    //网关
    GATE(1),
    //账号验证
    ACCOUNT(2),
    //大厅服务器
    HALL(3),
    //房间
    ROOM(4);

    NodeType(int value) {
        this.value = value;
    }

    private int value;

    public int getValue() {
        return value;
    }
}
