package com.jjg.game.core.manager;

import com.jjg.game.common.config.NodeConfig;
import com.jjg.game.common.curator.NodeType;
import com.jjg.game.core.base.reddot.IRedDotService;
import com.jjg.game.core.pb.reddot.RedDotDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/**
 * 红点服务注册器
 * 使用Spring事件机制避免循环依赖问题
 */
@Component
public class RedDotServiceRegistrar {

    private static final Logger log = LoggerFactory.getLogger(RedDotServiceRegistrar.class);

    private final ApplicationContext applicationContext;

    private final RedDotManager redDotManager;

    private final NodeConfig nodeConfig;

    public RedDotServiceRegistrar(ApplicationContext applicationContext, RedDotManager redDotManager,
                                  NodeConfig nodeConfig) {
        this.applicationContext = applicationContext;
        this.redDotManager = redDotManager;
        this.nodeConfig = nodeConfig;
    }

    /** 应用启动完成后，按当前节点类型注册红点服务。 */
    @EventListener(ApplicationReadyEvent.class)
    public void onContextRefreshed() {
        try {
            NodeType currentNodeType = NodeType.getNodeTypeByName(nodeConfig.getType());
            if (currentNodeType == null) {
                throw new IllegalStateException("无法识别当前节点类型: " + nodeConfig.getType());
            }

            Map<String, IRedDotService> beansOfType = applicationContext.getBeansOfType(IRedDotService.class);
            log.info("开始加载红点服务，当前节点类型: {}，发现服务类数量: {}",currentNodeType, beansOfType.size());
            int registeredCount = 0;
            for (Map.Entry<String, IRedDotService> redDotServiceEntry : beansOfType.entrySet()) {
                String name = redDotServiceEntry.getKey();
                IRedDotService service = redDotServiceEntry.getValue();
                if (service == null) {
                    continue;
                }

                Set<NodeType> supportedNodeTypes = service.getSupportedNodeTypes();
                if (!supportedNodeTypes.contains(currentNodeType)) {
                    log.debug("当前节点不加载红点服务: {}，支持节点类型: {}", name, supportedNodeTypes);
                    continue;
                }

                RedDotDetails.RedDotModule serviceModule = service.getModule();
                if (serviceModule == null) {
                    log.warn("红点服务 {} 未指定模块", name);
                    continue;
                }
                for (Integer submodule : service.getSubmodules()) {
                    redDotManager.registerService(serviceModule, submodule, service);
                }
                log.debug("成功注册红点服务: {} -> {}", serviceModule, name);
                registeredCount++;
            }
            log.info("红点服务加载完成，当前节点成功注册: {}/{}", registeredCount, beansOfType.size());
        } catch (Exception e) {
            log.error("初始化红点管理器失败", e);
            throw new RuntimeException("红点管理器初始化失败", e);

        }
    }


}
