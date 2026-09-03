package com.jjg.game.core.manager;

import cn.hutool.core.collection.CollectionUtil;
import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.curator.MarsNode;
import com.jjg.game.common.curator.NodeType;
import com.jjg.game.common.protostuff.PFSession;
import com.jjg.game.core.base.reddot.IRedDotService;
import com.jjg.game.core.dao.RedDotDao;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.data.PlayerSessionInfo;
import com.jjg.game.core.pb.reddot.NotifyRedDot;
import com.jjg.game.core.pb.reddot.RedDotDetails;
import com.jjg.game.core.service.PlayerSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 红点管理器
 * 负责管理所有红点服务的注册和调用
 */
@Component
public class RedDotManager {

    private static final Logger log = LoggerFactory.getLogger(RedDotManager.class);

    private final ClusterSystem clusterSystem;
    private final PlayerSessionService playerSessionService;
    private final RedDotDao redDotDao;
    /**
     * 红点服务实例缓存
     * Key: 红点模块, Value: {子模块id->服务实例}
     */
    private final Map<RedDotDetails.RedDotModule, Map<Integer, IRedDotService>> redDotServiceMap = new ConcurrentHashMap<>();

    public RedDotManager(@Autowired ClusterSystem clusterSystem, PlayerSessionService playerSessionService, RedDotDao redDotDao) {
        this.clusterSystem = clusterSystem;
        this.playerSessionService = playerSessionService;
        this.redDotDao = redDotDao;
    }


    /**
     * 注册红点服务
     *
     * @param module  红点模块
     * @param service 红点服务实例
     */
    public void registerService(RedDotDetails.RedDotModule module, IRedDotService service) {
        if (module != null && service != null) {
            registerService(module, service.getSubmodule(), service);
        }
    }

    public void registerService(RedDotDetails.RedDotModule module, int submodule, IRedDotService service) {
        if (module != null && service != null) {
            Map<Integer, IRedDotService> serviceMap = redDotServiceMap.computeIfAbsent(module, key -> new ConcurrentHashMap<>());
            serviceMap.put(submodule, service);
            log.debug("注册红点服务: {}-{} -> {}", module, submodule, service.getClass().getSimpleName());
        }
    }

    private boolean isServiceRegistered(RedDotDetails.RedDotModule module, int submodule) {
        Map<Integer, IRedDotService> serviceMap = redDotServiceMap.get(module);
        return CollectionUtil.isNotEmpty(serviceMap) && (submodule == 0 || serviceMap.containsKey(submodule));
    }

    private boolean isServiceSupported(RedDotDetails.RedDotModule module, int submodule, NodeType nodeType) {
        Map<Integer, IRedDotService> serviceMap = redDotServiceMap.get(module);
        if (nodeType == null || CollectionUtil.isEmpty(serviceMap)) {
            return false;
        }
        if (submodule == 0) {
            return serviceMap.values().stream()
                    .anyMatch(service -> service.getSupportedNodeTypes().contains(nodeType));
        }
        IRedDotService service = serviceMap.get(submodule);
        return service != null && service.getSupportedNodeTypes().contains(nodeType);
    }

    private RedDotRecipient resolveRecipient(long playerId) {
        PFSession localSession = clusterSystem.getSession(playerId);
        if (localSession != null) {
            return new RedDotRecipient(localSession,
                    NodeType.getNodeTypeByName(clusterSystem.nodeConfig.getType()), null);
        }

        PlayerSessionInfo sessionInfo = playerSessionService.getInfo(playerId);
        if (sessionInfo == null) {
            return null;
        }
        NodeType nodeType = resolveNodeType(sessionInfo.getCurrentNode());
        return nodeType == null ? null : new RedDotRecipient(null, nodeType, sessionInfo);
    }

    private NodeType resolveNodeType(String nodePath) {
        if (nodePath == null) {
            return null;
        }
        if (Objects.equals(clusterSystem.getNodePath(), nodePath)) {
            return NodeType.getNodeTypeByName(clusterSystem.nodeConfig.getType());
        }
        MarsNode node = clusterSystem.getNode(nodePath);
        return node == null || node.getNodeConfig() == null
                ? null
                : NodeType.getNodeTypeByName(node.getNodeConfig().getType());
    }

