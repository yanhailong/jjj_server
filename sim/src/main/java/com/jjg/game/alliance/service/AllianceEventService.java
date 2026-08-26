package com.jjg.game.alliance.service;

import com.jjg.game.core.base.condition.numeric.ActionConditionEvent;
import com.jjg.game.core.base.condition.numeric.ConditionEvent;
import com.jjg.game.core.base.condition.numeric.GameConditionEvent;
import com.jjg.game.core.base.condition.numeric.RechargeConditionEvent;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.listener.ItemConsumeListener;
import com.jjg.game.core.service.PlayerStatService;
import com.jjg.game.core.task.pb.Task;
import com.jjg.game.core.utils.ItemUtils;
import com.jjg.game.sim.data.SimPlayerContext;
import com.jjg.game.sim.manager.SimPlayerContextRegistry;
import com.jjg.game.sim.service.SimConditionEventFactory;
import com.jjg.game.sim.service.SimPackService;
import com.jjg.game.sim.service.SimTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * sim 玩法条件事件门面（保留原类名以兼容现有调用方）。
 * <p>
 * 玩法只上报建筑升级、抽卡、充值等事实事件，本类同步投递给 sim 主线/成就和联盟任务；
 * 条件 id 只存在于 task 配置与 core 规则注册表中。slots 旋转额外驱动联盟对决积分掉落。
 * <p>
 * 两个任务消费者分别隔离异常，任一侧故障不影响玩法主流程；联盟高频路径先经本地任务缓存短路。
 *
 * @author 11
 * @date 2026/6/11
 */
@Service
public class AllianceEventService implements ItemConsumeListener {
    private static final Logger log = LoggerFactory.getLogger(AllianceEventService.class);

    @Autowired
    private AllianceTaskService taskService;
    @Autowired
    private AllianceBattleService battleService;
    @Autowired
    private SimTaskService simTaskService;
    @Autowired
    private SimPlayerContextRegistry simPlayerContextRegistry;
    @Autowired
    private PlayerStatService playerStatService;
    @Lazy
    @Autowired
    private SimPackService simPackService;

    /** 通用扩展入口：业务只上报事实事件，具体 condition id 由当前任务配置决定。 */
    public void onConditionEvent(long playerId, ConditionEvent event) {
        try {
            taskService.onConditionEvent(playerId, event);
        } catch (Exception e) {
            log.error("联盟任务条件事件处理失败 playerId={},event={}", playerId, event, e);
        }
        onSimOperation(playerId, event);
    }

    /**
     * 仅驱动 sim 主线/成就任务的经营类事件 (12208-12220 等 condition 不用于联盟任务, 无需过联盟侧)。
     * 玩家未在 sim 在线时静默跳过, 不影响玩法主流程。
     */
    public void onSimOperation(long playerId, ConditionEvent event) {
        try {
            SimPlayerContext ctx = simPlayerContextRegistry.getContext(playerId);
            if (ctx != null) {
                simTaskService.onConditionEvent(ctx, event);
            }
        } catch (Exception e) {
            log.error("sim 任务条件事件处理失败 playerId={},event={}", playerId, event, e);
        }
    }

    /** 观看广告一次 (清 CD / 领离线收益走广告倍数): 推进 12209。 */
    public void onAdWatch(long playerId) {
        playerStatService.recordAdWatch(playerId);
        onSimOperation(playerId, SimConditionEventFactory.adWatch());
    }

    /** 拜访一次: 推进 12217。 */
    public void onVisit(long playerId) {
        playerStatService.recordVisit(playerId);
        onSimOperation(playerId, SimConditionEventFactory.visit());
    }

    /** 一次经营金币收益 (自产 / 离线 / 游客产出): 推进 12215。 */
    public void onBusinessIncome(long playerId, long gold) {
        if (gold > 0) {
            onBusinessIncome(playerId, Map.of(ItemUtils.getGoldItemId(), gold));
        }
    }

    /** 一次经营收益，按实际到账道具分别累计。 */
    public void onBusinessIncome(long playerId, Map<Integer, Long> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        playerStatService.recordBusinessIncome(playerId, items);
        items.forEach((itemId, count) -> {
            if (itemId != null && count != null && count > 0) {
                onSimOperation(playerId, SimConditionEventFactory.businessIncome(itemId, count));
            }
        });
    }

