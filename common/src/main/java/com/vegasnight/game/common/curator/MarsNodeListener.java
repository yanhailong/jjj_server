package com.vegasnight.game.common.curator;

/**
 * 节点监听器
 * @since 1.0
 */
public interface MarsNodeListener {
    enum NodeChangeType {
        NODE_ADD, NODE_REMOVE, DATA_CHANGE
    }

    /**
     * 节点状态改变
     *
     * @param nodeChangeType
     * @param marsNode
     */
    void nodeChange(NodeChangeType nodeChangeType, MarsNode marsNode);
}
