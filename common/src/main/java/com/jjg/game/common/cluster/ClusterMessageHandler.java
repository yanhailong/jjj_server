package com.jjg.game.common.cluster;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.gate.GateSession;
import com.jjg.game.common.listener.*;
import com.jjg.game.common.message.*;
import com.jjg.game.common.net.Connect;
import com.jjg.game.common.netty.NettyConnect;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.common.protostuff.PFSession;
import com.jjg.game.common.rpc.RpcServerService;
import com.jjg.game.common.rpc.msg.ReqRpcServiceData;
import com.jjg.game.common.rpc.msg.RespRpcServiceData;
import com.jjg.game.common.utils.CommonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 集群消息处理器
 *
 * @since 1.0
 */
@Component
@MessageType(MessageConst.MessageTypeDef.SESSION_TYPE)
public class ClusterMessageHandler {

    public Map<String, SessionVerifyListener> sessionVerifyListenerMap;
    public Map<String, SessionEnterListener> sessionEnterListenerMap;
    public Map<String, SessionCloseListener> sessionCloseListenerMap;
    public Map<String, SessionLogoutListener> sessionLogoutListenerMap;
    public Map<String, SessionLoginListener> sessionLoginListenerMap;
    private final Logger log = LoggerFactory.getLogger(getClass());
    @Autowired
    private ClusterSystem clusterSystem;
    @Autowired
    private RpcServerService rpcServerService;


    public void init() {
        sessionVerifyListenerMap = CommonUtil.getContext().getBeansOfType(SessionVerifyListener.class);
        sessionEnterListenerMap = CommonUtil.getContext().getBeansOfType(SessionEnterListener.class);
        sessionCloseListenerMap = CommonUtil.getContext().getBeansOfType(SessionCloseListener.class);
        sessionLogoutListenerMap = CommonUtil.getContext().getBeansOfType(SessionLogoutListener.class);
        sessionLoginListenerMap = CommonUtil.getContext().getBeansOfType(SessionLoginListener.class);
    }

    /**
     * 连接断开退出
     */
    @Command(MessageConst.SessionConst.NOTIFY_SESSION_QUIT)
    public void sessionClose(SessionQuit sessionQuit) {
        String sessionId = sessionQuit.sessionId;
        log.info("用户连接退出，sessionId={}", sessionId);
        PFSession pfSession = clusterSystem.getSession(sessionId);
        if (sessionCloseListenerMap != null && !sessionCloseListenerMap.isEmpty() && pfSession != null) {
            for (Map.Entry<String, SessionCloseListener> en : sessionCloseListenerMap.entrySet()) {
                en.getValue().sessionClose(pfSession);
            }
        }
        //移除session
        clusterSystem.removeSession(sessionId);
    }

    /**
     * 认证成功后通知给网关服务器
     *
     * @param resSessionVerifyPass
     */
    @Command(MessageConst.SessionConst.RES_NOTIFY_SESSION_VERIFYPASS)
    public void sessionVerifyPass(ResSessionVerifyPass resSessionVerifyPass) {
        if (resSessionVerifyPass.success) {
            if (this.sessionVerifyListenerMap != null && !this.sessionVerifyListenerMap.isEmpty()) {
                this.sessionVerifyListenerMap.forEach((k, v) -> v.userVerifyPass(resSessionVerifyPass.sessionId,
                        resSessionVerifyPass.playerId, resSessionVerifyPass.ip));
            }
        } else {
            GateSession gateSession = GateSession.getGateSessionMap().get(resSessionVerifyPass.sessionId);
            if (gateSession != null) {
                gateSession.close();
            }
        }
    }

    /**
     * 收到session进入消息
     *
     * @param sessionCreate
     */
    @Command(MessageConst.SessionConst.NOTIFY_SESSION_ENTER)
    public void sessionEnter(PFSession pfSession, Connect<Object> connect, SessionCreate sessionCreate) {
        String sessionId = sessionCreate.sessionId;
        long playerId = sessionCreate.playerId;
        String gatePath = sessionCreate.nodePath;
        log.info("用户连接进入，sessionId={}", sessionId);
        if (pfSession == null) {
            pfSession = new PFSession(sessionId, connect, sessionCreate.netAddress);
            pfSession.setPlayerId(playerId);
        }
        pfSession.setAddress(sessionCreate.netAddress);
        pfSession.gatePath = gatePath;
        if (sessionCreate.loginData != null && this.sessionLoginListenerMap != null && !this.sessionLoginListenerMap.isEmpty()) {
            for (Map.Entry<String, SessionLoginListener> en : this.sessionLoginListenerMap.entrySet()) {
                en.getValue().login(pfSession, sessionCreate.loginData);
            }
        } else {
            if (this.sessionEnterListenerMap != null && !this.sessionEnterListenerMap.isEmpty()) {
                for (Map.Entry<String, SessionEnterListener> en : this.sessionEnterListenerMap.entrySet()) {
                    en.getValue().sessionEnter(pfSession, playerId);
                }
            }
        }
        clusterSystem.putSession(pfSession.sessionId(), pfSession);
    }

