package com.jjg.game.core.recharge.service;

import cn.hutool.core.collection.CollectionUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.concurrent.BaseHandler;
import com.jjg.game.common.concurrent.PlayerExecutorGroupDisruptor;
import com.jjg.game.common.protostuff.PFSession;
import com.jjg.game.common.utils.ObjectMapperUtil;
import com.jjg.game.core.base.condition.handler.TodayDepositCondition;
import com.jjg.game.core.base.gameevent.GameEventManager;
import com.jjg.game.core.base.gameevent.PlayerEventCategory;
import com.jjg.game.core.dao.CountDao;
import com.jjg.game.core.dao.PlayerRechargeFlowDao;
import com.jjg.game.core.data.Order;
import com.jjg.game.core.data.OrderStatus;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.listener.OrderGenerate;
import com.jjg.game.core.logger.CoreLogger;
import com.jjg.game.core.pb.NotifyPayInfo;
import com.jjg.game.core.pb.NotifyRechargeServer;
import com.jjg.game.core.pb.RechargeType;
import com.jjg.game.core.recharge.dao.OfflineRechargeDao;
import com.jjg.game.core.service.CorePlayerService;
import com.jjg.game.core.service.OrderService;
import com.jjg.game.core.task.manager.TaskManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author lm
 * @date 2026/1/6 09:40
 */
@Service
public class RechargeService {
    private static final Logger log = LoggerFactory.getLogger(RechargeService.class);
    private final OfflineRechargeDao offlineRechargeDao;
    private final CorePlayerService playerService;
    private final OrderService orderService;
    private final GameEventManager gameEventManager;
    private final CountDao countDao;
    private final PlayerRechargeFlowDao playerRechargeFlowDao;
    private final TaskManager taskManager;
    private final ClusterSystem clusterSystem;
    private final TodayDepositCondition todayDepositCondition;
    private final CoreLogger coreLogger;
    private final Map<RechargeType, OrderGenerate> orderGenerateMap;

    public RechargeService(OfflineRechargeDao offlineRechargeDao,
                           CorePlayerService playerService,
                           OrderService orderService,
                           GameEventManager gameEventManager,
                           CountDao countDao,
                           PlayerRechargeFlowDao playerRechargeFlowDao,
                           TaskManager taskManager,
                           ClusterSystem clusterSystem,
                           TodayDepositCondition conditionManager, CoreLogger coreLogger,
                           List<OrderGenerate> orderGenerateList) {
        this.offlineRechargeDao = offlineRechargeDao;
        this.playerService = playerService;
        this.orderService = orderService;
        this.gameEventManager = gameEventManager;
        this.countDao = countDao;
        this.playerRechargeFlowDao = playerRechargeFlowDao;
        this.taskManager = taskManager;
        this.clusterSystem = clusterSystem;
        this.todayDepositCondition = conditionManager;
        this.coreLogger = coreLogger;
        this.orderGenerateMap = orderGenerateList.stream().collect(Collectors.toMap(OrderGenerate::getRechargeType, Function.identity()));
    }

    public void loadOfflineRecharge(long playerId) {
        List<String> offlineRechargeJson;
        try {
            offlineRechargeJson = offlineRechargeDao.pollAll(playerId);
        } catch (Exception e) {
            log.error("检查玩家离线充值数据异常 playerId = {}", playerId, e);
            return;
        }
        if (CollectionUtil.isEmpty(offlineRechargeJson)) {
            return;
        }
        ObjectMapper mapper = ObjectMapperUtil.getDefualtConfigObjectMapper();
        for (String json : offlineRechargeJson) {
            try {
                NotifyRechargeServer notifyRechargeServer = mapper.readValue(json, NotifyRechargeServer.class);
                if (notifyRechargeServer == null) {
                    continue;
                }
                log.info("开始处理离线充值 playerId = {},orderId = {}", playerId, notifyRechargeServer.orderId);
                notifyRecharge(notifyRechargeServer, false);
            } catch (Exception e) {
                log.error("处理离线充值记录异常 playerId = {},json = {}", playerId, json, e);
            }
        }
    }

