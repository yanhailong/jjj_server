package com.jjg.game.core.manager;

import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.protostuff.PFSession;
import com.jjg.game.core.base.reddot.IRedDotService;
import com.jjg.game.core.pb.reddot.NotifyRedDot;
import com.jjg.game.core.pb.reddot.RedDotDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * 红点管理器
 * 负责管理所有红点服务的注册和调用
 */
@Component
public class RedDotManager {

    private static final Logger log = LoggerFactory.getLogger(RedDotManager.class);

    private final ClusterSystem clusterSystem;

    /**
     * 红点服务实例缓存
     * Key: 红点模块, Value: 服务实例
     */
    private final Map<RedDotDetails.RedDotModule, IRedDotService> redDotServiceCache = new ConcurrentHashMap<>();

    public RedDotManager(@Autowired ClusterSystem clusterSystem) {
        this.clusterSystem = clusterSystem;
    }

    /**
     * 注册红点服务
     *
     * @param module  红点模块
     * @param service 红点服务实例
     */
    public void registerService(RedDotDetails.RedDotModule module, IRedDotService service) {
        if (module != null && service != null) {
            redDotServiceCache.put(module, service);
            log.debug("注册红点服务: {} -> {}", module, service.getClass().getSimpleName());
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
                List<RedDotDetails> detailsList = load(module, 0, playerId);
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
    public List<RedDotDetails> load(RedDotDetails.RedDotModule module, int submodule, long playerId) {
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

    /**
     * 更新客户端的红点数据。
     *
     * @param supplier 用于提供红点详情列表的Supplier
     * @param playerId 玩家ID，如果小于等于0，则向所有在线玩家广播更新
     */
    public void updateRedDot(Supplier<List<RedDotDetails>> supplier, long playerId) {
        List<RedDotDetails> list = supplier.get();
        if (list == null || list.isEmpty()) {
            return;
        }
        updateRedDot(list, playerId);
    }

    /**
     * 通知客户端刷新红点数据
     *
     * @param redDotService 红点服务
     * @param submodule     子模块
     * @param playerId      玩家id 如果参数<=0则广播给所有在线玩家
     */
    public void updateRedDot(IRedDotService redDotService, int submodule, long playerId) {
        if (redDotService == null) {
            return;
        }
        List<RedDotDetails> details = redDotService.initialize(playerId, submodule);
        updateRedDot(details, playerId);
    }

}

