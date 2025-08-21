package com.jjg.game.common.cluster;

import com.jjg.game.common.concurrent.BaseFuncProcessor;
import com.jjg.game.common.concurrent.BaseHandler;
import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.common.protostuff.*;
import io.netty.channel.ChannelHandler;
import com.jjg.game.common.listener.SessionReferenceBinder;
import com.jjg.game.common.net.Connect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 节点消息分发
 *
 * @author nobody
 * @since 1.0
 */
@ChannelHandler.Sharable
public class ClusterMessageDispatcher {

    ClusterSystem clusterSystem;
    private Map<Integer, MessageController> messageControllers;
    private final Logger log = LoggerFactory.getLogger(getClass());
    private Map<String, SessionReferenceBinder> sessionRefenerceBinderMap;
    private final ClusterProcessorExecutors processorExecutors = ClusterProcessorExecutors.getInstance();

    public ClusterMessageDispatcher(ClusterSystem clusterSystem) {
        this.clusterSystem = clusterSystem;
    }

    /**
     * 初始化
     *
     * @param context
     */
    public void init(ApplicationContext context, Set<Integer> noStartGameMsgTypeSet) {
        this.sessionRefenerceBinderMap = context.getBeansOfType(SessionReferenceBinder.class);

        messageControllers = MessageUtil.load(context, noStartGameMsgTypeSet);
        MessageUtil.loadResponseMessage(noStartGameMsgTypeSet, CoreConst.Common.BASE_PROJECT_PACKAGE_PATH);
        messageControllers.forEach((key, value) -> log.info("消息处理器[{}]->{}", key, value.been.getClass().getName()));
    }

    public PFSession getPFSession(String id) {
        PFSession pfSession = clusterSystem.getSession(id);
//        if (pfSession==null){
//            return new PFSession();
//        }
        return pfSession;
    }

    /**
     * 收到消息
     *
     * @param connect
     * @param clusterMessage
     */
    public void onClusterReceive(Connect<ClusterMessage> connect, ClusterMessage clusterMessage) {
        String sessionId = clusterMessage.getSessionId();
        PFSession session = null;
        if (sessionId != null && !sessionId.trim().isEmpty()) {
            session = getPFSession(sessionId);
            if (session != null) {
                session.activeTime = System.currentTimeMillis();
            } else if (sessionRefenerceBinderMap != null && !sessionRefenerceBinderMap.isEmpty()) {
                session = new PFSession(sessionId, connect, null);
                //session.setReference();
                session.playerId = clusterMessage.getPlayerId();

                final PFSession pfSession = session;

                sessionRefenerceBinderMap.values().forEach(o -> {
                    o.bind(pfSession, clusterMessage.getPlayerId());
                });
            }
        }
        PFMessage msg = clusterMessage.getMsg();
        try {
            BaseFuncProcessor processor = processorExecutors.getProcessorById(
                session == null ? 0 : session.getWorkId());
            if (processor == null) {
                handle(connect, session, msg);
            } else {
                PFSession finalSession = session;
                processor.executeHandler(new BaseHandler<>() {
                    @Override
                    public void action() {
                        handle(connect, finalSession, msg);
                    }
                }.setHandlerParamWithSelf("handle message:" + msg.cmd));
            }
        } catch (Exception e) {
            log.warn("节点消息分发异常!", e);
        }
    }

    /**
     * 根据消息查找对应的controller或者handler进行处理
     *
     * @param connect
     * @param session
     * @param msg
     */
    public void handle(Connect<ClusterMessage> connect, PFSession session, PFMessage msg) {
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
                    // 处理分组消息
                    if (messageController.getMessageType().isGroupMessage()) {
                        if (handGropMessage(connect, session, msg, messageController)) {
                            return;
                        }
                    }
                    log.warn("找不到处理函数,bean={},messageType={},cmd={},hexCmd = {}", messageController.been,
                        "0x" + Integer.toHexString(messageType).toUpperCase(), command,
                        "0x" + Integer.toHexString(command).toUpperCase());
                    return;
                }
                // 调用消息具体实现方法
                invokeMessage(connect, session, msg, messageController, methodInfo);
            } else {
                log.warn("未被注册的消息,messageType={},cmd={},hex=0x{}", messageType, command, Integer.toHexString(command));
            }
        } catch (Exception e) {
            log.warn("消息解析错误,messageType={},cmd={},hex = 0x{}", messageType, command, Integer.toHexString(command), e);
        } finally {
            MDC.remove("playerId");
        }
    }

    /**
     * 处理分组消息
     */
    protected boolean handGropMessage(Connect<ClusterMessage> connect, PFSession session, PFMessage msg,
                                      MessageController messageController) throws Exception{
        Map<Integer, MethodInfo> methodInfos = messageController.MethodInfos;
        Set<MethodInfo> groupMsgDispatcher =
            methodInfos.values().stream().filter(val -> val.getCommandAnno().isGroupMsgDispatcher())
                .collect(Collectors.toSet());
        if (groupMsgDispatcher.isEmpty()) {
            return false;
        }
        MethodInfo methodInfo = groupMsgDispatcher.iterator().next();
        invokeMessage(connect, session, msg, messageController, methodInfo);
        return true;
    }

    /**
     * 调用消息实现方法
     */
    private void invokeMessage(Connect<ClusterMessage> connect, PFSession session, PFMessage msg,
                               MessageController messageController, MethodInfo methodInfo) throws Exception{

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
                } else if (PFMessage.class.isAssignableFrom(clazz)) {
                    args[i] = msg;
                } else {
                    if (msg.data != null && msg.data.length > 0) {
                        //System.out.println(Arrays.toString(msg.data));
                        args[i] = ProtostuffUtil.deserialize(msg.data, clazz);
                    }else {
                        Constructor<?> constructor = clazz.getConstructor();
                        args[i] = constructor.newInstance();
                    }
                }
            }

            if (session != null) {
                MDC.put("playerId", session.playerId + "-" + session.sessionId());
            }

            messageController.methodAccess.invoke(bean, methodInfo.index, args);
        } else {
            if (session != null) {
                MDC.put("playerId", session.playerId + "-" + session.sessionId());
            }
            messageController.methodAccess.invoke(bean, methodInfo.index);
        }
    }
}
