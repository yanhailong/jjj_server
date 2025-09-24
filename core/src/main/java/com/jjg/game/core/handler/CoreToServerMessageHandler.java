package com.jjg.game.core.handler;

import com.alibaba.fastjson.JSON;
import com.jjg.game.common.baselogic.function.SystemInterfaceHolder;
import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.NotifyKickout;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.core.base.gameevent.GameEventManager;
import com.jjg.game.core.base.player.IRecharge;
import com.jjg.game.core.constant.BackendGMCmd;
import com.jjg.game.core.data.Marquee;
import com.jjg.game.core.data.Order;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.ShopProduct;
import com.jjg.game.core.base.gameevent.ActivityChangeEvent;
import com.jjg.game.core.base.gameevent.PlayerEventCategory;
import com.jjg.game.core.data.Order;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.ShopProduct;
import com.jjg.game.core.manager.CoreMarqueeManager;
import com.jjg.game.core.pb.NotifyAllNodesMarqueeServer;
import com.jjg.game.core.pb.NotifyAllNodesStopMarqueeServer;
import com.jjg.game.core.pb.NotifyRechargeServer;
import com.jjg.game.core.pb.activity.NotifyActivityServerChange;
import com.jjg.game.core.pb.gm.NotifyCarouselUpdate;
import com.jjg.game.core.pb.gm.NotifyShopProductChange;
import com.jjg.game.core.pb.gm.ReqAllKickout;
import com.jjg.game.core.service.CorePlayerService;
import com.jjg.game.core.service.OrderService;
import com.jjg.game.core.service.ShopService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author 11
 * @since 2025/8/6 13:56
 */
public class CoreToServerMessageHandler {
    protected Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private CoreMarqueeManager marqueeManager;
    @Autowired
    private ClusterSystem clusterSystem;
    @Autowired
    private ShopService shopService;
    @Autowired
    private CorePlayerService playerService;
    @Autowired
    private OrderService orderService;
    @Autowired
    private GameEventManager gameEventManager;

    /**
     * 其他节点推送的跑马灯信息
     */
    @Command(MessageConst.ToServer.NOTICE_MARQUEE_HALL_MASTER)
    public void notifyMarqueeHallMaster(NotifyAllNodesMarqueeServer notify) {
        try {
            log.info("收到其他节点推送的跑马灯信息 notify = {}", JSON.toJSONString(notify));
            Marquee marquee = new Marquee();
            marquee.setId(notify.marqueeInfo.id);

            if (notify.marqueeInfo.content != null) {
                marquee.setContent(notify.marqueeInfo.content.toData());
            }

            marquee.setShowTime(notify.marqueeInfo.showTime);
            marquee.setInterval(notify.marqueeInfo.interval);
            marquee.setType(notify.type);
            marquee.setStartTime(notify.marqueeInfo.startTime);
            marquee.setEndTime(notify.marqueeInfo.endTime);

            marqueeManager.addNewMarquee(marquee);
        } catch (Exception e) {
            log.error("", e);
        }
    }

    /**
     * 收到其他节点推送的停止跑马灯信息
     */
    @Command(MessageConst.ToServer.NOTICE_STOP_MARQUEE_HALL_MASTER)
    public void notifyStopMarqueeHallMaster(NotifyAllNodesStopMarqueeServer notify) {
        try {
            log.info("收到其他节点推送的停止跑马灯信息 id = {}", notify.id);
            marqueeManager.removeMarquee(notify.id);
        } catch (Exception e) {
            log.error("", e);
        }
    }

    @Command(MessageConst.ToServer.NOTICE_ALL_KICK_OUT)
    public void reqAllKickout(ReqAllKickout req) {
        log.info("收到其他节点推送的全服踢人的请求 langId = {}", req.langId);
        try {
            NotifyKickout notifyKickout = new NotifyKickout();
            notifyKickout.langId = req.langId;

            clusterSystem.broadcastToOnlinePlayer(notifyKickout);
        } catch (Exception e) {
            log.error("", e);
        }
    }

    /**
     * 收到轮播数据变化消息
     */
    @Command(MessageConst.ToServer.NOTICE_ALL_UPDATE_CAROUSEL)
    public void updateCarousel(NotifyCarouselUpdate updateMsg) {
        log.info("收到轮播数据变化消息!msg={}", updateMsg);
        try {
            clusterSystem.broadcastToOnlinePlayer(updateMsg);
        } catch (Exception e) {
            log.error("", e);
        }
    }

    @Command(MessageConst.ToServer.NOTICE_SHOP_PRODUCT_CHANGE)
    public void reqShopProductChange(NotifyShopProductChange req) {
        log.info("收到后台发送商城商品变更的命令");
        String result = BackendGMCmd.Result.SUCCESS;
        try {
            shopService.loadShopProducts();
        } catch (Exception e) {
            log.error("", e);
            result = BackendGMCmd.Result.FAIL;
        }
//        coreLogger.gmOrder(BackendGMCmd.CHANGE_GAME_STATUS + ":" + req.cmdParam, null, result);
    }

    /**
     * 玩家充值成功
     * 充值成功后，recharge节点会通知到玩家当前所在的节点
     * 如果玩家不在线或没找到当前所在节点，则会随机找一个大厅节点接收该消息
     */
    @Command(MessageConst.ToServer.NOTIFY_PLAYER_RECHARGE)
    public void notifyRecharge(NotifyRechargeServer notify) {
        Player player = playerService.get(notify.playerId);
        Order order = orderService.getOrder(notify.orderId);
        ShopProduct shopProduct = shopService.getShopProduct(order.getProductId());

        //接口通知
        SystemInterfaceHolder.callGameSysAction(IRecharge.class, (f) -> f.rechargeSuccess(player, order, shopProduct));
        //充值事件
        gameEventManager.triggerEvent(new PlayerEventCategory.PlayerRechargeEvent(player, order));

        log.info("充值成功，通知到玩家所在的当前节点 playerId = {},orderId = {}", player.getId(), order.getId());
    }


    /**
     * 活动信息变化
     */
    @Command(MessageConst.ToServer.NOTIFY_ACTIVITY_SERVER_CHANGE)
    public void notifyActivityServerChange(NotifyActivityServerChange notify) {
        //活动变化事件
        SystemInterfaceHolder.callGameSysAction(ActivityChangeEvent.class, (f) ->
                f.onActivityDataChange(notify));
    }


}