    /** 一次道具消费: 推进 12220 (按 itemId 过滤金币 / 钻石)。 */
    public void onItemConsume(long playerId, int itemId, long count) {
        if (count > 0) {
            onSimOperation(playerId, SimConditionEventFactory.itemConsume(itemId, count));
        }
    }

    /** 背包道具扣除成功后由 PlayerPackService 回调 (体力/知名度/赛季币等特殊资源不计入消费)。 */
    @Override
    public void onItemsConsumed(long playerId, Map<Integer, Long> items, AddType addType) {
        if (simPlayerContextRegistry.getContext(playerId) != null) {
            items.forEach((itemId, count) -> onItemConsume(playerId, itemId, count));
            return;
        }
        simPackService.forwardPackItemsConsumed(playerId, items, addType);
    }

    /** 跨节点消费事件只返回任务变化，由 RPC 边界负责通知当前客户端会话。 */
    public List<Task> collectItemConsumeTaskUpdates(SimPlayerContext ctx, Map<Integer, Long> items) {
        if (ctx == null || items == null || items.isEmpty()) {
            return List.of();
        }
        List<Task> updates = new ArrayList<>();
        items.forEach((itemId, count) -> {
            if (count != null && count > 0) {
                updates.addAll(simTaskService.collectConditionEventUpdates(
                        ctx, SimConditionEventFactory.itemConsume(itemId, count)));
            }
        });
        return updates.isEmpty() ? List.of() : List.copyOf(updates);
    }

    public void onBuildingUpgrade(long playerId, int buildingId, int level) {
        playerStatService.recordBuildingUpgrade(playerId, buildingId);
        onConditionEvent(playerId, new ActionConditionEvent(ActionConditionEvent.Type.BUILDING_UPGRADE,
                buildingId, 0, 0, 1, 0, false));
        onConditionEvent(playerId, new ActionConditionEvent(ActionConditionEvent.Type.BUILDING_LEVEL,
                buildingId, 0, level, 1, 0, false));
    }

    public void onBuildingLevel(long playerId, int buildingId, int level) {
        onSimOperation(playerId, new ActionConditionEvent(ActionConditionEvent.Type.BUILDING_LEVEL,
                buildingId, 0, level, 1, 0, false));
    }

    public void onCardPoolDraw(long playerId, int poolId, long count) {
        onConditionEvent(playerId, new ActionConditionEvent(ActionConditionEvent.Type.CARD_POOL_DRAW,
                poolId, 0, 0, count, 0, false));
    }

    public void onGuestPoolDraw(long playerId, long count) {
        if (count <= 0) {
            return;
        }
        playerStatService.recordGuestPoolDraw(playerId, count);
        onSimOperation(playerId, new ActionConditionEvent(ActionConditionEvent.Type.GUEST_POOL_DRAW,
                0, 0, 0, count, 0, false));
    }

    public void onEmployeePoolDraw(long playerId, long count) {
        if (count <= 0) {
            return;
        }
        playerStatService.recordEmployeePoolDraw(playerId, count);
        onSimOperation(playerId, new ActionConditionEvent(ActionConditionEvent.Type.EMPLOYEE_POOL_DRAW,
                0, 0, 0, count, 0, false));
    }

    public void onGameResearch(long playerId, int gameType) {
        onConditionEvent(playerId, new ActionConditionEvent(ActionConditionEvent.Type.GAME_RESEARCH,
                gameType, 0, 0, 1, 0, false));
    }

    public void onEmployeeRecruit(long playerId, int professionId, long count) {
        onConditionEvent(playerId, new ActionConditionEvent(ActionConditionEvent.Type.EMPLOYEE_RECRUIT,
                professionId, 0, 0, count, 0, false));
    }

    public void onGuestRecruit(long playerId, boolean paid, long count) {
        onConditionEvent(playerId, new ActionConditionEvent(ActionConditionEvent.Type.GUEST_RECRUIT,
                0, 0, 0, count, 0, paid));
    }

