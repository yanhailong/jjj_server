package com.jjg.game.common.gate;

import com.jjg.game.common.cluster.ClusterClient;
import com.jjg.game.common.cluster.ClusterMessage;
import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.curator.NodeType;
import com.jjg.game.common.message.SessionCreate;
import com.jjg.game.common.message.SessionQuit;
import com.jjg.game.common.net.Connect;
import com.jjg.game.common.net.ConnectListener;
import com.jjg.game.common.net.Inbox;
import com.jjg.game.common.net.NetAddress;
import com.jjg.game.common.netty.NettyConnect;
import com.jjg.game.common.pb.NetStatEnum;
import com.jjg.game.common.pb.NoticeServerStatus;
import com.jjg.game.common.pb.ResHeartBeat;
import com.jjg.game.common.protostuff.MessageUtil;
import com.jjg.game.common.protostuff.PFMessage;
import com.jjg.game.common.protostuff.ProtostuffUtil;
import com.jjg.game.common.pb.*;
import com.jjg.game.common.protostuff.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * 玩家网关服务器会话对象
 * @since 1.0
 */
public class GateSession extends NettyConnect<PFMessage> implements Inbox<PFMessage>, ConnectListener {

    public static Map<String, GateSession> gateSessionMap = new HashMap<>();

    protected Logger log = LoggerFactory.getLogger(getClass());
    /**
     * 当前所在节点
     */
    private ClusterClient currentClient;
    /**
     * 会话ID
     */
    public String sessionId;
    /**
     * 用户ID
     */
    public long playerId;
    /**
     * 是否已认证
     */
    public boolean certify;

    public long activeTime;

    public long createTime;

    public Connect connect;

    /**
     * 收到集群中其他节点发送过来的消息
     *
     * @param msg
     */
    @Override
    public void onClusterReceive(Connect connect, PFMessage msg) {
        write(msg);
    }

    /**
     * 接收客户端发送过来的消息
     *
     * @param msg
     */
    @Override
    public void messageReceived(PFMessage msg) {
        if (msg.cmd == MessageConst.ToClientConst.REQ_HEART_BEAT) {//拦截心跳
            activeTime = System.currentTimeMillis();
            PFMessage pfMessage = new PFMessage(MessageConst.ToClientConst.RES_HEART_BEAT, ProtostuffUtil.serialize(new ResHeartBeat(activeTime)));
            write(pfMessage);
            return;
        }

        //token验证
        if (msg.cmd == MessageConst.CertifyMessage.REQ_LOGIN) {
            log.info("收到请求登录消息");
            login(msg);
            return;
        }
        if(!certify){
            log.warn("未认证状态，不处理消息,且断开连接，message={}", msg);
            close();
            return;
        }


        ClusterMessage clusterMessage = new ClusterMessage(sessionId, msg, playerId);
        if (connect == null || !connect.isActive()) {
            if (connect != null) {
                currentClient.close(connect);
            }
            connect = getConnect();
        }
        connect.write(clusterMessage);
    }

    /**
     * 用户连接关闭
     */
    @Override
    public void onClose() {
        log.debug("连接断开,sessionId={}", sessionId);
        gateSessionMap.remove(sessionId);
        sendClose();
        if (playerId > 0) {
            // 移除session
            //向登录服务器发送用户下线
//            sendLogout();
        }
        if (connect != null) {
            connect.removeConnectListener(this);
        }
        this.currentClient = null;
        this.connect = null;
        this.ctx.close();
    }

    /**
     * 向当前连接节点发送关闭消息
     */
    private void sendClose() {
        if (connect == null || currentClient == null) {
            log.warn("向集群节点发送退出消息失败，连接为空");
            return;
        }
        SessionQuit sessionQuit = new SessionQuit();
        sessionQuit.sessionId = sessionId;
        PFMessage pfMessage = MessageUtil.getPFMessage(sessionQuit);
        ClusterMessage clusterMessage = new ClusterMessage(sessionId, pfMessage, playerId);
        connect.write(clusterMessage);
    }

    /**
     * 向当前连接节点发送进入消息
     */
    private void sendEnter(byte[] loginData) {
        if (connect == null || currentClient == null) {
            log.warn("向集群节点发送进入消息失败，连接为空");
            return;
        }
        log.debug("向集群节点发送进入消息,nodeName={},nodeAddress={}", currentClient.nodeConfig.getName(), currentClient.nodeConfig.getTcpAddress());
        SessionCreate sessionCreate = new SessionCreate();
        sessionCreate.sessionId = sessionId;
        sessionCreate.netAddress = remoteAddress;
        sessionCreate.playerId = playerId;
        sessionCreate.nodePath = ClusterSystem.system.getNodePath();
        sessionCreate.loginData = loginData;
        PFMessage pfMessage = MessageUtil.getPFMessage(sessionCreate);
        ClusterMessage clusterMessage = new ClusterMessage(sessionId, pfMessage, playerId);
        connect.write(clusterMessage);
    }

