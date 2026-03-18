package com.jjg.game.recharge.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jjg.game.common.cluster.ClusterClient;
import com.jjg.game.common.cluster.ClusterMessage;
import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.protostuff.MessageUtil;
import com.jjg.game.common.protostuff.PFMessage;
import com.jjg.game.common.utils.ObjectMapperUtil;
import com.jjg.game.core.data.Order;
import com.jjg.game.core.data.OrderStatus;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerSessionInfo;
import com.jjg.game.core.logger.CoreLogger;
import com.jjg.game.core.pb.NotifyRechargeServer;
import com.jjg.game.core.recharge.dao.OfflineRechargeDao;
import com.jjg.game.core.service.CorePlayerService;
import com.jjg.game.core.service.OrderService;
import com.jjg.game.core.service.PlayerSessionService;
import io.micrometer.common.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

/**
 * @author 11
 * @date 2025/9/22 19:37
 */
public abstract class AbstractCallbackController {
    protected Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    protected OrderService orderService;
    @Autowired
    protected CorePlayerService playerService;
    @Autowired
    protected PlayerSessionService playerSessionService;
    @Autowired
    protected ClusterSystem clusterSystem;
    @Autowired
    protected CoreLogger coreLogger;
    @Autowired
    protected OfflineRechargeDao offlineRechargeDao;

    protected final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 检查订单
     *
     * @param order
     * @param money
     * @param currency
     * @param channelProductId
     * @return
     */
    protected Order checkOrder(Order order, String money, String currency, String channelProductId) {
        order.setMoney(money);
        order.setRegionCode(currency);
        order.setChannelProductId(channelProductId);

        OrderStatus status = order.getOrderStatus();
        if (status == null) {
            log.warn("订单状态为空 orderId = {}", order.getId());
            return null;
        }
        if (status == OrderStatus.ORDER) {
            Order callbackOrder = orderService.orderCallback(order);
            if (callbackOrder != null) {
                return callbackOrder;
            }
            Order latestOrder = orderService.getOrder(order.getId());
            if (latestOrder != null) {
                OrderStatus latestStatus = latestOrder.getOrderStatus();
                if (latestStatus == OrderStatus.CALLBACK || latestStatus.isProcessingOrder()) {
                    log.debug("订单状态已变化，按最新状态处理 orderId = {},status = {}", order.getId(), latestStatus);
                    return latestOrder;
                }
            }
            log.warn("修改订单状态失败 orderId = {}", order.getId());
            return null;
        }
        if (status == OrderStatus.CALLBACK || status.isProcessingOrder()) {
            log.debug("该订单重复回调 orderId = {},status = {}", order.getId(), status);
            return order;
        }
        log.warn("订单状态不允许继续处理回调 orderId = {},status = {}", order.getId(), status);
        return null;
    }

    /**
     * 回调获取订单后处理逻辑
     *
     * @param order
     * @return
     */
    protected void payCallback(Order order, String money, String regionCode, String channelProductId) {
        NotifyRechargeServer notify = buildRechargeNotify(order, money, regionCode, channelProductId);
        try {
            //获取玩家session信息
            PlayerSessionInfo info = playerSessionService.getInfo(order.getPlayerId());
            log.info("收到充值回调，开始通知处理 playerId = {},orderId = {}", order.getPlayerId(), order.getId());
            //将充值回调消息通知玩家所在节点
            notifyPlayerCurrentNode(info, order, notify);
        } catch (Exception e) {
            log.error("通知充值处理异常，回退到离线充值 playerId = {},orderId = {}", order.getPlayerId(), order.getId(), e);
            addOfflineRecharge(notify, "通知充值处理异常");
        }
    }


    /**
     * 将充值成功消息通知玩家所在节点
     *
     * @param info
     * @param order
     * @throws Exception
     */
    protected void notifyPlayerCurrentNode(PlayerSessionInfo info, Order order, NotifyRechargeServer notify) throws Exception {
        ClusterClient clusterClient;
        if (info != null && !StringUtils.isEmpty(info.getCurrentNode())) {
            //可能会出现玩家已经不在当前节点需要自行处理
            clusterClient = clusterSystem.getClusterByPath(info.getCurrentNode());
        } else {
            log.info("因玩家不在线，已将充值回调添加到离线充值 playerId = {},orderId = {}", order.getPlayerId(), order.getId());
            //离线玩家登陆时处理,离线充值
            addOfflineRecharge(notify, "玩家不在线");
            return;
        }
        if (clusterClient == null) {
            log.info("因未找到玩家所在节点信息，已将充值回调添加到离线充值 playerId = {},orderId = {}", order.getPlayerId(), order.getId());
            //离线玩家登陆时处理,离线充值
            addOfflineRecharge(notify, "未找到玩家所在节点");
            return;
        }
        PFMessage pfMessage = MessageUtil.getPFMessage(notify);
        ClusterMessage msg = new ClusterMessage(pfMessage);
        clusterClient.write(msg);
        log.info("已将充值回调消息通知玩家所在节点 playerId = {},orderId = {},toNodePath = {}", order.getPlayerId(), order.getId(), clusterClient.nodeConfig.getName());
    }

    protected NotifyRechargeServer buildRechargeNotify(Order order, String money, String regionCode, String channelProductId) {
        NotifyRechargeServer notify = new NotifyRechargeServer();
        notify.playerId = order.getPlayerId();
        notify.orderId = order.getId();
        notify.regionCode = regionCode;
        notify.channelProductId = channelProductId;
        notify.money = money;
        return notify;
    }

    protected void addOfflineRecharge(NotifyRechargeServer notify, String reason) {
        try {
            offlineRechargeDao.addRecharge(notify.playerId, ObjectMapperUtil.getDefualtConfigObjectMapper().writeValueAsString(notify));
            log.info("{}，已将充值回调添加到离线充值 playerId = {},orderId = {}", reason, notify.playerId, notify.orderId);
        } catch (Exception e) {
            log.error("{}，写入离线充值失败 playerId = {},orderId = {}", reason, notify.playerId, notify.orderId, e);
        }
    }

    /**
     * 失败订单处理
     *
     * @param orderId
     * @param desc
     */
    protected ResponseEntity<String> failOrder(String orderId, String desc) {
        Order order = orderService.orderFail(orderId);
        if (order == null) {
            log.warn("处理失败订单时，未找到该订单 orderId = {},desc = {}", orderId, desc);
            return ResponseEntity.ok("get order fail1,orderId=" + orderId);
        }
        return failOrder(order, desc);
    }

    protected ResponseEntity<String> failOrder(Order order, String desc) {
        Player player = playerService.get(order.getPlayerId());
        coreLogger.order(player, order, "", desc);
        return ResponseEntity.ok("handle fail order, id = " + order.getId());
    }
}
