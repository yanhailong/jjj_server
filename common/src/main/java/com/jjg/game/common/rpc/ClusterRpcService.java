package com.jjg.game.common.rpc;

import com.jjg.game.common.utils.CommonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * rpc服务
 *
 * @author 2CL
 */
@Service
public class ClusterRpcService {

    private static final Logger log = LoggerFactory.getLogger(ClusterRpcService.class);
    // 服务提供者map
    private final Map<String, Object> providerMap = new HashMap<>();

    /**
     * 初始化rpc服务提供类
     * TODO 待优化，可以向所有节点广播服务提供者的消息，【节点数据，类名，方法名】本地缓存后可以提高查询效率，减少IO请求
     */
    public void initProvider() {
        Map<String, Object> providers = CommonUtil.getContext().getBeansWithAnnotation(GameRpcService.class);
        if (!providers.isEmpty()) {
            providers.forEach((s, provider) -> log.info("发现rpc服务提供者：{}", s));
            providerMap.putAll(providers);
        }
    }

    /**
     * 注册服务提供者
     */
    public void registerProvider(String beanClassName, Object value) {
        log.info("注册rpc服务提供者：{}", beanClassName);
        providerMap.put(beanClassName, value);
    }

    /**
     * 获取服务提供者
     */
    public Object getProvider(String beanClassName) {
        return providerMap.get(beanClassName);
    }
}
