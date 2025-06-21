package com.jjg.game.common.cluster;

import com.jjg.game.common.protostuff.*;
import com.jjg.game.common.utils.RandomUtils;
import io.netty.channel.ChannelHandler;
import com.jjg.game.common.listener.SessionRefenerceBinder;
import com.jjg.game.common.net.Connect;
import com.jjg.game.common.protostuff.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.ApplicationContext;

import java.util.Map;

/**
 * 节点消息分发
 * @since 1.0
 */
@ChannelHandler.Sharable
public class ClusterMessageDispacher {

    private Map<Integer, MessageController> messageControllers;

    private Logger log = LoggerFactory.getLogger(getClass());

    ClusterSystem clusterSystem;
    private Map<String, SessionRefenerceBinder> sessionRefenerceBinderMap;

    public ClusterMessageDispacher(ClusterSystem clusterSystem) {
        this.clusterSystem = clusterSystem;
    }

    /**
     * 初始化
     * @param context
     */
    public void init(ApplicationContext context){
        this.sessionRefenerceBinderMap = context.getBeansOfType(SessionRefenerceBinder.class);

        messageControllers = MessageUtil.load(context);
        MessageUtil.loadResponseMessage("com.jjg.game");
        messageControllers.entrySet().forEach(e -> log.info("消息处理器[{}]->{}", e.getKey(), e.getValue().been.getClass().getName()));
    }

    public PFSession getPFSession(String id) {
        PFSession pfSession = clusterSystem.sessionMap().get(id);
//        if (pfSession==null){
//            return new PFSession();
//        }
        return pfSession;
    }

    /**
     * 收到消息
     * @param connect
     * @param clusterMessage
     */
    public void onClusterReceive(Connect connect, ClusterMessage clusterMessage) {
        String sessionId = clusterMessage.sessionId;
        PFSession session = null;
        if (sessionId != null && !sessionId.trim().isEmpty()) {
            session = getPFSession(sessionId);
            if (session != null) {
                session.activeTime = System.currentTimeMillis();
            } else if (sessionRefenerceBinderMap != null && sessionRefenerceBinderMap.size() > 0) {
                session = new PFSession(sessionId, connect, null);
                //session.setReference();
                session.playerId = clusterMessage.playerId;

                final PFSession pfSession = session;

                sessionRefenerceBinderMap.values().forEach(o -> {
                    o.bind(pfSession,clusterMessage.playerId);
                });
            }
        }
        PFMessage msg = clusterMessage.msg;
        try {
            handle(connect, session, msg);
        } catch (Exception e) {
            log.warn("", e);
        }
    }

    /**
     * 根据消息查找对应的controller或者handler进行处理
     * @param connect
     * @param session
     * @param msg
     */
    public void handle(Connect connect, PFSession session, PFMessage msg) {
        int messageType = 0;
        int command = 0;
        try {
            //log.debug("打印handle  connect={},session={},msg={}",connect,session,msg);
            messageType = msg.messageType;
            command = msg.cmd;
            MessageController messageController = messageControllers.get(messageType);
            if (messageController != null) {
                MethodInfo methodInfo = messageController.MethodInfos.get(command);
                if (methodInfo == null) {
                    log.warn("找不到处理函数,bean={},messageType={},cmd={},hexCmd = {}", messageController.been,"0x" + Integer.toHexString(messageType).toUpperCase(), command,"0x" + Integer.toHexString(command).toUpperCase());
                    return;
                }
                Object bean = messageController.been;
                if (methodInfo.parms != null && methodInfo.parms.length > 0) {
                    Object[] args = new Object[methodInfo.parms.length];
                    Object reference = null;
                    if (session != null) {
                        reference = session.getReference();
                    }
                    for (int i = 0; i < args.length; i++) {
                        Class<?> clazz = methodInfo.parms[i];
                        if (clazz == PFSession.class) {
                            args[i] = session;
                        } else if (reference != null && clazz == reference.getClass()) {
                            args[i] = reference;
                        } else if (Connect.class.isAssignableFrom(clazz)) {
                            args[i] = connect;
                        } else {
                            if (msg.data != null && msg.data.length > 0) {
                                //System.out.println(Arrays.toString(msg.data));
                                args[i] = ProtostuffUtil.deserialize(msg.data, clazz);
                            }
                        }
                    }

                    if(session != null){
                        MDC.put("playerId", session.playerId + "-" + session.sessionId());
                    }

//                    log.debug("cmd = {},msg.data = {}", command,msg.data);
                    messageController.methodAccess.invoke(bean, methodInfo.index, args);
                } else {
                    if(session != null){
                        MDC.put("playerId", session.playerId + "-" + session.sessionId());
                    }
                    messageController.methodAccess.invoke(bean, methodInfo.index);
                }
            } else {
                log.warn("未被注册的消息,messageType={},cmd={},hex=0x{}", messageType, command,Integer.toHexString(command));
            }
        } catch (Exception e) {
            log.warn("消息解析错误,messageType=" + messageType + ",cmd=" + command + ",hex = 0x" + Integer.toHexString(command), e);
        } finally {
            MDC.remove("playerId");
        }
    }
}