    private void sendRedDots(List<RedDotDetails> list, RedDotRecipient recipient) {
        List<RedDotDetails> supportedRedDots = new ArrayList<>(list.size());
        for (RedDotDetails details : list) {
            if (isServiceSupported(details.getRedDotModule(), details.getRedDotSubmodule(), recipient.nodeType())) {
                supportedRedDots.add(details);
            }
        }
        if (supportedRedDots.isEmpty()) {
            return;
        }
        PFSession session = recipient.session() != null
                ? recipient.session()
                : playerSessionService.getSession(recipient.sessionInfo());
        if (session == null) {
            return;
        }
        NotifyRedDot notifyRedDot = new NotifyRedDot();
        notifyRedDot.setRedDotList(supportedRedDots);
        session.send(notifyRedDot);
    }

    private record RedDotRecipient(PFSession session, NodeType nodeType, PlayerSessionInfo sessionInfo) {
    }

    /**
     * 加载所有红点数据
     *
     * @param playerId 玩家ID
     * @return 所有红点详情列表
     */
    public List<RedDotDetails> loadAll(long playerId) {
        //获取需要存储的红点信息
        List<RedDotDetails> allRedDots = new ArrayList<>();
        boolean hasTrusteeshipService = redDotServiceMap.keySet().stream()
                .anyMatch(RedDotDetails.RedDotModule::isNeedTrusteeship);
        if (hasTrusteeshipService) {
            Map<RedDotDetails.RedDotModule, Map<Integer, Integer>> dotDaoAll = redDotDao.getAll(playerId);
            for (Map.Entry<RedDotDetails.RedDotModule, Map<Integer, Integer>> entry : dotDaoAll.entrySet()) {
                RedDotDetails.RedDotModule module = entry.getKey();
                if (!module.isNeedTrusteeship() || !isServiceRegistered(module, 0)) {
                    continue;
                }
                //该模块下的所有子模块
                for (Map.Entry<Integer, Integer> submoduleInfo : entry.getValue().entrySet()) {
                    if (isServiceRegistered(module, submoduleInfo.getKey())) {
                        allRedDots.add(buildRedDotDetails(module, submoduleInfo.getKey(), submoduleInfo.getValue()));
                    }
                }
            }
        }
        //获取不需要存储的红点信息
        if (CollectionUtil.isNotEmpty(redDotServiceMap)) {
            for (Map.Entry<RedDotDetails.RedDotModule, Map<Integer, IRedDotService>> entry : redDotServiceMap.entrySet()) {
                if (entry.getKey().isNeedTrusteeship()) {
                    continue;
                }
                try {
                    for (IRedDotService redDotService : new HashSet<>(entry.getValue().values())) {
                        allRedDots.addAll(redDotService.initialize(playerId, 0));
                    }
                } catch (Exception e) {
                    log.error("获取红点信息异常:{}", playerId, e);
                }
            }
        }
        log.debug("玩家 {} 总共加载了 {} 个红点", playerId, allRedDots.size());
        return allRedDots;
    }

    /**
     * 构建红点详情
     *
     * @param module    模块
     * @param submodule 子模块
     * @param count     数量
     * @return 红点详情
     */
    public RedDotDetails buildRedDotDetails(RedDotDetails.RedDotModule module, int submodule, int count) {
        return buildRedDotDetails(module, submodule, count, module.getRedDotType());
    }

    /** 子模块独立选择显示类型；数量红点保留真实非负数量。 */
    public RedDotDetails buildRedDotDetails(RedDotDetails.RedDotModule module, int submodule, long count,
                                          RedDotDetails.RedDotType type) {
        RedDotDetails details = new RedDotDetails();
        details.setCount(type == RedDotDetails.RedDotType.COUNT ? Math.max(0, count) : (count > 0 ? 1 : 0));
        details.setRedDotType(type);
        details.setRedDotModule(module);
        details.setRedDotSubmodule(submodule);
        return details;
    }

    /**
     * 自增红点数量并更新
     *
     */
    private void incrementRedDotData(RedDotDetails.RedDotModule module, int submodule, long playerId, int count, boolean notify) {
        int afterCount = redDotDao.incrementValue(playerId, module.getType(), submodule, count);
        if (notify) {
            updateRedDot(module, submodule, playerId, afterCount);
        }
    }

    /**
     * 设置红点数量并更新
     *
     */
    public void setRedDotData(RedDotDetails.RedDotModule module, int submodule, long playerId, int count, boolean notify) {
        redDotDao.setValue(playerId, module.getType(), submodule, count);
        if (notify) {
            updateRedDot(module, submodule, playerId, count);
        }
    }

