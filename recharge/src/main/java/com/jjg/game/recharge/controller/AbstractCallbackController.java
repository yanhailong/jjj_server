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
     * @return
     */
    protected Order checkOrder(Order order) {
        if (order.getOrderStatus() == OrderStatus.SUCCESS) {
            log.debug("该订单重复回调 orderId = {}", order.getId());
            return null;
        }

        //修改订单状态
        Order successOrder = orderService.orderSuccess(order.getId(), order.getChannelOrderId());
        if (successOrder == null) {
            log.warn("修改订单状态失败 orderId = {}", order.getId());
            //TODO 记录下来，检查该订单，这里不能再次修改订单状态，因为可能是多线程问题没有修改成功
            return null;
        }
        return successOrder;
    }

    /**
     * 回调获取订单后处理逻辑
     *
     * @param order
     * @return
     */
    protected void payCallback(Order order, String money, String regionCode, String channelProductId) {
        try {
            Player player = playerService.get(order.getPlayerId());
            //获取玩家session信息
            PlayerSessionInfo info = playerSessionService.getInfo(order.getPlayerId());
            coreLogger.order(player, order, money, channelProductId, regionCode, order.getProductId());
            log.info("玩家充值成功 playerId = {},orderId = {}", order.getPlayerId(), order.getId());
            //将充值成功消息通知玩家所在节点
            notifyPlayerCurrentNode(info, order, money, regionCode, channelProductId);
        } catch (Exception e) {
            log.error("", e);
        }
    }


    /**
     * 将充值成功消息通知玩家所在节点
     *
     * @param info
     * @param order
     * @param money
     * @param regionCode
     * @throws Exception
     */
    protected void notifyPlayerCurrentNode(PlayerSessionInfo info, Order order, String money, String regionCode, String channelProductId) throws Exception {
        NotifyRechargeServer notify = new NotifyRechargeServer();
        notify.playerId = order.getPlayerId();
        notify.orderId = order.getId();
        notify.regionCode = regionCode;
        notify.channelProductId = channelProductId;
        notify.money = money;
        ClusterClient clusterClient;
        if (info != null && !StringUtils.isEmpty(info.getNodeName())) {
            //可能会出现玩家已经不在当前节点需要自行处理
            clusterClient = clusterSystem.getClusterByPath(info.getCurrentNode());
        } else {
            log.info("因玩家不在线，已将充值成功添加到离线充值 playerId = {},orderId = {}", order.getPlayerId(), order.getId());
            //离线玩家登陆时处理,离线充值
            offlineRechargeDao.addRecharge(order.getPlayerId(), ObjectMapperUtil.getDefualtConfigObjectMapper().writeValueAsString(notify));
            return;
        }
        if (clusterClient == null) {
            return;
        }
        PFMessage pfMessage = MessageUtil.getPFMessage(notify);
        ClusterMessage msg = new ClusterMessage(pfMessage);
        clusterClient.write(msg);
        log.info("已将充值成功消息通知玩家所在节点 playerId = {},orderId = {},toNodePath = {}", order.getPlayerId(), order.getId(), clusterClient.nodeConfig.getName());
    }

    /**
     * 失败订单处理
     *
     * @param orderId
     * @param desc
     */
    protected void failOrder(String orderId, String desc) {
        Order order = orderService.orderFail(orderId);
        if (order == null) {
            return;
        }

        Player player = playerService.get(order.getPlayerId());
        coreLogger.order(player, order, null, desc);
    }
}
