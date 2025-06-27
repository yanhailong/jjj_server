package com.jjg.game.common.cluster;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.gate.GateSession;
import com.jjg.game.common.listener.*;
import com.jjg.game.common.message.*;
import com.jjg.game.common.listener.*;
import com.jjg.game.common.message.*;
import com.jjg.game.common.net.Connect;
import com.jjg.game.common.netty.NettyConnect;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.common.protostuff.PFSession;
import com.jjg.game.common.utils.CommonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 集群消息处理器
 * @since 1.0
 */
@Component
@MessageType(MessageConst.MessageTypeDef.SESSION_TYPE)
public class ClusterMessageHandler {

    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private ClusterSystem clusterSystem;

    public Map<String, SessionVerifyListener> sessionVerifyListenerMap;
    public Map<String, SessionEnterListener> sessionEnterListenerMap;
    public Map<String, SessionCloseListener> sessionCloseListenerMap;
    public Map<String, SessionLogoutListener> sessionLogoutListenerMap;
    public Map<String, SessionLoginListener> sessionLoginListenerMap;

    public void init(){
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
        PFSession pfSession = clusterSystem.sessionMap().remove(sessionId);
        if (sessionCloseListenerMap != null && sessionCloseListenerMap.size() > 0 && pfSession != null) {
            for(Map.Entry<String,SessionCloseListener> en: sessionCloseListenerMap.entrySet()){
                en.getValue().sessionClose(pfSession);
            }
        }
    }

    /**
     * 认证成功后通知给网关服务器
     *
     * @param resSessionVerifyPass
     */
    @Command(MessageConst.SessionConst.RES_NOTIFY_SESSION_VERIFYPASS)
    public void sessionVerifyPass(ResSessionVerifyPass resSessionVerifyPass) {
        if(resSessionVerifyPass.success){
            if(this.sessionVerifyListenerMap != null && this.sessionVerifyListenerMap.size() > 0){
                this.sessionVerifyListenerMap.forEach((k,v) -> v.userVerifyPass(resSessionVerifyPass.sessionId, resSessionVerifyPass.playerId, resSessionVerifyPass.ip));
            }
        }else {
            GateSession gateSession = GateSession.gateSessionMap.get(resSessionVerifyPass.sessionId);
            if(gateSession != null){
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
    public void sessionEnter(PFSession pfSession, Connect connect, SessionCreate sessionCreate) {
        String sessionId = sessionCreate.sessionId;
        long playerId = sessionCreate.playerId;
        String gatePath = sessionCreate.nodePath;
        log.info("用户连接进入，sessionId={}", sessionId);
        if (pfSession == null) {
            pfSession = new PFSession(sessionId, connect, sessionCreate.netAddress);
        }
        pfSession.setAddress(sessionCreate.netAddress);
        clusterSystem.sessionMap().put(sessionId, pfSession);
        pfSession.gatePath = gatePath;

        if(sessionCreate.loginData != null && this.sessionLoginListenerMap != null && this.sessionLoginListenerMap.size() > 0){
            for(Map.Entry<String,SessionLoginListener> en : this.sessionLoginListenerMap.entrySet()){
                en.getValue().login(pfSession,sessionCreate.loginData);
            }
        }else{
            if(this.sessionEnterListenerMap != null && this.sessionEnterListenerMap.size() > 0){
                for(Map.Entry<String, SessionEnterListener> en : this.sessionEnterListenerMap.entrySet()){
                    en.getValue().sessionEnter(pfSession,playerId);
                }
            }
        }
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
        if(this.sessionLogoutListenerMap != null && this.sessionLogoutListenerMap.size() > 0){
            for(Map.Entry<String, SessionLogoutListener> en : this.sessionLogoutListenerMap.entrySet()){
                en.getValue().logout(playerId,sessionId);
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
        GateSession gateSession = GateSession.gateSessionMap.get(sessionId);
        if (gateSession != null) {
            gateSession.onKickout();
        }
    }

    @Command(MessageConst.SessionConst.CLUSTER_CONNECT_REGISTER)
    public void clusterRegister(NettyConnect connect, ClusterRegsiterMsg clusterRegsiterMsg) {
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
        GateSession gateSession = GateSession.gateSessionMap.get(sessionId);
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
        GateSession.gateSessionMap.forEach((k,v) -> {
            //广播给已经认证的用户
            if (v != null && v.isActive() && v.certify) {
                v.write(broadCastMessage.msg);
            }
        });
    }
}
