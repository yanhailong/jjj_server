package com.jjg.game.common.cluster;

import com.jjg.game.common.curator.NodeType;
import com.jjg.game.common.message.BroadCastMessage;
import com.jjg.game.common.protostuff.MessageUtil;
import com.jjg.game.common.protostuff.PFMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 集群消息发送器
 * @since 1.0
 */
@Component
public class ClusterMsgSender {

    @Autowired
    ClusterSystem clusterSystem;

    Logger log = LoggerFactory.getLogger(getClass());

    /**
     * 先广播给本地大厅玩家，再通知其他大厅节点广播给各自的玩家。
     */
    public void broadcast2Halls(Object msg) {
        PFMessage pfMessage = MessageUtil.getPFMessage(msg);
        if (pfMessage == null) {
            return;
        }
        if (NodeType.HALL.toString().equals(clusterSystem.nodeConfig.getType())) {
            clusterSystem.broadcastToOnlinePlayer(pfMessage);
        }
        clusterSystem.notifyNode(MessageUtil.getPFMessage(new BroadCastMessage(pfMessage)),
                NodeType.HALL.toString()::equals);
    }

    /**
     * 向所有网关广播消息
     *
     * @param msg
     */
    public void broadcast2Gates(Object msg) {
        List<ClusterClient> clusterClients = clusterSystem.getAllGate();
        if (clusterClients != null && !clusterClients.isEmpty()) {
            clusterClients.forEach(clusterClient -> {
                try {
                    PFMessage pfmsg =MessageUtil.getPFMessage(msg);
                    PFMessage pfMessage = MessageUtil.getPFMessage(new BroadCastMessage(pfmsg));
                    ClusterMessage clusterMessage = new ClusterMessage(pfMessage);
                    clusterClient.write(clusterMessage);
                } catch (Exception e) {
                    e.printStackTrace();
                    log.warn("广播消息到网关失败,gateName=" + clusterClient.nodeConfig.getName(), e);
                }
            });
        }
    }
}
