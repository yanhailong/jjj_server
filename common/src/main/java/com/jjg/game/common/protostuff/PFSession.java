package com.jjg.game.common.protostuff;

import com.jjg.game.common.cluster.ClusterMessage;
import com.jjg.game.common.message.ResSessionVerifyPass;
import com.jjg.game.common.net.Connect;
import com.jjg.game.common.net.NetAddress;
import com.jjg.game.common.net.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 用户session
 *
 * @author nobody
 * @since 1.0
 */
public class PFSession extends Session<Object, Object> {

    public static Logger log = LoggerFactory.getLogger(PFSession.class);

    public long playerId;
    /* 网关节点PATH*/
    public String gatePath;
    /* 业务ID，用于根据该ID分配业务线程*/
    public long workId;

    public long activeTime;

    public PFSession(String sessionId, Connect connect, NetAddress address) {
        super(sessionId, connect, address);
        activeTime = System.currentTimeMillis();
    }

    public long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(long playerId) {
        this.playerId = playerId;
    }

    public String getGatePath() {
        return gatePath;
    }

    public void setGatePath(String gatePath) {
        this.gatePath = gatePath;
    }

    public long getWorkId() {
        return workId;
    }

    public void setWorkId(long workId) {
        this.workId = workId;
    }

    public long getActiveTime() {
        return activeTime;
    }

    public void setActiveTime(long activeTime) {
        this.activeTime = activeTime;
    }

    @Override
    public void send(Object msg) {
        PFMessage pfMessage;
        if (msg instanceof PFMessage) {
            pfMessage = (PFMessage) msg;
        } else {
            pfMessage = MessageUtil.getPFMessage(msg);
        }
        if (pfMessage != null) {
            ClusterMessage clusterMessage = new ClusterMessage(sessionId, pfMessage);
            connect.write(clusterMessage);
        }
    }

    public void send2Gate(Object msg) {
        PFMessage pfMessage = MessageUtil.getPFMessage(msg);
        ClusterMessage clusterMessage = new ClusterMessage(pfMessage);
        connect.write(clusterMessage);
    }

    /**
     * 当用户验证通过后调用
     *
     * @param playerId
     * @param reference
     */
    public void verifyPass(long playerId, String ip, Object reference) {
        this.reference = reference;
        this.playerId = playerId;
        ResSessionVerifyPass resSessionVerifyPass = new ResSessionVerifyPass();
        resSessionVerifyPass.playerId = playerId;
        resSessionVerifyPass.sessionId = sessionId;
        resSessionVerifyPass.ip = ip;
        resSessionVerifyPass.create = System.currentTimeMillis();
        resSessionVerifyPass.success = true;
        PFMessage pfMessage = MessageUtil.getPFMessage(resSessionVerifyPass);
        ClusterMessage clusterMessage = new ClusterMessage(pfMessage);
        connect.write(clusterMessage);
    }

    /**
     * 当用户验证未通过后调用
     */
    public void verifyPassFail() {
        ResSessionVerifyPass resSessionVerifyPass = new ResSessionVerifyPass();
        resSessionVerifyPass.create = System.currentTimeMillis();
        resSessionVerifyPass.success = false;
        resSessionVerifyPass.sessionId = sessionId;
        PFMessage pfMessage = MessageUtil.getPFMessage(resSessionVerifyPass);
        ClusterMessage clusterMessage = new ClusterMessage(pfMessage);
        connect.write(clusterMessage);
    }

    public void onClose() {
        sessionListener.onSessionClose(this);
    }
}
