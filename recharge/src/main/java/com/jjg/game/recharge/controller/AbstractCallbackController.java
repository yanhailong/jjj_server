package com.jjg.game.recharge.controller;

import com.jjg.game.common.cluster.ClusterClient;
import com.jjg.game.common.cluster.ClusterMessage;
import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.common.curator.NodeType;
import com.jjg.game.common.pb.ItemInfo;
import com.jjg.game.common.protostuff.MessageUtil;
import com.jjg.game.common.protostuff.PFMessage;
import com.jjg.game.common.protostuff.PFSession;
import com.jjg.game.core.base.player.IRecharge;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.constant.RechargeType;
import com.jjg.game.core.data.*;
import com.jjg.game.core.logger.CoreLogger;
import com.jjg.game.core.pb.NotifyPayCallBack;
import com.jjg.game.core.pb.NotifyRechargeServer;
import com.jjg.game.core.service.*;
import io.micrometer.common.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

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

    /**
     * 检查订单
     *
     * @param order
     * @return
     */
    protected CommonResult<ShopProduct> checkOrder(Order order) {
        CommonResult<ShopProduct> result = new CommonResult<>(Code.SUCCESS);
        if (order.getOrderStatus() == OrderStatus.SUCCESS) {
            log.debug("该订单重复回调 orderId = {}", order.getId());
            result.code = Code.REPEAT_OP;
            return result;
        }

        //修改订单状态
        Order successOrder = orderService.orderSuccess(order.getId());
        if (successOrder == null) {
            log.warn("未找到该订单 orderId = {},status = {}", order.getId(), OrderStatus.ORDER);
            //TODO 记录下来，检查该订单，这里不能再次修改订单状态，因为可能是多线程问题没有修改成功
            result.code = Code.PARAM_ERROR;
            return null;
        }

        //获取商品
        ShopProduct shopProduct = shopService.getShopProduct(order.getProductId());
        if (shopProduct == null) {
            //TODO 记录下来，检查该订单
            log.debug("未找到该商品 orderId = {},productId = {}", order.getId(), order.getProductId());
            result.code = Code.NOT_FOUND;
            return null;
        }
        result.data = shopProduct;
        return result;
    }

    /**
     * 回调获取订单后处理逻辑
     *
     * @param order
     * @return
     */
    protected void payCallback(Order order, ShopProduct shopProduct) {
        try {
            List<ItemInfo> itemInfoList = null;
            if (shopProduct.getRewardItems() != null && !shopProduct.getRewardItems().isEmpty()) {
                CommonResult<ItemOperationResult> addItemsResult = playerPackService.addItems(order.getPlayerId(), shopProduct.getRewardItems(), "recharge", order.getId());
                if (!addItemsResult.success()) {
                    log.warn("支付成功，但是添加道具失败 playerId = {},orderId = {},productId = {},code = {}", order.getPlayerId(), order.getId(), shopProduct.getId(), addItemsResult.code);
                }

                itemInfoList = new ArrayList<>();
                for (Map.Entry<Integer, Long> en : shopProduct.getRewardItems().entrySet()) {
                    ItemInfo itemInfo = new ItemInfo();
                    itemInfo.itemId = en.getKey();
                    itemInfo.count = en.getValue();
                    itemInfoList.add(itemInfo);
                }
            }

            Player player = playerService.get(order.getPlayerId());
            coreLogger.order(player, order);
            if(order.getRechargeType() == RechargeType.SHOP){
                coreLogger.shop(player, order);
            }
            log.info("玩家充值成功 playerId = {},orderId = {}", order.getPlayerId(), order.getId());

            //获取玩家session信息
            PlayerSessionInfo info = playerSessionService.getInfo(order.getPlayerId());
            //通知玩家充值成功
            notifyPlayerRechargeCallBack(info, order, itemInfoList);
            //将充值成功消息通知玩家所在节点
            notifyPlayerCurrentNode(info, order);
        } catch (Exception e) {
            log.error("", e);
        }
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
}
