package com.jjg.game.core.data;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * 机器人玩家，机器人的数据放置在这
 *
 * @author 2CL
 */
public class RobotPlayer extends Player {

    // 当前处于哪个节点
    @JsonIgnore
    transient private String nodePath;

    public String getNodePath() {
        return nodePath;
    }

    public void setNodePath(String nodePath) {
        this.nodePath = nodePath;
    }
}