    public void notifyRecharge(NotifyRechargeServer notify, boolean needCheck) {
        try {
            PFSession session = clusterSystem.getSession(notify.playerId);
            if (needCheck && session == null) {
                log.error("处理充值时玩家PFSession为null playerId={}", notify.playerId);
                addOfflineRecharge(notify, "处理充值时玩家PFSession为null");
                return;
            }
            if (session != null) {
                try {
                    boolean published = PlayerExecutorGroupDisruptor.getDefaultExecutor()
                            .tryPublish(session.getWorkId(), 0, new BaseHandler<String>() {
                                @Override
                                public void action() {
                                    dealRecharge(notify);
                                }
                            });
                    if (published) {
                        return;
                    }
                    log.error("充值事件分发到玩家线程失败，回滚到离线充值队列 playerId:{} orderId:{}", notify.playerId, notify.orderId);
                    addOfflineRecharge(notify, "充值事件分发失败");
                } catch (Exception e) {
                    log.error("充值事件分发异常 playerId:{} orderId:{}", notify.playerId, notify.orderId, e);
                    addOfflineRecharge(notify, "充值事件分发异常");
                }
                return;
            }
            log.info("充值时玩家session 为null playerId:{}", notify.playerId);
            dealRecharge(notify);
        } catch (Exception e) {
            log.error("通知充值异常 playerId:{} orderId:{}", notify.playerId, notify.orderId, e);
            addOfflineRecharge(notify, "通知充值异常");
        }
    }

