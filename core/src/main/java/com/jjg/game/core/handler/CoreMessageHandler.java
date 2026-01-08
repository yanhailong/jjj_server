package com.jjg.game.core.handler;

import cn.hutool.core.util.EnumUtil;
import com.alibaba.fastjson.JSON;
import com.jjg.game.common.baselogic.function.SystemInterfaceHolder;
import com.jjg.game.common.config.NodeConfig;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.curator.NodeType;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.common.utils.CommonUtil;
import com.jjg.game.common.utils.HttpUtils;
import com.jjg.game.core.base.gameevent.GameEventManager;
import com.jjg.game.core.base.gameevent.PlayerEventCategory;
import com.jjg.game.core.constant.*;
import com.jjg.game.core.dao.CountDao;
import com.jjg.game.core.data.*;
import com.jjg.game.core.listener.ChooseWareListener;
import com.jjg.game.core.listener.GmListener;
import com.jjg.game.core.listener.OrderGenerate;
import com.jjg.game.core.manager.CoreMarqueeManager;
import com.jjg.game.core.manager.CoreSendMessageManager;
import com.jjg.game.core.manager.RedDotManager;
import com.jjg.game.core.manager.SubscriptionManager;
import com.jjg.game.core.pb.*;
import com.jjg.game.core.pb.reddot.ReqRedDot;
import com.jjg.game.core.service.CorePlayerService;
import com.jjg.game.core.service.OrderService;
import com.jjg.game.core.service.PlayerPackService;
import com.jjg.game.core.task.manager.TaskManager;
import com.jjg.game.core.task.param.DefaultTaskConditionParam;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * @author 11
 * @since 2025/6/11 16:09
 */
