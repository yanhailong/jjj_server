package com.jjg.game.common.curator;

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
    //游戏
    GAME(4),
    //gm
    GM(5),
    //充值服务器
    RECHARGE(6);

    NodeType(int value) {
        this.value = value;
    }

    private int value;

    public int getValue() {
        return value;
    }

    /**
     * 检索与给定名称匹配的NodeType枚举值，忽略大小写。
     */
    public static NodeType getNodeTypeByName(String nodeTypeName) {
        for (NodeType nodeType : NodeType.values()) {
            if (nodeType.name().equalsIgnoreCase(nodeTypeName)) {
                return nodeType;
            }
        }
        return null;
    }
}