    private void dealRecharge(NotifyRechargeServer notify) {
        Player player;
        Order order;
        try {
            player = playerService.get(notify.playerId);
            order = orderService.getOrder(notify.orderId);
            if (player == null || order == null) {
                log.error("处理充值时玩家或订单为空，停止重试以避免离线队列毒消息 playerId:{} orderId:{} playerNull:{} orderNull:{}",
                        notify.playerId, notify.orderId, player == null, order == null);
                return;
            }
            OrderStatus orderStatus = order.getOrderStatus();
            if (orderStatus == null) {
                log.error("处理充值时订单状态为空 playerId:{} orderId:{}", notify.playerId, notify.orderId);
                return;
            }
            if (orderStatus == OrderStatus.FAIL || orderStatus == OrderStatus.CANCEL) {
                log.warn("处理充值时订单已关闭，停止重试 playerId:{} orderId:{} orderStatus:{}", notify.playerId, notify.orderId, orderStatus);
                return;
            }
            if (orderStatus.isProcessingOrder()) {
                log.info("处理充值时订单已经在处理中 playerId:{} orderId:{}", notify.playerId, notify.orderId);
                return;
            }
            if (orderStatus != OrderStatus.CALLBACK) {
                log.warn("处理充值时订单状态不允许继续执行 playerId:{} orderId:{} orderStatus:{}", notify.playerId, notify.orderId, orderStatus);
                return;
            }
        } catch (Exception e) {
            log.error("处理充值读取玩家或订单异常 playerId = {},orderId = {}", notify.playerId, notify.orderId, e);
            addOfflineRecharge(notify, "处理充值读取玩家或订单异常");
            return;
        }
        Order newOlder;
        try {
            newOlder = orderService.orderProcessing(order.getId(), order.getChannelOrderId());
            if (newOlder == null) {
                log.error("修改订单状态为处理中失败 playerId = {},orderId = {}", notify.playerId, notify.orderId);
                logRechargeOrder(player, order, notify, "修改订单状态为处理中失败");
                return;
            }
        } catch (Exception e) {
            log.error("修改订单状态为处理中异常 playerId = {},orderId = {}", notify.playerId, notify.orderId, e);
            logRechargeOrder(player, order, notify, "修改订单状态为处理中异常");
            return;
        }
        BigDecimal orderPrice = newOlder.getPrice();
        long orderPlayerId = newOlder.getPlayerId();
        int allRechargeCount = 0;
        try {
            OrderGenerate orderGenerate = orderGenerateMap.get(newOlder.getRechargeType());
            if (orderGenerate == null) {
                log.error("处理订单逻辑中orderGenerate为null playerId = {},orderId = {}", notify.playerId, notify.orderId);
                logRechargeOrder(player, newOlder, notify, "处理订单逻辑中数据异常");
                return;
            }
            try {
                if (!orderGenerate.onReceivedRecharge(player, newOlder)) {
                    log.error("处理订单逻辑失败 playerId = {},orderId = {}", notify.playerId, notify.orderId);
                    logRechargeOrder(player, newOlder, notify, "处理订单逻辑失败");
                    return;
                }
            } catch (Exception e) {
                log.error("处理订单逻辑中出现异常 playerId = {},orderId = {}", notify.playerId, notify.orderId, e);
                logRechargeOrder(player, newOlder, notify, "处理订单逻辑中出现异常");
                return;
            }
            gameEventManager.syncTriggerEvent(new PlayerEventCategory.PlayerRechargeEvent(player, newOlder, notify.money, notify.regionCode, notify.channelProductId));
            Long countLong = countDao.getCountLong(CountDao.CountType.RECHARGE_COUNT.getParam(), String.valueOf(player.getId()));
            if (countLong != null) {
                allRechargeCount = countLong.intValue();
            }
            Order successOrder = orderService.orderSuccess(newOlder.getId(), newOlder.getChannelOrderId());
            if (successOrder == null) {
                log.error("修改订单状态为成功失败 playerId = {},orderId = {}", notify.playerId, notify.orderId);
                newOlder.setOrderStatus(OrderStatus.SUCCESS);
                notifyPayInfo(notify, newOlder, allRechargeCount, player);
                logRechargeOrder(player, newOlder, notify, "充值奖励已发放，订单状态回写失败");
                return;
            }
            newOlder = successOrder;
        } catch (Exception e) {
            log.error("dealRecharge执行异常 playerId = {},orderId = {}", notify.playerId, notify.orderId, e);
            logRechargeOrder(player, newOlder, notify, "dealRecharge执行异常");
            return;
        }
        logRechargeOrder(player, newOlder, notify, newOlder.getProductId());
        notifyPayInfo(notify, newOlder, allRechargeCount, player);

    }

    private void notifyPayInfo(NotifyRechargeServer notify, Order newOlder, int allRechargeCount, Player player) {
        try {
            NotifyPayInfo notifyPayInfo = new NotifyPayInfo();
            notifyPayInfo.orderId = newOlder.getId();
            notifyPayInfo.allRechargeCount = allRechargeCount;
            clusterSystem.sendToPlayer(notifyPayInfo, player.getId());
        } catch (Exception e) {
            log.error("充值处理完成给玩家发送信息时异常playerId = {},orderId = {}", notify.playerId, notify.orderId, e);
        }
    }

    private void addOfflineRecharge(NotifyRechargeServer notify, String reason) {
        try {
            String json = ObjectMapperUtil.getDefualtConfigObjectMapper().writeValueAsString(notify);
            offlineRechargeDao.addRecharge(notify.playerId, json);
            log.info("{}，已写入离线充值队列 playerId:{} orderId:{}", reason, notify.playerId, notify.orderId);
        } catch (Exception e) {
            log.error("{}，写入离线充值队列失败 playerId:{} orderId:{}", reason, notify.playerId, notify.orderId, e);
        }
    }

    private void logRechargeOrder(Player player, Order order, NotifyRechargeServer notify, String desc) {
        if (order == null) {
            return;
        }
        try {
            coreLogger.order(player, order, order.getMoney(), order.getChannelProductId(), order.getRegionCode(), desc);
        } catch (Exception e) {
            log.error("记录充值订单日志异常 playerId = {},orderId = {}", notify.playerId, notify.orderId, e);
        }
    }

}
