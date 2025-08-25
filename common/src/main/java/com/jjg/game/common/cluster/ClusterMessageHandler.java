package com.jjg.game.common.cluster;

import com.alibaba.fastjson.JSON;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.gate.GateSession;
import com.jjg.game.common.listener.*;
import com.jjg.game.common.message.*;
import com.jjg.game.common.net.Connect;
import com.jjg.game.common.netty.NettyConnect;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.common.protostuff.PFSession;
import com.jjg.game.common.rpc.ClusterRpcService;
import com.jjg.game.common.rpc.RpcClientService;
import com.jjg.game.common.utils.CommonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

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
    private Logger log = LoggerFactory.getLogger(getClass());
    @Autowired
    private ClusterSystem clusterSystem;
    @Autowired
    private ClusterRpcService clusterRpcService;
    @Autowired
    private RpcClientService rpcClientService;

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
        PFSession pfSession = clusterSystem.removeSession(sessionId);
        if (sessionCloseListenerMap != null && !sessionCloseListenerMap.isEmpty() && pfSession != null) {
            for (Map.Entry<String, SessionCloseListener> en : sessionCloseListenerMap.entrySet()) {
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
        if (resSessionVerifyPass.success) {
            if (this.sessionVerifyListenerMap != null && this.sessionVerifyListenerMap.size() > 0) {
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
        }
        pfSession.setAddress(sessionCreate.netAddress);
        clusterSystem.putSession(sessionId, pfSession);
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
    public void reqClusterRpcMessage(PFSession pfSession, ReqRpcServiceData req) {
        RespRpcServiceData resp = new RespRpcServiceData();
        if (req == null) {
            throw new RuntimeException("调用RPC时，参数为空");
        }
        log.debug("节点：{} 收到RPC消息:{}", clusterSystem.getNodePath(), req);
        resp.requestId = req.requestId;
        resp.success = false;
        Object provider = clusterRpcService.getProvider(req.serviceClassName);
        // 如果没有找到对应的Provider
        if (provider == null) {
            log.debug("节点：{} 找不到对应的服务提供者: {}", clusterSystem.getNodePath(), req.serviceClassName);
            // 发送返回数据
            pfSession.send(resp);
            return;
        }
        Map<String, Object> parameterNameOfData =
            JSON.parseObject(req.parameterTypeWithData);
        Class<?>[] parameterTypes = new Class[parameterNameOfData.size()];
        // 数据
        Object[] args = new Object[parameterNameOfData.size()];
        int i = 0;
        try {
            for (Map.Entry<String, Object> entry : parameterNameOfData.entrySet()) {
                parameterTypes[i++] = Class.forName(entry.getKey());
                args[i] = entry.getValue();
            }
            Method method = provider.getClass().getMethod(req.serviceMethodName, parameterTypes);
            method.setAccessible(true);
            // 调用provider中的方法
            Object o = method.invoke(provider, args);
            // 序列化后返回
            resp.responseData = JSON.toJSONString(o);
            resp.success = true;
            pfSession.send(resp);
            log.debug("向发送方：{} 返回调用RPC结果:{}", pfSession.gatePath, resp);
        } catch (NoSuchMethodException e) {
            log.error("调用RPC时，未找到类：{} 对应的方法：{}", req.serviceClassName, req.serviceMethodName, e);
        } catch (ClassNotFoundException e) {
            log.error("调用RPC时，通过类型未找到对应的类. {}", e.getMessage(), e);
        } catch (InvocationTargetException | IllegalAccessException e) {
            log.error("调用RPC时，发生逻辑异常 类：{} 对应的方法：{}", req.serviceClassName, req.serviceMethodName, e);
        }
    }


    /**
     * rpc消息返回
     *
     * @param res rpc消息
     */
    @Command(MessageConst.SessionConst.RPC_RES_SERVICE_DATA_CARRIER)
    public void resClusterRpcMessage(RespRpcServiceData res) {
        if (res == null || res.requestId == 0) {
            log.debug("收到RPC返回消息，但是节点：{} 接收的数据为空", clusterSystem.getNodePath());
            return;
        }
        CompletableFuture<RespRpcServiceData> completableFuture =
            rpcClientService.completeCompletableFuture(res.requestId);
        // 按道理不应为空
        if (completableFuture == null) {
            log.debug("节点：{} 找不到对应rpc：{} 的Future", clusterSystem.getNodePath(), res.requestId);
            return;
        }
        // 收到消息，调用完成
        completableFuture.complete(res);
    }
}