    /**
     * 更新活动红点
     *
     * @param playerId     玩家id
     * @param activityType 活动类型
     * @param hasRedDot    是否有红点
     */
    public void updateActivityRedDot(long playerId, int activityType, boolean hasRedDot) {
        List<RedDotDetails> list = new ArrayList<>();
        list.add(buildRedDotDetails(RedDotDetails.RedDotModule.ACTIVITY, activityType, hasRedDot ? 1 : 0));
        updateRedDot(list, playerId);
    }


    /**
     * 清除红点
     *
     */
    public void clearRedDot(RedDotDetails.RedDotModule module, int submodule, long playerId, boolean notify) {
        redDotDao.delete(playerId, module.getType(), submodule);
        if (notify) {
            updateRedDot(module, submodule, playerId, 0);
        }
    }

    /**
     * 设置红点并更新
     *
     */
    public void incrementRedDotDataAndUpdate(RedDotDetails.RedDotModule module, int submodule, long playerId, int count) {
        incrementRedDotData(module, submodule, playerId, count, true);
    }

    /**
     * 设置红点并更新
     *
     */
    public void incrementRedDotDataAndUpdate(RedDotDetails.RedDotModule module, long playerId, int count) {
        incrementRedDotData(module, 0, playerId, count, true);
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
        if (!isServiceRegistered(module, submodule)) {
            return Collections.emptyList();
        }
        List<RedDotDetails> list = new ArrayList<>();
        //托管给红点系统的
        if (module.isNeedTrusteeship()) {
            Map<RedDotDetails.RedDotModule, Map<Integer, Integer>> serviceMap = redDotDao.getAll(playerId);
            if (CollectionUtil.isEmpty(serviceMap)) {
                log.warn("玩家没有红点 {} ，玩家ID: {}", module, playerId);
                return Collections.emptyList();
            }
            Map<Integer, Integer> map = serviceMap.get(module);
            if (CollectionUtil.isEmpty(map)) {
                log.warn("未找到红点模块 {} ，玩家ID: {}", module, playerId);
                return Collections.emptyList();
            }
            if (submodule == 0) {
                for (Map.Entry<Integer, Integer> entry : map.entrySet()) {
                    if (isServiceRegistered(module, entry.getKey())) {
                        list.add(buildRedDotDetails(module, entry.getKey(), entry.getValue()));
                    }
                }
            } else {
                list.add(buildRedDotDetails(module, submodule, map.getOrDefault(submodule, 0)));
            }
            return list;
        }
        //自己处理的
        Map<Integer, IRedDotService> serviceMap = redDotServiceMap.get(module);
        if (submodule == 0) {
            for (IRedDotService redDotService : new HashSet<>(serviceMap.values())) {
                list.addAll(redDotService.initialize(playerId, submodule));
            }
        } else {
            IRedDotService redDotService = serviceMap.get(submodule);
            if (redDotService != null) {
                list.addAll(redDotService.initialize(playerId, submodule));
            }
        }
        return list;
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
        if (playerId > 0) {
            RedDotRecipient recipient = resolveRecipient(playerId);
            if (recipient != null) {
                sendRedDots(list, recipient);
            }
            return;
        }

        List<RedDotDetails> supportedRedDots = new ArrayList<>(list.size());
        for (RedDotDetails details : list) {
            if (isServiceRegistered(details.getRedDotModule(), details.getRedDotSubmodule())) {
                supportedRedDots.add(details);
            }
        }
        if (supportedRedDots.isEmpty()) {
            return;
        }
        NotifyRedDot notifyRedDot = new NotifyRedDot();
        notifyRedDot.setRedDotList(supportedRedDots);
        clusterSystem.broadcastToOnlinePlayer(notifyRedDot);
    }


    /**
     * 通知客户端刷新红点数据
     *
     * @param submodule 子模块
     * @param playerId  玩家id 如果参数<=0则广播给所有在线玩家
     */
    public void updateRedDot(RedDotDetails.RedDotModule module, int submodule, long playerId, long redCount) {
        if (module == null) {
            return;
        }
        updateRedDot(List.of(buildRedDotDetails(module, submodule, redCount, module.getRedDotType())), playerId);
    }

    /**
     * 通知客户端刷新红点数据
     *
     * @param submodule 子模块
     * @param playerId  玩家id 如果参数<=0则广播给所有在线玩家
     * @param module    红点模块
     */
    public void updateRedDot(RedDotDetails.RedDotModule module, int submodule, long playerId) {
        if (module == null) {
            return;
        }
        List<RedDotDetails> load = new ArrayList<>(1);
        load.add(buildRedDotDetails(module, submodule, 1));
        updateRedDot(load, playerId);
    }

