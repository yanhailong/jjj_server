package com.jjg.game.core.handler;

import com.alibaba.fastjson.JSON;
import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.NotifyKickout;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.redis.RedisLock;
import com.jjg.game.core.constant.BackendGMCmd;
import com.jjg.game.core.dao.luckytreasure.LuckyTreasureConfigRedisDao;
import com.jjg.game.core.data.Marquee;
import com.jjg.game.core.manager.CoreMarqueeManager;
import com.jjg.game.core.pb.NotifyAllNodesMarqueeServer;
import com.jjg.game.core.pb.NotifyAllNodesStopMarqueeServer;
import com.jjg.game.core.pb.gm.NotifyCarouselUpdate;
import com.jjg.game.core.pb.gm.NotifyShopProductChange;
import com.jjg.game.core.pb.gm.ReqAllKickout;
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
    private LuckyTreasureConfigRedisDao luckyTreasureConfigRedisDao;
    @Autowired
    private RedisLock redisLock;

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

}
