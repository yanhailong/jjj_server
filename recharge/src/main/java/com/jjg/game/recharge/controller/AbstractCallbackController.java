package com.jjg.game.recharge.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jjg.game.common.cluster.ClusterClient;
import com.jjg.game.common.cluster.ClusterMessage;
import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.curator.NodeType;
import com.jjg.game.common.pb.ItemInfo;
import com.jjg.game.common.protostuff.MessageUtil;
import com.jjg.game.common.protostuff.PFMessage;
import com.jjg.game.common.protostuff.PFSession;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.pb.RechargeType;
import com.jjg.game.core.data.*;
import com.jjg.game.core.logger.CoreLogger;
import com.jjg.game.core.pb.NotifyPayCallBack;
import com.jjg.game.core.pb.NotifyRechargeServer;
import com.jjg.game.core.service.*;
import io.micrometer.common.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author 11
 * @date 2025/9/22 19:37
 */
public abstract class AbstractCallbackController {
    protected Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    protected OrderService orderService;
    @Autowired
    protected ShopService shopService;
    @Autowired
    protected PlayerPackService playerPackService;
    @Autowired
    protected CorePlayerService playerService;
    @Autowired
    protected PlayerSessionService playerSessionService;
    @Autowired
    protected ClusterSystem clusterSystem;
    @Autowired
    protected CoreLogger coreLogger;
    @Autowired
    protected ThirdServiceInfo thirdServiceInfo;

    protected final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 检查订单
     *
     * @param order
     * @return
     */
    protected boolean checkOrder(Order order) {
        if (order.getOrderStatus() == OrderStatus.SUCCESS) {
            log.debug("该订单重复回调 orderId = {}", order.getId());
            return false;
        }

        //修改订单状态
        Order successOrder = orderService.orderSuccess(order.getId(), order.getChannelOrderId());
        if (successOrder == null) {
            log.warn("未找到该订单 orderId = {},status = {}", order.getId(), OrderStatus.ORDER);
            //TODO 记录下来，检查该订单，这里不能再次修改订单状态，因为可能是多线程问题没有修改成功
            return false;
        }

        //获取商品
        ShopProduct shopProduct = shopService.getShopProduct(Long.parseLong(order.getProductId()));
        if (shopProduct == null) {
            //TODO 记录下来，检查该订单
            log.debug("未找到该商品 orderId = {},productId = {}", order.getId(), order.getProductId());
            return false;
        }
        return true;
    }

    /**
     * 回调获取订单后处理逻辑
     *
     * @param order
     * @return
     */
    protected void payCallback(Order order, String money, String regionCode) {
        try {
            Player player = playerService.get(order.getPlayerId());
            //获取玩家session信息
            PlayerSessionInfo info = playerSessionService.getInfo(order.getPlayerId());
            //处理商城订单
            if (order.getRechargeType() == RechargeType.SHOP) {
                handleShopOrder(player, info, order, money, regionCode);
            }

            coreLogger.order(player, order, money, regionCode);
            log.info("玩家充值成功 playerId = {},orderId = {}", order.getPlayerId(), order.getId());
            //将充值成功消息通知玩家所在节点
            notifyPlayerCurrentNode(info, order);
        } catch (Exception e) {
            log.error("", e);
        }
    }

    /**
     * 处理商城订单
     *
     * @param info
     * @param order
     */
    private void handleShopOrder(Player player, PlayerSessionInfo info, Order order, String money, String regionCode) {
        ShopProduct shopProduct = shopService.getShopProduct(Long.parseLong(order.getProductId()));
        if (shopProduct == null) {
            log.debug("获取商品失败 orderId = {},productId = {}", order.getId(), order.getProductId());
            return;
        }

        List<ItemInfo> itemInfoList = null;
        if (shopProduct.getRewardItems() != null && !shopProduct.getRewardItems().isEmpty()) {
            CommonResult<ItemOperationResult> addItemsResult = playerPackService.addItems(order.getPlayerId(), shopProduct.getRewardItems(), AddType.RECHARGE, order.getId());
            if (!addItemsResult.success()) {
                log.warn("支付成功，但是添加道具失败 playerId = {},orderId = {},productId = {},code = {}", order.getPlayerId(), order.getId(), shopProduct.getId(), addItemsResult.code);
            }else{
                itemInfoList = new ArrayList<>();
                for (Map.Entry<Integer, Long> en : shopProduct.getRewardItems().entrySet()) {
                    ItemInfo itemInfo = new ItemInfo();
                    itemInfo.itemId = en.getKey();
                    itemInfo.count = en.getValue();
                    itemInfoList.add(itemInfo);
                }
                log.debug("商城充值后添加道具成功 playerId = {},orderId = {}", order.getPlayerId(), order.getId());
            }
        }
        coreLogger.shop(player, order, shopProduct, money, regionCode);
        //通知玩家充值成功
        notifyPlayerRechargeCallBack(info, order, itemInfoList);
    }

    /**
     * 通知玩家充值成功
     *
     * @param info
     * @param order
     * @param itemInfoList
     */
    protected void notifyPlayerRechargeCallBack(PlayerSessionInfo info, Order order, List<ItemInfo> itemInfoList) {
        if (info == null) {
            return;
        }
        PFSession session = playerSessionService.getSession(info);
        if (session == null) {
            return;
        }

        NotifyPayCallBack notify = new NotifyPayCallBack();
        notify.orderId = order.getId();
        notify.items = itemInfoList;
        session.send(notify);
    }

    /**
     * 将充值成功消息通知玩家所在节点
     *
     * @param info
     * @param order
     * @throws Exception
     */
    protected void notifyPlayerCurrentNode(PlayerSessionInfo info, Order order) throws Exception {
        ClusterClient clusterClient;
        boolean online = false;
        if (info != null && !StringUtils.isEmpty(info.getNodeName())) {
            clusterClient = clusterSystem.getClusterByPath(info.getCurrentNode());
            online = true;
        } else {
            //如果玩家不在线，则随机找一个大厅节点
            clusterClient = clusterSystem.randClientByType(NodeType.HALL);
        }

        if (clusterClient == null) {
            return;
        }

        NotifyRechargeServer notify = new NotifyRechargeServer();
        notify.playerId = order.getPlayerId();
        notify.orderId = order.getId();

        PFMessage pfMessage = MessageUtil.getPFMessage(notify);
        ClusterMessage msg = new ClusterMessage(pfMessage);
        clusterClient.write(msg);
        if (online) {
            log.info("已将充值成功消息通知玩家所在节点 playerId = {},orderId = {},toNodePath = {}", order.getPlayerId(), order.getId(), clusterClient.nodeConfig.getName());
        } else {
            log.info("因玩家不在线，已将充值成功消息随机通知大厅节点 playerId = {},orderId = {},toNodePath = {}", order.getPlayerId(), order.getId(), clusterClient.nodeConfig.getName());
        }
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
        coreLogger.order(player, order, desc);
    }

    //获取商品价格
    protected BigDecimal getProductPrice(RechargeType rechargeType, String productId) {
        return BigDecimal.ONE;
    }
}