    /**
     * 通知客户端刷新红点数据
     *
     * @param submodule 子模块
     * @param playerId  玩家id 如果参数<=0则广播给所有在线玩家
     * @param module    红点模块
     */
    public void updateRedDotByInitialize(RedDotDetails.RedDotModule module, int submodule, long playerId) {
        updateRedDotByInitialize(module, List.of(submodule), playerId);
    }

    /**
     * 通知客户端刷新红点数据
     *
     * @param submoduleList 子模块列表
     * @param playerId      玩家id 如果参数<=0则广播给所有在线玩家
     * @param module        红点模块
     */
    public void updateRedDotByInitialize(RedDotDetails.RedDotModule module, List<Integer> submoduleList, long playerId) {
        if (module == null || module.isNeedTrusteeship() || CollectionUtil.isEmpty(submoduleList)) {
            return;
        }
        Map<Integer, IRedDotService> serviceMap = redDotServiceMap.get(module);
        if (serviceMap == null) {
            return;
        }
        RedDotRecipient recipient = playerId > 0 ? resolveRecipient(playerId) : null;
        if (playerId > 0 && recipient == null) {
            return;
        }
        List<RedDotDetails> updateList = new ArrayList<>();
        for (Integer submodule : submoduleList) {
            IRedDotService iRedDotService = serviceMap.get(submodule);
            if (iRedDotService == null || (recipient != null
                    && !iRedDotService.getSupportedNodeTypes().contains(recipient.nodeType()))) {
                continue;
            }
            updateList.addAll(iRedDotService.initialize(playerId, submodule));
        }
        if (recipient == null) {
            updateRedDot(updateList, playerId);
        } else {
            sendRedDots(updateList, recipient);
        }
    }

    /**
     * 按每个在线玩家的业务数据重新计算红点并分别推送。
     * 适用于公告等包含玩家个人已读状态、无法使用统一数量广播的模块。
     */
    public void updateRedDotByInitializeForOnlinePlayers(RedDotDetails.RedDotModule module, int submodule) {
        Map<Long, PlayerSessionInfo> onlinePlayers = playerSessionService.getAll();
        if (CollectionUtil.isEmpty(onlinePlayers)) {
            return;
        }
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            onlinePlayers.keySet().forEach(playerId -> executor.submit(() -> {
                try {
                    updateRedDotByInitialize(module, submodule, playerId);
                } catch (Exception e) {
                    log.error("按玩家刷新红点失败 module={},submodule={},playerId={}", module, submodule, playerId, e);
                }
            }));
        }
    }


    /**
     * 通知客户端刷新出红点数据
     *
     * @param playerId 玩家id 如果参数<=0则广播给所有在线玩家
     */
    public void updateRedDot(RedDotDetails.RedDotModule module, long playerId) {
        updateRedDot(module, 0, playerId);
    }


    /** 只允许业务服务确认可阅读状态，客户端不能清除可领取/可升级红点。 */
    public void markRead(PlayerController player, RedDotDetails.RedDotModule module, int submodule,
                         List<Integer> entityIds) {
        if (module == null || module.isNeedTrusteeship() || submodule <= 0
                || (entityIds != null && entityIds.size() > 200)) return;
        Map<Integer, IRedDotService> services = redDotServiceMap.get(module);
        IRedDotService service = services == null ? null : services.get(submodule);
        if (service != null && service.markRead(player.playerId(), submodule,
                entityIds == null ? List.of() : entityIds)) {
            notifyReddot(player, module, submodule);
        }
    }

    public void notifyReddot(PlayerController playerController, RedDotDetails.RedDotModule module, int submodule) {
        List<RedDotDetails> result = new ArrayList<>();
        if (module != null) {
            List<RedDotDetails> redDots = load(module, submodule, playerController.playerId());
            result.addAll(redDots);
        } else {
            List<RedDotDetails> redDotDetails = loadAll(playerController.playerId());
            result.addAll(redDotDetails);
        }
        NotifyRedDot notifyRedDot = new NotifyRedDot();
        notifyRedDot.setRedDotList(result);
        //回复红点数据
        playerController.send(notifyRedDot);
//        log.debug("推送玩家红点数据 module = {}，notify = {}", module, JSON.toJSONString(notifyRedDot));
    }
}

