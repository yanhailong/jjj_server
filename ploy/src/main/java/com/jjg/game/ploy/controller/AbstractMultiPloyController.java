package com.jjg.game.ploy.controller;

import com.jjg.game.common.cluster.ClusterClient;
import com.jjg.game.common.cluster.ClusterMessage;
import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.common.curator.MarsCurator;
import com.jjg.game.common.curator.NodeType;
import com.jjg.game.common.listener.IGameClusterLeaderListener;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.protostuff.MessageUtil;
import com.jjg.game.ploy.data.PlayerMultiPloyGameData;
import com.jjg.game.ploy.data.PloyGameRoom;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * 多人策略游戏抽象控制器
 * <p>
 * 提供集群通信、本地广播、机器人管理的通用能力，子类实现具体游戏逻辑。
 * </p>
 *
 * @author 11
 * @date 2026/3/19
 */
public abstract class AbstractMultiPloyController<T extends PlayerMultiPloyGameData, R extends PloyGameRoom> extends AbstractPloyController<T> implements IGameClusterLeaderListener {
    @Autowired
    protected ClusterSystem clusterSystem;
    @Autowired
    protected MarsCurator marsCurator;

    protected Class<R> gameRoomDataCla;

    /**
     * 机器人玩家数据 key=robotId
     */
    protected final Map<Long, T> robotDataMap = new ConcurrentHashMap<>();

    public AbstractMultiPloyController(Logger log, Class<T> playerGameDataCla, Class<R> gameRoomDataCla) {
        super(log, playerGameDataCla);
        this.gameRoomDataCla = gameRoomDataCla;
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
     * @param msg 要同步的消息
     */
    protected void messageSync(AbstractMessage msg) {
        try {
            List<ClusterClient> nodes = ClusterSystem.system.getNodesByTypeExcludeSelf(NodeType.GAME, CoreConst.GameType.AIR_STRIKE);
            if (nodes == null || nodes.isEmpty()) {
                return;
            }
            ClusterMessage clusterMessage = new ClusterMessage(MessageUtil.getPFMessage(msg));
            for (ClusterClient node : nodes) {
                node.write(clusterMessage);
            }
        } catch (Exception e) {
            log.error("", e);
        }

    }

    /**
     * 通知到本节点所有的真实玩家
     *
     * @param msg 要推送的消息
     */
    protected void broadcastLocalPlayers(AbstractMessage msg) {
        broadcastLocalPlayersExcept(msg, 0);
    }

    /**
     * 通知到本节点所有的真实玩家
     *
     * @param msg
     * @param excludePlayerId 排除玩家
     */
    protected void broadcastLocalPlayersExcept(AbstractMessage msg, long excludePlayerId) {
        this.gameDataMap.forEach((playerId, playerData) -> {
            if (playerId == excludePlayerId) {
                return;
            }
            if (playerData.getPlayerController() != null) {
                playerData.getPlayerController().send(msg);
            }
        });
    }

    // ==================== 机器人管理(子类按需覆写) ====================

    /**
     * 清空所有机器人数据(新回合时调用)
     */
    protected void clearRobots() {
        robotDataMap.clear();
    }
}
