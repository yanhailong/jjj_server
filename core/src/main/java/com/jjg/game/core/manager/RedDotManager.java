package com.jjg.game.core.manager;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSONObject;
import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.protostuff.PFSession;
import com.jjg.game.core.base.reddot.IRedDotService;
import com.jjg.game.core.dao.RedDotDao;
import com.jjg.game.core.pb.reddot.NotifyRedDot;
import com.jjg.game.core.pb.reddot.RedDotDetails;
import com.jjg.game.core.service.PlayerSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
            Map<Integer, IRedDotService> serviceMap = redDotServiceMap.computeIfAbsent(module, key -> new ConcurrentHashMap<>());
            serviceMap.put(service.getSubmodule(), service);
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
        //获取需要存储的红点信息
        Map<RedDotDetails.RedDotModule, Map<Integer, Integer>> dotDaoAll = redDotDao.getAll(playerId);
        List<RedDotDetails> allRedDots = new ArrayList<>();
        if (!dotDaoAll.isEmpty()) {
            for (Map.Entry<RedDotDetails.RedDotModule, Map<Integer, Integer>> entry : dotDaoAll.entrySet()) {
                //该模块下的所有子模块
                for (Map.Entry<Integer, Integer> submoduleInfo : entry.getValue().entrySet()) {
                    allRedDots.add(buildRedDotDetails(entry.getKey(), submoduleInfo.getKey(), submoduleInfo.getValue()));
                }
            }
        }
        //获取不需要存储的红点信息
        if (CollectionUtil.isNotEmpty(redDotServiceMap)) {
            for (Map<Integer, IRedDotService> iRedDotServiceMap : redDotServiceMap.values()) {
                try {
                    for (IRedDotService redDotService : iRedDotServiceMap.values()) {
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
     * @param module 模块
     * @param submodule 子模块
     * @param count 数量
     * @return 红点详情
     */
    private RedDotDetails buildRedDotDetails(RedDotDetails.RedDotModule module, int submodule, int count) {
        RedDotDetails details = new RedDotDetails();
        details.setCount(count);
        if (module.getRedDotType() == RedDotDetails.RedDotType.COMMON) {
            details.setCount(count >= 1 ? 1 : 0);
        }
        details.setRedDotType(module.getRedDotType());
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
     * 更新活动红点
     * @param playerId 玩家id
     * @param activityType 活动类型
     * @param hasRedDot 是否有红点
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
                    list.add(buildRedDotDetails(module, entry.getKey(), entry.getValue()));
                }
            } else {
                list.add(buildRedDotDetails(module, submodule, map.getOrDefault(submodule, 0)));
            }
            return list;
        }
        //自己处理的
        Map<Integer, IRedDotService> serviceMap = redDotServiceMap.get(module);
        if (CollectionUtil.isEmpty(serviceMap)) {
            return list;
        }
        if (submodule == 0) {
            for (IRedDotService redDotService : serviceMap.values()) {
                list.addAll(redDotService.initialize(playerId, 0));
            }
        } else {
            IRedDotService redDotService = serviceMap.get(submodule);
            if (redDotService != null) {
                list.addAll(redDotService.initialize(playerId, 0));
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
        NotifyRedDot notifyRedDot = new NotifyRedDot();
        notifyRedDot.setRedDotList(list);
        if (playerId > 0) {
            PFSession session = playerSessionService.getSession(playerId);
            if (session == null) {
                return;
            }
            session.send(notifyRedDot);
            log.debug("玩家刷新红点 playerId = {},details = {}", playerId, JSONObject.toJSONString(list));
        } else {
            clusterSystem.broadcastToOnlinePlayer(notifyRedDot);
        }
    }


    /**
     * 通知客户端刷新红点数据
     *
     * @param submodule     子模块
     * @param playerId      玩家id 如果参数<=0则广播给所有在线玩家
     */
    public void updateRedDot(RedDotDetails.RedDotModule module, int submodule, long playerId, int redCount) {
        if (module == null) {
            return;
        }
        List<RedDotDetails> load = load(module, submodule, playerId);
        if (CollectionUtil.isEmpty(load)) {
            return;
        }
        updateRedDot(List.of(buildRedDotDetails(module, submodule, redCount)), playerId);
    }

    /**
     * 通知客户端刷新红点数据
     *
     * @param submodule     子模块
     * @param playerId      玩家id 如果参数<=0则广播给所有在线玩家
     * @param module 红点模块
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
     * @param submodule     子模块
     * @param playerId      玩家id 如果参数<=0则广播给所有在线玩家
     * @param module 红点模块
     */
    public void updateRedDotByInitialize(RedDotDetails.RedDotModule module, int submodule, long playerId) {
        updateRedDotByInitialize(module, List.of(submodule), playerId);
    }

    /**
     * 通知客户端刷新红点数据
     *
     * @param submoduleList     子模块列表
     * @param playerId      玩家id 如果参数<=0则广播给所有在线玩家
     * @param module 红点模块
     */
    public void updateRedDotByInitialize(RedDotDetails.RedDotModule module, List<Integer> submoduleList, long playerId) {
        if (module == null || module.isNeedTrusteeship() || CollectionUtil.isEmpty(submoduleList)) {
            return;
        }
        Map<Integer, IRedDotService> serviceMap = redDotServiceMap.get(module);
        List<RedDotDetails> updateList = new ArrayList<>();
        for (Integer submodule : submoduleList) {
            IRedDotService iRedDotService = serviceMap.get(submodule);
            if (iRedDotService == null) {
                continue;
            }
            updateList.addAll(iRedDotService.initialize(playerId, submodule));
        }
        updateRedDot(updateList, playerId);
    }


    /**
     * 通知客户端刷新出红点数据
     *
     * @param playerId      玩家id 如果参数<=0则广播给所有在线玩家
     */
    public void updateRedDot(RedDotDetails.RedDotModule module, long playerId) {
        updateRedDot(module, 0, playerId);
    }
}