    /** 定时或购买生成游客，累计玩家级招商次数。 */
    public void onGuestGenerated(long playerId, boolean paid, long count) {
        if (count <= 0) {
            return;
        }
        playerStatService.recordGuestRecruit(playerId, paid, count);
        onSimOperation(playerId, new ActionConditionEvent(ActionConditionEvent.Type.GUEST_RECRUIT,
                0, 0, 0, count, 0, paid));
    }

    public void onDonate(long playerId, long amount) {
        onConditionEvent(playerId, new ActionConditionEvent(ActionConditionEvent.Type.ALLIANCE_DONATE,
                0, 0, amount, 1, 0, false));
    }

    public void onRecharge(long playerId, int channelId, long amount) {
        onConditionEvent(playerId, new RechargeConditionEvent(channelId, amount));
    }

    /**
     * 兼容现有跨节点 RPC 的旧数值协议。协议暂不改动，进入 hall 后立即转换为统一事实事件；
     * 新业务应直接使用上面的事实事件方法。
     */
    public void onEvent(long playerId, int conditionId, long param, long value) {
        switch (conditionId) {
            case 12206 -> onAllianceConditionEvent(playerId,
                    new ActionConditionEvent(ActionConditionEvent.Type.BUILDING_UPGRADE,
                            (int) param, 0, 0, 1, 0, false));
            case 12301 -> onAllianceConditionEvent(playerId,
                    SimConditionEventFactory.fromGameResult(0, Long.MAX_VALUE, 0, param));
            case 12302 -> onAllianceConditionEvent(playerId,
                    SimConditionEventFactory.fromGameResult((int) param, Long.MAX_VALUE, 0, 0));
            case 12303 -> onAllianceConditionEvent(playerId,
                    new ActionConditionEvent(ActionConditionEvent.Type.CARD_POOL_DRAW,
                            (int) param, 0, 0, value, 0, false));
            case 12304 -> onAllianceConditionEvent(playerId,
                    new ActionConditionEvent(ActionConditionEvent.Type.GAME_RESEARCH,
                            (int) param, 0, 0, 1, 0, false));
            case 12305 -> onAllianceConditionEvent(playerId,
                    new ActionConditionEvent(ActionConditionEvent.Type.ALLIANCE_DONATE,
                            0, 0, param, value, 0, false));
            case 12306 -> onAllianceConditionEvent(playerId,
                    SimConditionEventFactory.fromGameResult((int) param, Long.MAX_VALUE, value, 0));
            case 12307 -> onAllianceConditionEvent(playerId, new RechargeConditionEvent((int) param, value));
            default -> log.warn("联盟事件使用了不支持的旧条件 id playerId={},conditionId={}", playerId, conditionId);
        }
    }

    /** 兼容原有 GM 命令入口，复用跨节点旧协议适配器。 */
    public void onGmEvent(long playerId, int conditionId, long param, long value) {
        onEvent(playerId, conditionId, param, value);
    }

    private void onAllianceConditionEvent(long playerId, ConditionEvent event) {
        try {
            taskService.onConditionEvent(playerId, event);
        } catch (Exception e) {
            log.error("联盟旧协议事件处理失败 playerId={},event={}", playerId, event, e);
        }
    }

    /**
     * slots 旋转联动入口 (sim 在 SimManager.onSlotsSpin 处调用):
     * 同一个不可变事件驱动当前联盟任务，消耗体力额外驱动对决积分掉落。
     *
     * @param costPower 本次消耗体力
     * @param event     已由 SimConditionEventFactory 构造的旋转事实
     */
    public void onSpin(long playerId, int costPower, GameConditionEvent event) {
        try {
            if (costPower > 0) {
                //需求: 任意常规玩法消耗体力均有概率掉落对决积分
                battleService.onPowerConsumed(playerId, costPower);
            }
            taskService.onConditionEvent(playerId, event);
        } catch (Exception e) {
            log.error("联盟spin联动失败 playerId={},gameType={},multiple={}",
                    playerId, event == null ? 0 : event.gameType(), event == null ? 0 : event.multiple(), e);
        }
    }

    /**
     * 赚取金币上报: 由有金币结算值的链路调用 (游戏节点经 bridge / 大厅结算处)。
     *
     * @param gameType 玩法类型
     * @param gold     本次赚取金币数
     */
    public void onEarnGold(long playerId, int gameType, long gold) {
        taskService.onEarnGold(playerId, gameType, gold);
    }
}
