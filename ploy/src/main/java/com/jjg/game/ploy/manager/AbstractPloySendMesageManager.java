package com.jjg.game.ploy.manager;

import com.jjg.game.common.cluster.ClusterClient;
import com.jjg.game.common.cluster.ClusterMessage;
import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.common.curator.NodeType;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.protostuff.MessageUtil;
import com.jjg.game.ploy.data.PlayerMultiPloyGameData;
import com.jjg.game.ploy.data.PloyGameRoom;
import com.jjg.game.ploy.games.airraid.AirRaidPloyController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * 消息发送管理器
 *
 * @author 11
 * @date 2026/5/13
 */
public class AbstractPloySendMesageManager<T extends PlayerMultiPloyGameData, R extends PloyGameRoom> {
    protected Logger log = LoggerFactory.getLogger(getClass());

    /**
     * 消息同步到其他节点
     *
     * @param msg 要同步的消息
     */
    public void messageSync(AbstractMessage msg) {
        try {
            List<ClusterClient> nodes = ClusterSystem.system.getNodesByTypeExcludeSelf(NodeType.GAME, CoreConst.GameMajorType.PLOY);
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
    public void broadcastLocalPlayers(Map<Long, T> gameDataMap, AbstractMessage msg) {
        broadcastLocalPlayersExcept(gameDataMap, msg, 0);
    }

    /**
     * 通知到本节点所有的真实玩家
     *
     * @param msg
     * @param excludePlayerId 排除玩家
     */
    public void broadcastLocalPlayersExcept(Map<Long, T> gameDataMap, AbstractMessage msg, long excludePlayerId) {
        gameDataMap.forEach((playerId, playerData) -> {
            if (playerId != excludePlayerId && playerData.getPlayerController() != null) {
                playerData.getPlayerController().send(msg);
            }
        });
    }
}
