package com.jjg.game.core.handler;

import cn.hutool.core.util.EnumUtil;
import com.jjg.game.common.config.NodeConfig;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.curator.NodeType;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.common.utils.CommonUtil;
import com.jjg.game.core.base.gameevent.GameEventManager;
import com.jjg.game.core.base.gameevent.PlayerEventCategory;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.constant.RechargeType;
import com.jjg.game.core.data.*;
import com.jjg.game.core.constant.SubscriptionTopic;
import com.jjg.game.core.listener.GmListener;
import com.jjg.game.core.manager.CoreMarqueeManager;
import com.jjg.game.core.manager.CoreSendMessageManager;
import com.jjg.game.core.manager.RedDotManager;
import com.jjg.game.core.manager.SubscriptionManager;
import com.jjg.game.core.pb.*;
import com.jjg.game.core.pb.reddot.NotifyRedDot;
import com.jjg.game.core.pb.reddot.RedDotDetails;
import com.jjg.game.core.pb.reddot.ReqRedDot;
import com.jjg.game.core.service.CorePlayerService;
import com.jjg.game.core.service.OrderService;
import com.jjg.game.core.service.PlayerPackService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

            if (req.order.isEmpty()) {
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

            if ("addGold".equalsIgnoreCase(cmd)) {
                addGold(res, playerController, req.order, params);
                return;
            }

            if ("addDiamond".equalsIgnoreCase(cmd)) {
                addDiamond(res, playerController, req.order, params);
                return;
            }

            if ("setGold".equalsIgnoreCase(cmd)) {
                setGold(res, playerController, req.order, params);
                return;
            }

            if ("setDiamond".equalsIgnoreCase(cmd)) {
                setDiamond(res, playerController, req.order, params);
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
                        playerService.betDeductGold(playerController.playerId(), num, true, true,"gmtest");
                res.code = result.code;
                playerController.send(res);
                return;
            }

            if ("recharge".equals(cmd)) {
                log.debug("收到充值的gm命令 playerId = {},gmOrders = {}", playerController.playerId(), arr);
                int type = Integer.parseInt(arr[1]);
                //1等级礼包 测试用
                RechargeType rechargeType = EnumUtil.getBy(RechargeType.class, e -> e.getType() == type);
                int id = Integer.parseInt(arr[2]);
                Order order = orderService.generateOrder(playerController.getPlayer().getId(), id, 11, rechargeType);
                gameEventManager.triggerEvent(new PlayerEventCategory.PlayerRechargeEvent(playerController.getPlayer(),order));
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
     * gm修改金币
     */
    private void addGold(ResGm res, PlayerController playerController, String order, String params) throws Exception {
        if (params == null || params.isEmpty()) {
            res.code = Code.PARAM_ERROR;
            log.debug("params为空，使用gm失败 playerId = {},order = {}", playerController.playerId(), order);
            return;
        }

        long num = Long.parseLong(params);
        CommonResult<Player> result = playerService.addGold(playerController.playerId(), num, "gmAddGold", null);
        if (!result.success()) {
            res.code = result.code;
            log.debug("使用gm失败 playerId = {},order = {},code = {}", playerController.playerId(), order, result.code);
            return;
        }
        playerController.getPlayer().setGold(result.data.getGold());
        coreSendMessageManager.buildMoneyChangeMessage(playerController, result.data);
    }

    /**
     * gm修改金币
     */
    private void setGold(ResGm res, PlayerController playerController, String order, String params) throws Exception {
        if (params == null || params.isEmpty()) {
            res.code = Code.PARAM_ERROR;
            log.debug("params为空，使用gm失败 playerId = {},order = {}", playerController.playerId(), order);
            return;
        }

        long num = Long.parseLong(params);
        CommonResult<Player> result = playerService.gmSetGold(playerController.playerId(), num, "gmSetGold", null);
        if (!result.success()) {
            res.code = result.code;
            log.debug("使用gm失败 playerId = {},order = {},code = {}", playerController.playerId(), order, result.code);
            return;
        }
        playerController.getPlayer().setGold(result.data.getGold());
        coreSendMessageManager.buildMoneyChangeMessage(playerController, result.data);
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
        CommonResult<Player> result = playerService.addDiamond(playerController.playerId(), num, "gmAddDiamond", null);
        if (!result.success()) {
            res.code = result.code;
            log.debug("使用gm失败 playerId = {},order = {},code = {}", playerController.playerId(), order, result.code);
            return;
        }
        playerController.getPlayer().setDiamond(result.data.getDiamond());
        coreSendMessageManager.buildMoneyChangeMessage(playerController, result.data);
    }

    /**
     * gm修改钻石
     */
    private void setDiamond(ResGm res, PlayerController playerController, String order, String params) throws Exception {
        if (params == null || params.isEmpty()) {
            res.code = Code.PARAM_ERROR;
            log.debug("params为空，使用gm失败 playerId = {},order = {}", playerController.playerId(), order);
            return;
        }

        long num = Long.parseLong(params);
        CommonResult<Player> result = playerService.gmSetDiamond(playerController.playerId(), num, "gmSetDiamond",
                null);
        if (!result.success()) {
            res.code = result.code;
            log.debug("使用gm失败 playerId = {},order = {},code = {}", playerController.playerId(), order, result.code);
            return;
        }
        playerController.getPlayer().setDiamond(result.data.getDiamond());
        coreSendMessageManager.buildMoneyChangeMessage(playerController, result.data);
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
        CommonResult<Player> result = playerService.setVip(playerController.playerId(), num, "gmSetVip", null);
        if (!result.success()) {
            res.code = result.code;
            log.debug("使用gm失败 playerId = {},order = {},code = {},params = {}", playerController.playerId(), order,
                    result.code, params);
            return;
        }
        playerController.getPlayer().setVipLevel(result.data.getVipLevel());
        coreSendMessageManager.buildMoneyChangeMessage(playerController, result.data);
    }

    private void addItem(ResGm res, PlayerController playerController, String[] orders) throws Exception {
        if (orders.length < 3) {
            res.code = Code.PARAM_ERROR;
            log.debug("orders 为空，使用gm失败 playerId = {},orders = {}", playerController.playerId(), orders);
            return;
        }

        int itemId = Integer.parseInt(orders[1]);
        int count = Integer.parseInt(orders[2]);

        CommonResult<ItemOperationResult> result = playerPackService.addItem(playerController.playerId(), itemId, count, "gmAdd");
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
        List<RedDotDetails> result = new ArrayList<>();
        long playerId = playerController.getPlayer().getId();
        RedDotDetails.RedDotModule module = req.getModule();
        if (module != null) {
            int submodule = req.getSubmodule();
            if (submodule == 0) {
                List<RedDotDetails> redDots = redDotManager.load(module, submodule, playerId);
                result.addAll(redDots);
            }
        } else {
            List<RedDotDetails> redDotDetails = redDotManager.loadAll(playerId);
            result.addAll(redDotDetails);
        }
        NotifyRedDot notifyRedDot = new NotifyRedDot();
        notifyRedDot.setRedDotList(result);
        //回复红点数据
        playerController.send(notifyRedDot);
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
    }

}
