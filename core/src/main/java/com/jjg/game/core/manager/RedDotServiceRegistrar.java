package com.jjg.game.core.manager;

import com.jjg.game.core.base.reddot.IRedDotService;
import com.jjg.game.core.pb.reddot.RedDotDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 红点服务注册器
 * 使用Spring事件机制避免循环依赖问题
 */
@Component
public class RedDotServiceRegistrar {

    private static final Logger log = LoggerFactory.getLogger(RedDotServiceRegistrar.class);

    private final ApplicationContext applicationContext;

    private final RedDotManager redDotManager;

    public RedDotServiceRegistrar(ApplicationContext applicationContext, RedDotManager redDotManager) {
        this.applicationContext = applicationContext;
        this.redDotManager = redDotManager;
    }

    /**
     * 监听Spring容器刷新完成事件
     * 在容器完全初始化后注册红点服务
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onContextRefreshed() {
        try {
            Map<String, IRedDotService> beansOfType = this.applicationContext.getBeansOfType(IRedDotService.class);
            log.info("开始加载红点服务，发现服务类数量: {}", beansOfType.size());
            int successCount = 0;
            for (Map.Entry<String, IRedDotService> redDotServiceEntry : beansOfType.entrySet()) {
                String name = redDotServiceEntry.getKey();
                IRedDotService service = redDotServiceEntry.getValue();
                if (service != null) {
                    RedDotDetails.RedDotModule serviceModule = service.getModule();
                    if (serviceModule != null && !serviceModule.isNeedTrusteeship()) {
                        redDotManager.registerService(serviceModule, service);
                        log.debug("成功注册红点服务: {} -> {}", serviceModule, name);
                    } else {
                        log.warn("红点服务 {} 的模块为托管", name);
                    }
                    successCount++;
                }
            }
            log.info("红点服务加载完成，成功注册: {}/{}", successCount, beansOfType.size());
        } catch (Exception e) {
            log.error("初始化红点管理器失败", e);
            throw new RuntimeException("红点管理器初始化失败", e);

        }
    }


}
