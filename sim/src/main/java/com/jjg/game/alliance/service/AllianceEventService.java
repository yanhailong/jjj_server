package com.jjg.game.alliance.service;

import com.jjg.game.alliance.constant.AllianceConst;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 联盟事件门面 —— 其他系统接入联盟的唯一上报入口 (核心扩展点)。
 * <p>
 * 任何玩法产生的玩家行为 (slots 旋转/赚金币/中奖/消耗体力/未来新玩法) 调用本类即可同时驱动:
 * 联盟任务进度 + 联盟对决积分掉落。新增任务目标类型只需扩展
 * {@link AllianceConst.TaskGoalType} 并在产生行为处调用 {@link #onEvent}。
 * <p>
 * 全部方法不抛异常 (内部吞掉并打日志), 保证联盟侧故障不影响游戏主流程;
 * 高频路径在 {@code AllianceTaskService}/{@code AllianceBattleService} 内有本地缓存短路。
 *
 * @author 11
 * @date 2026/6/11
 */
@Service
public class AllianceEventService {
    private static final Logger log = LoggerFactory.getLogger(AllianceEventService.class);

    @Autowired
    private AllianceTaskService taskService;
    @Autowired
    private AllianceBattleService battleService;

    /**
     * 通用事件上报: 驱动联盟任务进度。
     *
     * @param goalType 目标类型 (AllianceConst.TaskGoalType)
     * @param param    事件参数 (EARN_GOLD=gameType / WIN_TIMES=本次倍数)
     * @param value    增量
     */
    public void onEvent(long playerId, int goalType, long param, long value) {
        try {
            taskService.onProgress(playerId, goalType, param, value);
        } catch (Exception e) {
            log.error("联盟事件处理失败 playerId={},goalType={},param={},value={}", playerId, goalType, param, value, e);
        }
    }

    /**
     * slots 旋转联动入口 (sim 在 SimManager.onSlotsSpin 处调用):
     * 消耗体力 + 中奖倍数 两类任务进度 + 对决积分掉落。
     * 赚金币(EARN_GOLD)任务因旋转链路无金币值, 由游戏节点经 ToAllianceBridge 单独上报。
     *
     * @param gameType  slots 玩法类型
     * @param winTimes  本次中奖倍数
     * @param costPower 本次消耗体力
     */
    public void onSpin(long playerId, int gameType, int winTimes, int costPower) {
        try {
            if (costPower > 0) {
                taskService.onProgress(playerId, AllianceConst.TaskGoalType.COST_POWER, 0, costPower);
                //需求: 任意常规玩法消耗体力均有概率掉落对决积分
                battleService.onPowerConsumed(playerId, costPower);
            }
            if (winTimes > 0) {
                taskService.onProgress(playerId, AllianceConst.TaskGoalType.WIN_TIMES, winTimes, 1);
            }
        } catch (Exception e) {
            log.error("联盟spin联动失败 playerId={},gameType={},winTimes={}", playerId, gameType, winTimes, e);
        }
    }

    /**
     * 赚取金币上报 (EARN_GOLD 任务): 由有金币结算值的链路调用 (游戏节点经 bridge / 大厅结算处)。
     *
     * @param gameType 玩法类型 (任务配置 goalParam=0 表示不限玩法)
     * @param gold     本次赚取金币数
     */
    public void onEarnGold(long playerId, int gameType, long gold) {
        onEvent(playerId, AllianceConst.TaskGoalType.EARN_GOLD, gameType, gold);
    }
}
