package com.jjg.game.core.manager;

import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.protostuff.PFSession;
import com.jjg.game.core.base.reddot.IRedDotService;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.pb.reddot.NotifyRedDot;
import com.jjg.game.core.pb.reddot.RedDotDetails;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 红点管理器
 * 负责管理所有红点服务的注册和调用
 */
@Component
public class RedDotManager implements ApplicationContextAware {

    private static final Logger log = LoggerFactory.getLogger(RedDotManager.class);

    private ApplicationContext applicationContext;

    private final ClusterSystem clusterSystem;

    /**
     * 红点服务实例缓存
     * Key: 红点模块, Value: 服务实例
     */
    private final Map<RedDotDetails.RedDotModule, IRedDotService> redDotServiceCache = new ConcurrentHashMap<>();

    public RedDotManager(@Autowired ClusterSystem clusterSystem) {
        this.clusterSystem = clusterSystem;
    }

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @PostConstruct
    public void init() {
        try {
            Map<String, IRedDotService> beansOfType = this.applicationContext.getBeansOfType(IRedDotService.class);
            log.info("开始加载红点服务，发现服务类数量: {}", beansOfType.size());
            int successCount = 0;
            for (Map.Entry<String, IRedDotService> redDotServiceEntry : beansOfType.entrySet()) {
                String name = redDotServiceEntry.getKey();
                IRedDotService service = redDotServiceEntry.getValue();
                if (service != null) {
                    RedDotDetails.RedDotModule serviceModule = service.getModule();
                    if (serviceModule != null) {
                        redDotServiceCache.put(serviceModule, service);
                        successCount++;
                        log.debug("成功注册红点服务: {} -> {}", serviceModule, name);
                    } else {
                        log.warn("红点服务 {} 的模块为空，跳过注册", name);
                    }
                }
            }
            log.info("红点服务加载完成，成功注册: {}/{}", successCount, beansOfType.size());
        } catch (Exception e) {
            log.error("初始化红点管理器失败", e);
            throw new RuntimeException("红点管理器初始化失败", e);
        }
    }

    /**
     * 加载所有红点数据
     *
     * @param playerId 玩家ID
     * @return 所有红点详情列表
     */
    public List<RedDotDetails> loadAll(long playerId) {
        if (redDotServiceCache.isEmpty()) {
            log.warn("没有可用的红点服务，玩家ID: {}", playerId);
            return Collections.emptyList();
        }

        List<RedDotDetails> allRedDots = new ArrayList<>();
        for (Map.Entry<RedDotDetails.RedDotModule, IRedDotService> entry : redDotServiceCache.entrySet()) {
            try {
                RedDotDetails.RedDotModule module = entry.getKey();
                List<RedDotDetails> detailsList = load(module, null, playerId);
                if (detailsList != null && !detailsList.isEmpty()) {
                    allRedDots.addAll(detailsList);
                }
            } catch (Exception e) {
                log.error("加载红点数据失败，玩家ID: {}, 模块: {}", playerId, entry.getKey(), e);
            }
        }
        log.debug("玩家 {} 总共加载了 {} 个红点", playerId, allRedDots.size());
        return allRedDots;
    }

    /**
     * 加载指定模块的红点数据
     *
     * @param module   红点模块
     * @param playerId 玩家ID
     * @return 指定模块的红点详情列表，如果模块不存在则返回空列表
     */
    public List<RedDotDetails> load(RedDotDetails.RedDotModule module, RedDotDetails.RedDotSubmodule submodule, long playerId) {
        if (module == null) {
            log.warn("红点模块为空，玩家ID: {}", playerId);
            return Collections.emptyList();
        }
        IRedDotService service = redDotServiceCache.get(module);
        if (service == null) {
            log.warn("未找到红点模块 {} 对应的服务，玩家ID: {}", module, playerId);
            return Collections.emptyList();
        }
        try {
            List<RedDotDetails> redDots = service.initialize(playerId, submodule);
            if (redDots == null) {
                return Collections.emptyList();
            }
            log.debug("玩家 {} 的 {} 模块加载了 {} 个红点", playerId, module, redDots.size());
            return redDots;
        } catch (Exception e) {
            log.error("加载红点数据失败，玩家ID: {}, 模块: {}", playerId, module, e);
            return Collections.emptyList();
        }
    }

    /**
     * 通知客户端刷新红点数据
     *
     * @param list     红点数据列表
     * @param playerId 玩家id 如果参数<=0则广播给所有在线玩家
     */
    public void updateRedDot(List<RedDotDetails> list, long playerId) {
        if (list == null || list.isEmpty()) {
            return;
        }
        NotifyRedDot notifyRedDot = new NotifyRedDot();
        notifyRedDot.setRedDotList(list);
        if (playerId > 0) {
            PFSession session = clusterSystem.getSession(playerId);
            if (session == null) {
                return;
            }
            session.send(notifyRedDot);
        } else {
            clusterSystem.broadcastToOnlinePlayer(notifyRedDot);
        }
    }

}

