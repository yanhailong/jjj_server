package com.jjg.game.common.baselogic.function;

import com.jjg.game.common.baselogic.IGameSysFuncInterface;
import org.apache.commons.lang3.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 系统接口和对应实现类的映射关系保存类
 *
 * @author 2CL
 */
@Component
public class SystemInterfaceHolder implements BeanPostProcessor {

    /**
     * 接口map缓存
     */
    private final static Map<Class<? extends IGameSysFuncInterface>, List<IGameSysFuncInterface>> INTERFACE_MAP =
        new HashMap<>();
    private static final Logger log = LoggerFactory.getLogger(SystemInterfaceHolder.class);

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (IGameSysFuncInterface.class.isAssignableFrom(bean.getClass())) {
            // 获取bean中所有的功能接口
            Set<Class<IGameSysFuncInterface>> funcSet =
                ClassUtils.getAllInterfaces(bean.getClass()).stream()
                    .filter(IGameSysFuncInterface.class::isAssignableFrom)
                    .map(f -> (Class<IGameSysFuncInterface>) f)
                    .collect(Collectors.toSet());
            for (Class<IGameSysFuncInterface> sysFuncInterfaceClass : funcSet) {
                INTERFACE_MAP
                    .computeIfAbsent(sysFuncInterfaceClass, k -> new ArrayList<>())
                    .add((IGameSysFuncInterface) bean);
            }
        }
        return BeanPostProcessor.super.postProcessAfterInitialization(bean, beanName);
    }

    /**
     * 获取对应类的所有实现
     */
    public static <T extends IGameSysFuncInterface> List<T> getGameSysInterface(Class<T> clazz) {
        return Collections.unmodifiableList((List<T>) INTERFACE_MAP.getOrDefault(clazz, new ArrayList<>())
            .stream()
            .sorted(Comparator.comparingInt(IGameSysFuncInterface::executeOrder)));
    }

    /**
     * 调用功能接口中具体的方法
     *
     * @param clazz            功能接口类
     * @param consumePerModule 调用功能的具体方法行为
     * @param <T>              T
     */
    public static <T extends IGameSysFuncInterface> void callGameSysAction(
        Class<T> clazz, Consumer<T> consumePerModule) {
        INTERFACE_MAP.getOrDefault(clazz, new ArrayList<>()).stream()
            .sorted(Comparator.comparingInt(IGameSysFuncInterface::executeOrder))
            .forEach(t -> {
                try {
                    consumePerModule.accept((T) t);
                } catch (Exception e) {
                    log.error("调用功能接口：{} 对应的实现：{} 发生异常！{}",
                        clazz.getSimpleName(), t.getClass().getName(), e.getMessage(), e);
                }
            });
    }
}
