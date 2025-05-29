package com.vegasnight.game.common.cluster;

import io.netty.channel.ChannelHandler;
import com.vegasnight.game.common.executor.MarsWorkExecutor;
import com.vegasnight.game.common.listener.SessionRefenerceBinder;
import com.vegasnight.game.common.net.Connect;
import com.vegasnight.game.common.protostuff.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import java.util.Map;

/**
 * @since 1.0
 */
@ChannelHandler.Sharable
public class ClusterMessageDispacher {

    private Map<Integer, MessageController> messageControllers;

    private Logger log = LoggerFactory.getLogger(getClass());

    ClusterSystem clusterSystem;
    private Map<String, SessionRefenerceBinder> sessionRefenerceBinderMap;

    @Autowired(required = false)
    public MarsWorkExecutor marsWorkExecutor;

    public ClusterMessageDispacher(ClusterSystem clusterSystem) {
        this.clusterSystem = clusterSystem;
    }

    public PFSession getPFSession(String id) {
        PFSession pfSession = clusterSystem.sessionMap().get(id);
//        if (pfSession==null){
//            return new PFSession();
//        }
        return pfSession;
    }

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
            final PFSession pfSession = session;
            // 此处如果用户有工作ID，采用工作线程提交
            if (session != null && session.workId > 0 && marsWorkExecutor != null) {
                marsWorkExecutor.submit(session.workId, () -> handle(connect, pfSession, msg));
            } else {
                handle(connect, session, msg);
            }
        } catch (Exception e) {
            log.warn("", e);
        }
    }

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
                    messageController.methodAccess.invoke(bean, methodInfo.index, args);
                } else {
                    messageController.methodAccess.invoke(bean, methodInfo.index);
                }
            } else {
                log.warn("未被注册的消息,messageType={},cmd={},hex=0x{}", messageType, command,Integer.toHexString(command));
            }
        } catch (Exception e) {
            log.warn("消息解析错误,messageType=" + messageType + ",cmd=" + command + ",hex = 0x" + Integer.toHexString(command), e);
        }
    }

    public void init(ApplicationContext context){
        this.sessionRefenerceBinderMap = context.getBeansOfType(SessionRefenerceBinder.class);

        messageControllers = MessageUtil.load(context);
        MessageUtil.loadResponseMessage("com.vegasnight.game");
        messageControllers.entrySet().forEach(e -> log.info("消息处理器[{}]->{}", e.getKey(), e.getValue().been.getClass().getName()));
    }

    /*@Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        init(event.getApplicationContext());
    }*/
}
