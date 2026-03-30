package com.jjg.game.ploy.controller;

import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.curator.NodeType;
import com.jjg.game.common.listener.IGameClusterLeaderListener;
import com.jjg.game.common.protostuff.MessageUtil;
import com.jjg.game.common.protostuff.PFMessage;
import com.jjg.game.ploy.data.PlayerMultiPloyGameData;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.Set;


/**
 * 多人策略游戏抽象控制器
 *
 * @author 11
 * @date 2026/3/19
 */
public abstract class AbstractMultiPloyController<T extends PlayerMultiPloyGameData> extends AbstractPloyController<T> implements IGameClusterLeaderListener {
    @Autowired
    private ClusterSystem clusterSystem;

    public AbstractMultiPloyController(Logger log, Class<T> cla) {
        super(log, cla);
    }

    @Override
    public void notLeader() {

    }

    @Override
    public void isLeader() {

    }

    /**
     * 消息同步到其他节点
     *
     * @param msg
     */
    protected void messageSync(Object msg, boolean toClient) {
        PFMessage pfMessage = MessageUtil.getPFMessage(msg);
        clusterSystem.notifyNode(pfMessage, Set.of(NodeType.HALL.toString(), NodeType.GAME.toString())::contains);

        if (toClient) {
            broadcastLocalPlayers(msg);
        }
    }

    /**
     * 通知到本节点所有的玩家
     *
     * @param msg
     */
    protected void broadcastLocalPlayers(Object msg) {
        Map<Long, T> tmpMap = this.gameDataMap.get(this.roomCfgId);
        if (tmpMap != null) {
            tmpMap.forEach((id, playerData) -> {
                if (playerData.getPlayerController() != null) {
                    playerData.getPlayerController().send(msg);
                }
            });
        }
    }
}