    /**
     * 用户连接建立
     */
    @Override
    public void onCreate() {
        sessionId = ctx.channel().id().asShortText();
        log.debug("连接服务器成功  sessionId={},net={}", sessionId, remoteAddress);
        String ip = remoteAddress.getHost();
        remoteAddress.setHost(ip);
    }

    protected void login(PFMessage msg){
        try{
            log.debug("网关收到客户端登录消息，sessionId={},netAddress={}", sessionId, remoteAddress);

            activeTime = createTime = System.currentTimeMillis();
            // 将session添加到集群节点中
            //gateSessionMap.put(sessionId, this);
            //连接建立成功以后，分配一个大厅服务器
            currentClient = ClusterSystem.system.getByNodeType(NodeType.HALL, remoteAddress.getHost(), playerId);

            if (currentClient == null) {
                log.debug("找不到可用的登录服务器");
                write(MessageUtil.getPFMessage(new NoticeServerStatus(NetStatEnum.NETWORK_CANT_USE)));
                return;
            }

            connect = getConnect();
            if (connect == null) {
                log.debug("登录服务器连接不可用");
                write(MessageUtil.getPFMessage(new NoticeServerStatus(NetStatEnum.NETWORK_CANT_USE)));
                return;
            }
            sendEnter(msg.data);
        }catch (Exception e){
            log.error("玩家登录异常",e);
        }
    }

//    protected boolean exchange(PFMessage msg){
//        try{
//            ReqAesExchange req = ProtostuffUtil.deserialize(msg.data, ReqAesExchange.class);
//            if(StringUtils.isEmpty(req.key)){
//                log.debug("密钥为空...");
//                return false;
//            }
//
//            //aeskey = RSA.decode(Base64.decode(req.key),RSA.toPrivateKey(Base64.decode(ClusterSystem.system.nodeConfig.privateKey)),1024);
//            if(aeskey == null){
//                log.debug("密钥为空11...");
//                return false;
//            }
//            write(MessageUtil.getPFMessage(new ResAesExchange(200)));
//            log.info("交换密钥成功  sessionId={}",sessionId);
//            return true;
//        }catch (Exception e){
//            log.error("交换密钥异常",e);
//        }
//        return false;
//    }

    public void switchNode(ClusterClient clusterClient) {
        log.debug("切换节点,sessionId={},playerId={},srcPath={},targetPath={},certify={}", sessionId, playerId, this.currentClient.nodeConfig.getName(), clusterClient.nodeConfig.getName(), certify);
        if (certify) {
            //向源节点发送退出
            sendClose();
            this.currentClient = clusterClient;
            this.connect = getConnect();
            sendEnter(null);
        }
    }

    public void onKickout() {
        log.info("用户被顶号下线，sessionId={}，playerId={}", sessionId, playerId);
        playerId = 0;
        try {
            writeAndClose(MessageUtil.getPFMessage(new NoticeServerStatus(NetStatEnum.PLAYER_KICKOUT)));
            this.channelInactive(this.ctx);
        } catch (Exception e) {
            log.warn("用户被顶号下线,消息发送异常，", e);
            e.printStackTrace();
        }
    }

    @Override
    public void onConnectClose(Connect connect) {
        log.warn("服务节点连接断开,playerId={},sessionId={},nodeAddress={}", playerId, sessionId, connect.address());
        try {
            writeAndClose(MessageUtil.getPFMessage(new NoticeServerStatus(NetStatEnum.NETWORK_CANT_USE)));
        } catch (Exception e) {
            log.warn("服务节点连接断开,消息发送异常，", e);
            e.printStackTrace();
        }
    }

    public Connect getConnect() {
        if (connect != null) {
            connect.removeConnectListener(this);
        }
        try {
            connect = currentClient.getConnectSync();
            connect.addConnectListener(this);
        } catch (Exception e) {
            log.warn("集群客户端获取连接异常,nodePath=" + currentClient.marsNode.getNodePath(), e);
            write(MessageUtil.getPFMessage(new NoticeServerStatus(NetStatEnum.NETWORK_CANT_USE)));
            gateSessionMap.remove(sessionId);
            close();
        }
        return connect;
    }

    public boolean isActive() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - createTime > 5000 && !certify) {
            return false;
        }
        if (currentTime - activeTime > 30000) {
            return false;
        }
        return true;
    }

    public void setHost(String hostIp) {
        if (hostIp != null && !hostIp.isEmpty()) {
            remoteAddress = new NetAddress(hostIp, remoteAddress != null ? remoteAddress.getPort() : 0);
        }
    }

    public ClusterClient getCurrentClient() {
        return currentClient;
    }

    @Override
    public String toString() {
        return "GateSession{" +
                "sessionId='" + sessionId + '\'' +
                ", playerId=" + playerId +
                ", certify=" + certify +
                ", connect=" + connect +
                '}';
    }
}
