package com.jjg.game.core.handler;

import com.alibaba.fastjson.JSON;
import com.jjg.game.common.baselogic.function.SystemInterfaceHolder;
import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.config.NodeConfig;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.curator.NodeManager;
import com.jjg.game.common.pb.NotifyKickout;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.core.base.gameevent.GameEventManager;
import com.jjg.game.core.base.gameevent.PlayerEventCategory;
import com.jjg.game.core.base.player.IRecharge;
import com.jjg.game.core.config.ConfigManager;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.BackendGMCmd;
import com.jjg.game.core.constant.GameConstant;
import com.jjg.game.core.constant.TaskConstant;
import com.jjg.game.core.data.*;
import com.jjg.game.core.manager.AmazonBucketManager;
import com.jjg.game.core.manager.CoreMarqueeManager;
import com.jjg.game.core.pb.NotifyAllNodesMarqueeServer;
import com.jjg.game.core.pb.NotifyAllNodesStopMarqueeServer;
import com.jjg.game.core.pb.NotifyConfigUpdate;
import com.jjg.game.core.pb.NotifyRechargeServer;
import com.jjg.game.core.pb.gm.*;
import com.jjg.game.core.service.CorePlayerService;
import com.jjg.game.core.service.LoginConfigService;
import com.jjg.game.core.service.OrderService;
import com.jjg.game.core.service.ShopService;
import com.jjg.game.core.task.manager.TaskManager;
import com.jjg.game.core.task.param.DefaultTaskConditionParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.function.Supplier;

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
    @Autowired
    private NodeConfig nodeConfig;
    @Autowired
    private NodeManager nodeManager;
    @Autowired
    private ConfigManager configManager;
    @Autowired
    private TaskManager taskManager;
    @Autowired
    private AmazonBucketManager amazonBucketManager;
    @Autowired
    private LoginConfigService loginConfigService;

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
        ShopProduct shopProduct = shopService.getShopProduct(Long.parseLong(order.getProductId()));

        //接口通知
        SystemInterfaceHolder.callGameSysAction(IRecharge.class, (f) -> f.rechargeSuccess(player, order, shopProduct));
        //充值事件
        gameEventManager.triggerEvent(new PlayerEventCategory.PlayerRechargeEvent(player, order));
        //任务条件参数
        Supplier<DefaultTaskConditionParam> paramSupplier = () -> {
            DefaultTaskConditionParam param = new DefaultTaskConditionParam();
            param.setAddValue(order.getPrice().longValue());
            return param;
        };
        //单笔充值任务
        taskManager.trigger(order.getPlayerId(), TaskConstant.ConditionType.PLAYER_PAY, paramSupplier);
        //累计充值任务
        taskManager.trigger(order.getPlayerId(), TaskConstant.ConditionType.PLAYER_SUM_PAY, paramSupplier);
        log.info("充值成功，通知到玩家所在的当前节点 playerId = {},orderId = {}", player.getId(), order.getId());
    }

    /**
     * 配置更新
     */
    @Command(MessageConst.ToServer.CONFIG_UPDATE)
    public void notifyConfigUpdate(NotifyConfigUpdate notifyConfigUpdate) {
        String name = notifyConfigUpdate.getName();
        configManager.reLoadAllConfigsFromRedis(name);
    }

    /**
     * 节点信息变化
     */
    @Command(MessageConst.ToServer.NOTIFY_GAME_NODE_CHANGE)
    public void notifyGameNodeChange(NotifyGameNodeChange notify) {
        log.debug("收到需要改变节点信息的消息 notify = {}", JSON.toJSONString(notify));
        try {
            nodeConfig.setWeight(notify.weight);

            if (notify.ips != null && !notify.ips.isEmpty()) {
                nodeConfig.setWhiteIpList(notify.ips.toArray(new String[0]));
            } else {
                nodeConfig.setWhiteIpList(null);
            }

            if (notify.ids != null && !notify.ids.isEmpty()) {
                nodeConfig.setWhiteIdList(notify.ids.toArray(new String[0]));
            } else {
                nodeConfig.setWhiteIdList(null);
            }
            nodeManager.update();
        } catch (Exception e) {
            log.error("", e);
        }
    }

    /**
     * 配置表变化
     */
    @Command(MessageConst.ToServer.NOTIFY_EXCEL_CHANGE)
    public void notifyExcelChange(NotifyExcelChange notify) {
        log.debug("收到需要更新配置表的消息 notify = {}", JSON.toJSONString(notify));
        try {
            amazonBucketManager.dowmloadFiles(notify.nameList);
        } catch (Exception e) {
            log.error("", e);
        }
    }

    /**
     * 加载登录配置
     */
    @Command(MessageConst.ToServer.NOTIFY_LOAD_LOGIN_CONFIG)
    public void notifyLoadLoginConfig(NotifyLoadLoginConfig notify) {
        log.debug("收到需要重新加载登录配置的消息 notify = {}", JSON.toJSONString(notify));
        try {
            loginConfigService.load();
        } catch (Exception e) {
            log.error("", e);
        }
    }

    /**
     * 通知修改玩家金币修改
     */
    @Command(MessageConst.ToServer.NOTIFY_GOLD_OPERATE)
    public void notifyGoldOperate(NotifyGoldOperator notify) {
        log.debug("收到需要修改玩家金币的消息 notify = {}", JSON.toJSONString(notify));
        try {
            CommonResult<Player> result;

            AddType addType = AddType.valueOf(notify.addType);

            if(notify.type == 1){  //增加
                if(notify.currency_id == GameConstant.Item.TYPE_GOLD){
                    result = playerService.addGoldAndDiamond(notify.playerId, notify.quantity,0,addType,true,notify.remark);
                }else {
                    result = playerService.addGoldAndDiamond(notify.playerId, 0,notify.quantity,addType,true,notify.remark);
                }
            }else {  //减少
                if(notify.currency_id == GameConstant.Item.TYPE_GOLD){
                    result = playerService.deductGoldAndDiamond(notify.playerId, notify.quantity,0,addType,true,notify.remark);
                }else {
                    result = playerService.deductGoldAndDiamond(notify.playerId, 0,notify.quantity,addType,true,notify.remark);
                }
            }

            if(result.success()){
                log.info("修改玩家账户成功 notify = {}", JSON.toJSONString(notify));
            }else {
                log.info("修改玩家账户失败 notify = {},code = {}", JSON.toJSONString(notify),result.code);
            }
        } catch (Exception e) {
            log.error("", e);
        }
    }

}