@Component
@MessageType(MessageConst.MessageTypeDef.CORE_MESSAGE_TYPE)
public class CoreMessageHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private NodeConfig nodeConfig;
    @Autowired
    private CorePlayerService playerService;
    @Autowired
    private CoreSendMessageManager coreSendMessageManager;
    @Autowired
    private PlayerPackService playerPackService;
    @Autowired
    private CoreMarqueeManager marqueeManager;
    @Autowired
    private RedDotManager redDotManager;
    @Autowired
    private GameEventManager gameEventManager;
    @Autowired
    private SubscriptionManager subscriptionManager;
    @Autowired
    private OrderService orderService;
    @Autowired
    private TaskManager taskManager;
    @Autowired
    private CountDao countDao;

    public Map<String, ChooseWareListener> chooseWareListenerMap;

    public void init() {
        chooseWareListenerMap = CommonUtil.getContext().getBeansOfType(ChooseWareListener.class);
    }

    /**
     *
     */
    @Command(MessageConst.CoreMessage.REQ_GM)
    public void reqGm(PlayerController playerController, ReqGm req) {
        ResGm res = new ResGm(Code.SUCCESS);
        try {
            if (!nodeConfig.isGm()) {
                res.code = Code.FORBID;
                playerController.send(res);
                log.debug("gm功能已经关闭 playerId = {}", playerController.playerId());
                return;
            }

            if (StringUtils.isEmpty(req.order)) {
                res.code = Code.PARAM_ERROR;
                playerController.send(res);
                log.debug("参数错误，使用gm失败 playerId = {},order = {}", playerController.playerId(), req.order);
                return;
            }


            String[] arr = req.order.trim().split("\\s+");
            if (arr.length < 1) {
                res.code = Code.PARAM_ERROR;
                playerController.send(res);
                log.debug("参数错误2，使用gm失败 playerId = {},order = {}", playerController.playerId(), req.order);
                return;
            }

            if (arr.length < 2) {
                res.code = Code.PARAM_ERROR;
                playerController.send(res);
                log.debug("参数错误2，gm命令长度必须大于2 playerId = {},order = {}", playerController.playerId(), req.order);
                return;
            }

            log.debug("收到gm命令 playerId = {},order = {}", playerController.playerId(), req.order);
            String cmd = arr[0];
            String params = arr[1];

            if ("init".equals(cmd)) {
                long goldNum = Long.parseLong(params);
                long diamondNum = Long.parseLong(arr[2]);
                int vip = Integer.parseInt(arr[3]);
                int level = Integer.parseInt(arr[4]);
                init(res, playerController, req.order, goldNum, diamondNum, vip, level);
                return;
            }

            if ("addGold".equalsIgnoreCase(cmd)) {
                addGold(res, playerController, req.order, params);
                return;
            }

            if ("addDiamond".equalsIgnoreCase(cmd)) {
                addDiamond(res, playerController, req.order, params);
                return;
            }

            if ("setVip".equalsIgnoreCase(cmd)) {
                setVip(res, playerController, req.order, params);
                return;
            }

            if ("addItem".equalsIgnoreCase(cmd)) {
                addItem(res, playerController, arr);
                return;
            }

            if ("playerWinMarquee".equalsIgnoreCase(cmd)) {
                marqueeManager.playerWinMarquee("shiyi", 17001, 100100026, 500000);
                return;
            }

            if ("bet".equals(cmd)) {
                log.debug("收到添加经验的gm命令 playerId = {},gmOrders = {}", playerController.playerId(), arr);
                long num = Long.parseLong(params);
                CommonResult<Player> result =
                        playerService.betDeductGold(playerController.playerId(), num, true, true, AddType.GM_TEST);
                res.code = result.code;
                playerController.send(res);
                return;
            }

            if ("recharge".equals(cmd)) {
                log.debug("收到充值的gm命令 playerId = {},gmOrders = {}", playerController.playerId(), arr);
                int type = Integer.parseInt(arr[1]);
                //1等级礼包 测试用
                RechargeType rechargeType = EnumUtil.getBy(RechargeType.class, e -> e.getType() == type);
                Order order = orderService.generateOrder(playerController.getPlayer(), PayType.GOOGLE, arr[2], BigDecimal.valueOf(100.99), rechargeType);
                gameEventManager.triggerEvent(new PlayerEventCategory.PlayerRechargeEvent(playerController.getPlayer(), order));
                //任务条件参数
                Supplier<DefaultTaskConditionParam> paramSupplier = () -> {
                    DefaultTaskConditionParam param = new DefaultTaskConditionParam();
                    param.setAddValue(1001);
                    return param;
                };

                long playerId = playerController.getPlayer().getId();
//                countDao.incrBy(CountDao.CountType.RECHARGE.getParam(), String.valueOf(playerId), order.getPrice());
                countDao.incrRechargeInfo(String.valueOf(playerId), order.getPrice());

                //单笔充值任务
                taskManager.trigger(playerId, TaskConstant.ConditionType.PLAYER_PAY, paramSupplier);
                //累计充值任务
                taskManager.trigger(playerId, TaskConstant.ConditionType.PLAYER_SUM_PAY, paramSupplier);
                return;
            }

            int notFound = 0;
            Map<String, GmListener> map = CommonUtil.getContext().getBeansOfType(GmListener.class);
            for (Map.Entry<String, GmListener> en : map.entrySet()) {
                CommonResult<String> gmResult = en.getValue().gm(playerController, arr);
                if (gmResult == null) {
                    continue;
                }

                if (gmResult.success()) {
                    res.result = gmResult.data;
                    playerController.send(res);
                    log.info("执行gm命令成功1 playerId = {},order = {}", playerController.playerId(), req.order);
                    return;
                }

                if (gmResult.code == Code.NOT_FOUND) {
                    notFound++;
                }
            }

            if (notFound == map.size()) {
                log.debug("未找到该命令 playerId = {},order = {}", playerController.playerId(), req.order);
                res.code = Code.NOT_FOUND;
            } else {
                res.code = Code.FAIL;
            }
        } catch (Exception e) {
            log.error("", e);
            res.code = Code.EXCEPTION;
        }
        playerController.send(res);
    }

    /**
     * gm玩家初始化
     */
    private void init(ResGm res, PlayerController playerController, String order, long goldNum, long diamongNum, int vip, int level) throws Exception {
        CommonResult<Player> result = playerService.gmPlayerInit(playerController.playerId(), goldNum, diamongNum, vip, level, AddType.GM_OPERATOR, null);
        if (!result.success()) {
            res.code = result.code;
            log.debug("使用gm失败 playerId = {},order = {},code = {}", playerController.playerId(), order, result.code);
            return;
        }
        playerController.getPlayer().setGold(result.data.getGold());
        //等级等基本信息
        coreSendMessageManager.buildBaseInfoChangeMessage(playerController, result.data);

        //货币变化信息
        List<MoneyChangeInfo> moneyChangeInfoList = new ArrayList<>();
        if (goldNum > 0) {
            moneyChangeInfoList.add(coreSendMessageManager.buildMoneyChangeInfo(GameConstant.Item.TYPE_GOLD, goldNum, result.data.getGold()));
        }
        if (diamongNum > 0) {
            moneyChangeInfoList.add(coreSendMessageManager.buildMoneyChangeInfo(GameConstant.Item.TYPE_DIAMOND, diamongNum, result.data.getGold()));
        }
        coreSendMessageManager.buildMoneyChangeInfoMessage(playerController.getSession(), moneyChangeInfoList);
    }

    /**
     * gm修改金币
     */
    private void addGold(ResGm res, PlayerController playerController, String order, String params) throws Exception {
        if (params == null || params.isEmpty()) {
            res.code = Code.PARAM_ERROR;
            log.debug("params为空，使用gm失败 playerId = {},order = {}", playerController.playerId(), order);
            return;
        }

        long num = Long.parseLong(params);
        CommonResult<Player> result = playerService.addGold(playerController.playerId(), num, AddType.GM_OPERATOR, null);
        if (!result.success()) {
            res.code = result.code;
            log.debug("使用gm失败 playerId = {},order = {},code = {}", playerController.playerId(), order, result.code);
            return;
        }
        playerController.getPlayer().setGold(result.data.getGold());
//        coreSendMessageManager.buildBaseInfoChangeMessage(playerController, result.data);
        coreSendMessageManager.buildGoldChangeMessage(playerController.getSession(), num, result.data.getGold());
    }

    /**
     * gm修改钻石
     */
    private void addDiamond(ResGm res, PlayerController playerController, String order, String params) throws Exception {
        if (params == null || params.isEmpty()) {
            res.code = Code.PARAM_ERROR;
            log.debug("params为空，使用gm失败 playerId = {},order = {}", playerController.playerId(), order);
            return;
        }

        long num = Long.parseLong(params);
        CommonResult<Player> result = playerService.addDiamond(playerController.playerId(), num, AddType.GM_OPERATOR, null);
        if (!result.success()) {
            res.code = result.code;
            log.debug("使用gm失败 playerId = {},order = {},code = {}", playerController.playerId(), order, result.code);
            return;
        }
        playerController.getPlayer().setDiamond(result.data.getDiamond());
//        coreSendMessageManager.buildBaseInfoChangeMessage(playerController, result.data);
        coreSendMessageManager.buildDiamondChangeMessage(playerController.getSession(), num, result.data.getDiamond());
    }

    /**
     * gm修改vip等级
     */
    private void setVip(ResGm res, PlayerController playerController, String order, String params) throws Exception {
        if (params == null || params.isEmpty()) {
            res.code = Code.PARAM_ERROR;
            log.debug("params为空，使用gm失败 playerId = {},order = {}", playerController.playerId(), order);
            return;
        }

        int num = Integer.parseInt(params);
        CommonResult<Player> result = playerService.setVip(playerController.playerId(), num, AddType.GM_OPERATOR, null);
        if (!result.success()) {
            res.code = result.code;
            log.debug("使用gm失败 playerId = {},order = {},code = {},params = {}", playerController.playerId(), order,
                    result.code, params);
            return;
        }
        playerController.getPlayer().setVipLevel(result.data.getVipLevel());
        coreSendMessageManager.buildBaseInfoChangeMessage(playerController, result.data);
    }

    private void addItem(ResGm res, PlayerController playerController, String[] orders) throws Exception {
        if (orders.length < 3) {
            res.code = Code.PARAM_ERROR;
            log.debug("orders 为空，使用gm失败 playerId = {},orders = {}", playerController.playerId(), orders);
            return;
        }

        int itemId = Integer.parseInt(orders[1]);
        int count = Integer.parseInt(orders[2]);

        CommonResult<ItemOperationResult> result = playerPackService.addItem(playerController.playerId(), itemId, count, AddType.GM_OPERATOR);
        if (!result.success()) {
            res.code = result.code;
            log.debug("使用gm失败 playerId = {},orders = {}", playerController.playerId(), orders);
            return;
        }
        playerController.send(res);
        log.debug("添加道具成功 playerId = {},orders = {}", playerController.playerId(), orders);
    }

    @Command(MessageConst.CoreMessage.REQ_CONFIRM_PLAYER_SCENE)
    public void reqConfirmPlayerScene(PlayerController playerController, ReqConfirmPlayerScene req) {
        // 获取当前节点类型
        NodeType nodeType = NodeType.getNodeTypeByName(nodeConfig.getType());
        // 如果玩家在房间中
        ResConfirmPlayerScene res = new ResConfirmPlayerScene(Code.SUCCESS);
        res.sceneType = nodeType == NodeType.GAME ? ESceneType.ROOM : ESceneType.HALL;
        playerController.send(res);
    }

    /**
     * 请求加载所有小红点
     */
    @Command(MessageConst.CoreMessage.REQ_RED_DOT)
    public void loadRedDot(PlayerController playerController, ReqRedDot req) {
        redDotManager.notifyReddot(playerController, req.getModule(), req.getSubmodule());
    }

    /**
     * 消息订阅处理
     */
    @Command(MessageConst.CoreMessage.REQ_SUBSCRIBE_TOPIC)
    public void subscription(PlayerController playerController, ReqSubscription msg) {
        String topic = msg.getTopic();
        ResSubscription res = new ResSubscription(Code.SUCCESS);
        res.setSubscription(msg.isSubscription());
        res.setTopic(topic);
        if (topic == null || topic.isEmpty()) {
            res.code = Code.PARAM_ERROR;
            playerController.send(res);
            return;
        }
        SubscriptionTopic subscriptionTopic = SubscriptionTopic.getTopic(e -> e.equals(topic));
        if (subscriptionTopic == null) {
            log.info("玩家[{}]订阅未知主题[{}]", playerController.playerId(), topic);
            res.code = Code.PARAM_ERROR;
            playerController.send(res);
            return;
        }
        if (msg.isSubscription()) {
            subscriptionManager.subscription(subscriptionTopic, playerController.playerId());
        } else {
            subscriptionManager.unsubscription(subscriptionTopic, playerController.playerId());
        }
        playerController.send(res);
        log.debug("玩家消息订阅处理 playerId = {},res = {}", playerController.playerId(), JSON.toJSONString(res));
    }

    /**
     * 请求预下单
     */
    @Command(MessageConst.CoreMessage.REQ_GENERATE_ORDER)
    public void generateOrder(PlayerController playerController, ReqGenerateOrder req) {
        ResGenerateOrder res = new ResGenerateOrder(Code.SUCCESS);
        try {
            PayType payType = PayType.valueOf(req.payType);
            if (payType == null) {
                log.debug("payType 类型错误 playerId = {},req = {}", playerController.playerId(), JSON.toJSONString(req));
                res.code = Code.PARAM_ERROR;
                playerController.send(res);
                return;
            }

            RechargeType rechargeType = RechargeType.valueOf(req.rechargeType);

            if (req.rechargeType < 1 || rechargeType == null) {
                log.debug("rechargeType 类型错误 playerId = {},req = {}", playerController.playerId(), JSON.toJSONString(req));
                res.code = Code.PARAM_ERROR;
                playerController.send(res);
                return;
            }
            BigDecimal price = null;
            for (OrderGenerate generate : SystemInterfaceHolder.getGameSysInterface(OrderGenerate.class)) {
                try {
                    if (generate.getRechargeType() == rechargeType) {
                        price = generate.generateOrderDetailInfo(playerController.getPlayer(), req);
                        break;
                    }
                } catch (Exception e) {
                    log.error("预下单获取 价格失败 playerId = {},req = {}", playerController.playerId(), JSON.toJSONString(req), e);
                }
            }
            if (price == null) {
                log.debug("预下单失败 playerId = {},req = {}", playerController.playerId(), JSON.toJSONString(req));
                res.code = Code.FAIL;
                playerController.send(res);
                return;
            }
            Order order = orderService.generateOrder(playerController.getPlayer(), payType, req.productId, price, rechargeType);
            if (order == null) {
                log.debug("预下单失败11 playerId = {},req = {}", playerController.playerId(), JSON.toJSONString(req));
                res.code = Code.FAIL;
                playerController.send(res);
                return;
            }

            if (payType == PayType.IOS) {
                res.orderId = order.getUuid();
            } else {
                res.orderId = order.getId();
            }
            log.debug("玩家预下单 req = {},resp = {}", JSON.toJSONString(req), JSON.toJSONString(res));
            //如果有测试充值url直接调用
            if (StringUtils.isNotEmpty(nodeConfig.getTestRechargeUrl())) {
                log.debug("测试充值玩家预下单调用 req = {},resp = {}", JSON.toJSONString(req), JSON.toJSONString(res));
                HttpUtils.HttpResponse httpResponse = HttpUtils.doPostWithJSON(nodeConfig.getTestRechargeUrl() + order.getId(), "");
                if (!httpResponse.isOk()) {
                    log.debug("测试充值玩家预下单调用失败 req = {},resp = {}", JSON.toJSONString(req), JSON.toJSONString(res));
                    res.code = Code.FAIL;
                    playerController.send(res);
                }
            }
        } catch (Exception e) {
            log.error("", e);
            res.code = Code.EXCEPTION;
        }
        playerController.send(res);
    }

    /**
     * 获取玩家的账户信息
     */
    @Command(MessageConst.CoreMessage.REQ_PLAYER_MONEY)
    public void reqPlayerMoney(PlayerController playerController, ReqPlayerMoney req) {
        ResPlayerMoney res = new ResPlayerMoney(Code.SUCCESS);
        try {
            Player player = playerService.get(playerController.getPlayer().getId());
            if (player == null) {
                log.debug("未找到该玩家信息 playerId = {}", playerController.playerId());
                res.code = Code.NOT_FOUND;
                playerController.send(res);
                return;
            }

            res.gold = player.getGold();
            res.diamond = player.getDiamond();
            res.safeBoxGold = player.getSafeBoxGold();
            res.safeBoxDiamond = player.getSafeBoxDiamond();
            log.debug("返回玩家账户信息 resp = {}", JSON.toJSONString(res));
        } catch (Exception e) {
            log.error("", e);
            res.code = Code.EXCEPTION;
        }
        playerController.send(res);
    }

    /**
     * 选择游戏场次进入
     *
     * @param playerController
     * @param req
     */
    @Command(MessageConst.CoreMessage.REQ_CHOOSE_WARE)
    public void reqChooseWare(PlayerController playerController, ReqChooseWare req) {
        if (this.chooseWareListenerMap == null || this.chooseWareListenerMap.isEmpty()) {
            log.warn("chooseWareListenerMap 为空，选择场次失败");
            return;
        }
        this.chooseWareListenerMap.forEach((s, listener) -> {
            listener.onChooseWare(playerController, req);
        });
    }

}
