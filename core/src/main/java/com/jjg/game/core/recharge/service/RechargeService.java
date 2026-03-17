package com.jjg.game.core.recharge.service;

import cn.hutool.core.collection.CollectionUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.protostuff.PFSession;
import com.jjg.game.common.utils.ObjectMapperUtil;
import com.jjg.game.core.base.condition.handler.TodayDepositCondition;
import com.jjg.game.core.base.gameevent.GameEventManager;
import com.jjg.game.core.base.gameevent.PlayerEventCategory;
import com.jjg.game.core.constant.TaskConstant;
import com.jjg.game.core.dao.CountDao;
import com.jjg.game.core.dao.PlayerRechargeFlowDao;
import com.jjg.game.core.data.Order;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.listener.TmpRechargeListener;
import com.jjg.game.core.pb.NotifyPayInfo;
import com.jjg.game.core.pb.NotifyRechargeServer;
import com.jjg.game.core.recharge.dao.OfflineRechargeDao;
import com.jjg.game.core.service.CorePlayerService;
import com.jjg.game.core.service.OrderService;
import com.jjg.game.core.task.manager.TaskManager;
import com.jjg.game.core.task.param.DefaultTaskConditionParam;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

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
    private final List<TmpRechargeListener> tmpRechargeListeners;

    public RechargeService(OfflineRechargeDao offlineRechargeDao,
                           CorePlayerService playerService,
                           OrderService orderService,
                           GameEventManager gameEventManager,
                           CountDao countDao,
                           PlayerRechargeFlowDao playerRechargeFlowDao,
                           TaskManager taskManager,
                           ClusterSystem clusterSystem,
                           TodayDepositCondition conditionManager, List<TmpRechargeListener> tmpRechargeListeners) {
        this.offlineRechargeDao = offlineRechargeDao;
        this.playerService = playerService;
        this.orderService = orderService;
        this.gameEventManager = gameEventManager;
        this.countDao = countDao;
        this.playerRechargeFlowDao = playerRechargeFlowDao;
        this.taskManager = taskManager;
        this.clusterSystem = clusterSystem;
        this.todayDepositCondition = conditionManager;
        this.tmpRechargeListeners = tmpRechargeListeners;
    }

    public void loadOfflineRecharge(long playerId) {
        try {
            List<String> offlineRechargeJson = offlineRechargeDao.pollAll(playerId);
            if (CollectionUtil.isEmpty(offlineRechargeJson)) {
                return;
            }
            ObjectMapper mapper = ObjectMapperUtil.getDefualtConfigObjectMapper();
            for (String json : offlineRechargeJson) {
                NotifyRechargeServer notifyRechargeServer = null;
                try {
                    notifyRechargeServer = mapper.readValue(json, NotifyRechargeServer.class);
                } catch (Exception e) {
                    log.info("离线充值解析失败 playerId = {},json = {}", playerId, json);
                }
                if (notifyRechargeServer == null) {
                    continue;
                }
                log.info("离线充值成功 playerId = {},orderId = {}", playerId, notifyRechargeServer.orderId);
                notifyRecharge(notifyRechargeServer, false);
            }
        } catch (Exception e) {
            log.error("检查玩家离线充值数据异常 playerId = {}", playerId, e);
        }
    }

    public void notifyRecharge(NotifyRechargeServer notify, boolean needCheck) {
        try {
            if (needCheck) {
                PFSession session = clusterSystem.getSession(notify.playerId);
                if (session == null) {
                    log.error("处理充值时玩家PFSession为null playerId={}", notify.playerId);
                    offlineRechargeDao.addRecharge(notify.playerId, ObjectMapperUtil.getDefualtConfigObjectMapper().writeValueAsString(notify));
                    return;
                }
            }
            Player player = playerService.get(notify.playerId);
            Order order = orderService.getOrder(notify.orderId);
            if (order == null) {
                log.error("处理充值时订单不存在 playerId = {},orderId = {}", notify.playerId, notify.orderId);
                return;
            }
            addRechargeFlowWithCompensate(order, notify.playerId);

            //todo 因为3.0版本的充值事件有改动，避免合代码麻烦，所以这里做临时修改
            int allRechargeCount = 0;

            if (StringUtils.isEmpty(order.getDesc())) {
                todayDepositCondition.addBaseProgress(player.getId(), order.getPrice());
                //充值事件
                gameEventManager.triggerEvent(new PlayerEventCategory.PlayerRechargeEvent(player, order, notify.money, notify.regionCode, notify.channelProductId));
                //任务条件参数
                Supplier<DefaultTaskConditionParam> paramSupplier = () -> {
                    DefaultTaskConditionParam param = new DefaultTaskConditionParam();
                    param.setAddValue(order.getPrice().multiply(BigDecimal.valueOf(100)).longValue());
                    return param;
                };
                Map<String, Object> resMap = countDao.incrRechargeInfo(String.valueOf(player.getId()), order.getPrice());
                //单笔充值任务
                taskManager.trigger(order.getPlayerId(), TaskConstant.ConditionType.PLAYER_PAY, paramSupplier);
                //累计充值任务
                taskManager.trigger(order.getPlayerId(), TaskConstant.ConditionType.PLAYER_SUM_PAY, paramSupplier);

                allRechargeCount = resMap == null ? 0 : ((Long) resMap.get(CountDao.CountType.RECHARGE_COUNT.getParam())).intValue();
            } else {
                if (CollectionUtil.isNotEmpty(this.tmpRechargeListeners)) {
                    this.tmpRechargeListeners.forEach(bean -> bean.recharge(player, order, notify.money, notify.regionCode, notify.channelProductId));
                }

                Long countLong = countDao.getCountLong(CountDao.CountType.RECHARGE_COUNT.getParam(), String.valueOf(player.getId()));
                if (countLong != null) {
                    allRechargeCount = countLong.intValue();
                }
            }

            NotifyPayInfo notifyPayInfo = new NotifyPayInfo();
            notifyPayInfo.orderId = order.getId();

            notifyPayInfo.allRechargeCount = allRechargeCount;
            clusterSystem.sendToPlayer(notifyPayInfo, player.getId());
            log.info("充值成功，通知到玩家所在的当前节点 playerId = {},orderId = {}", player.getId(), order.getId());
        } catch (Exception e) {
            log.error("处理充值出现异常 playerId = {},orderId = {}", notify.playerId, notify.orderId, e);
        }
    }


    private void addRechargeFlowWithCompensate(Order order, long playerId) {
        try {
            playerRechargeFlowDao.addRechargeFlow(order);
        } catch (Exception e) {
            log.error("记录玩家充值流水失败 playerId = {},orderId = {}", playerId, order.getId(), e);
        }
    }

}