    /**
     * session下线
     *
     * @param sessionLogout
     */
    @Command(MessageConst.SessionConst.NOTIFY_SESSION_LOGOUT)
    public void sessionLogout(Connect<ClusterMessage> connect, SessionLogout sessionLogout) {
        String sessionId = sessionLogout.sessionId;
        long playerId = sessionLogout.playerId;
        log.info("用户下线，sessionId={}，playerId={}", sessionId, playerId);
        if (this.sessionLogoutListenerMap != null && !this.sessionLogoutListenerMap.isEmpty()) {
            for (Map.Entry<String, SessionLogoutListener> en : this.sessionLogoutListenerMap.entrySet()) {
                en.getValue().logout(playerId, sessionId);
            }
        }
    }

    /**
     * 踢出用户下线
     *
     * @param sessionKickout
     */
    @Command(MessageConst.SessionConst.NOTIFY_SESSION_KICKOUT)
    public void sessionKickout(Connect<ClusterMessage> connect, SessionKickout sessionKickout) {
        String sessionId = sessionKickout.sessionId;
        long playerId = sessionKickout.playerId;
        log.info("用户被顶号下线，sessionId={}，playerId={}", sessionId, playerId);
        GateSession gateSession = GateSession.getGateSessionMap().get(sessionId);
        if (gateSession != null) {
            gateSession.onKickout();
        }
    }

    @Command(MessageConst.SessionConst.CLUSTER_CONNECT_REGISTER)
    public void clusterRegister(NettyConnect<Object> connect, ClusterRegsiterMsg clusterRegsiterMsg) {
        if (clusterRegsiterMsg == null) {
            log.debug("节点注册异常,connect={}", connect);
            return;
        }
        String nodePath = clusterRegsiterMsg.nodePath;

        ClusterClient clusterClient = clusterSystem.getClusterByPath(nodePath);
        if (clusterClient != null) {
            clusterClient.connectPool.addConnect(connect);
            log.debug("节点注册成功,nodePath={},connect={}", nodePath, connect);
        }
    }

    @Command(MessageConst.SessionConst.NOTIFY_SWITCH_NODE)
    public void switchNode(SwitchNodeMessage switchNodeMessage) {
        String targetNodePath = switchNodeMessage.targetNodePath;
        String sessionId = switchNodeMessage.sessionId;
        ClusterClient clusterClient = clusterSystem.getClusterByPath(targetNodePath);
        GateSession gateSession = GateSession.getGateSessionMap().get(sessionId);
        if (gateSession != null && clusterClient != null) {
            gateSession.switchNode(clusterClient);
        } else {
            log.warn("找不到gate session，sessionId={},gateSession={}", sessionId, gateSession);
            //GateSession.gateSessionMap.forEach((k,v)-> log.debug(k + "->" + v));
        }
    }

    /**
     * 该消息需要广播给所有用户
     *
     * @param broadCastMessage
     */
    @Command(MessageConst.SessionConst.BROADCAST_MSG)
    public void broadcast(BroadCastMessage broadCastMessage) {
        // TODO GateSession过多时应批量分段分时进行广播，否则容易在同一时段拉满宽带阻塞其他正常逻辑的响应
        GateSession.getGateSessionMap().forEach((k, v) -> {
            //广播给已经认证的用户
            if (v != null && v.isActive() && v.isCertify()) {
                v.write(broadCastMessage.msg);
            }
        });
    }

    /**
     * rpc消息请求
     *
     * @param req rpc消息
     */
    @Command(MessageConst.SessionConst.RPC_REQ_SERVICE_DATA_CARRIER)
    public void reqClusterRpcMessage(ClusterConnect clusterConnect, ReqRpcServiceData req) {
        rpcServerService.reqClusterRpcMessage(clusterConnect, req);
    }


    /**
     * rpc消息返回
     *
     * @param res rpc消息
     */
    @Command(MessageConst.SessionConst.RPC_RES_SERVICE_DATA_CARRIER)
    public void resClusterRpcMessage(RespRpcServiceData res) {
        rpcServerService.resClusterRpcMessage(res);
    }
}
