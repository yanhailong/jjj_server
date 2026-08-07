package com.jjg.game.sim.listener;

import com.jjg.game.core.base.condition.numeric.ActionConditionEvent;
import com.jjg.game.sim.data.SimPlayerContext;

import java.util.function.Consumer;

/**
 * "拥有量/总量"型任务条件的状态补报口。
 * <p>
 * 12208(建筑)/12212(雇员)/12214(游客)/12216(解锁游戏) 这类条件是 SET 语义, 平时只在对应动作
 * (升级/招募/升星/解锁建筑) 发生时上报。若玩家在任务节点激活前就已满足, 节点会一直停在 0 进度。
 * 由本接口在登录和打开任务界面时把当前状态重报一次, 补齐这个缺口。
 *
 * @author 11
 * @date 2026/7/28
 */
public interface SimTaskStateReporter {

    /**
     * 把本模块的当前持有量/总量重新上报给任务条件系统 (SET 语义, 重复上报幂等)。
     *
     * @param ctx  玩家上下文 (登录期尚未入 registry)
     * @param sink 状态事件接收口; 由任务服务统一决定是否通知客户端
     */
    void reportTaskState(SimPlayerContext ctx, Consumer<ActionConditionEvent> sink);
}
