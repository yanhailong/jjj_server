package com.jjg.game.common.cluster;

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
