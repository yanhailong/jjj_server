package com.vegasnight.game.common.micservice;

import com.vegasnight.game.common.protostuff.MessageType;
import com.vegasnight.game.common.protostuff.MessageUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 微服务管理器
 *
 * @since 1.0
 */
@Component
//public class MicServiceManager implements ApplicationListener<ContextRefreshedEvent> {
public class MicServiceManager {

    static Logger log = LoggerFactory.getLogger(MessageUtil.class);

    public Set<Integer> messageTypes = new HashSet<>();

    /*@Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        init(event.getApplicationContext());
    }*/
    public void init(ApplicationContext context){
        scanAndRegister(context);
    }

    public void scanAndRegister(ApplicationContext context) {
        Class<MicService> clazz = MicService.class;
        log.debug("开始扫描 {} 微服务", clazz);
        Map<String, Object> beans = context.getBeansWithAnnotation(clazz);
        beans.values().forEach(o -> {
            MessageType messageType = null;
            if (AopUtils.isAopProxy(o)) {
                messageType = AopUtils.getTargetClass(o).getAnnotation(MessageType.class);
            } else {
                messageType = o.getClass().getAnnotation(MessageType.class);
            }
            if (messageType == null) {
                log.debug("未被 MessageType 注解的微服务,{}", o.getClass());
            } else {
                log.debug("扫描到微服务,messageType={},class={}", messageType.value(), o.getClass());
                messageTypes.add(messageType.value());
            }
        });
    }
}
