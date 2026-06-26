package com.jjg.game.alliance.service;

import com.jjg.game.sim.data.SpinStatInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 联盟事件门面 —— 其他系统接入联盟的唯一上报入口 (核心扩展点)。
 * <p>
 * 任何玩法产生的玩家行为 (slots 旋转/赚金币/中奖/消耗体力/未来新玩法) 调用本类即可同时驱动:
 * 联盟任务进度 + 联盟对决积分掉落。任务条件类型来自 task.xlsx 的 taskConditionId 首位。
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
     * @param conditionId task.xlsx 的 taskConditionId 首位
     * @param param       事件参数
     * @param value       增量
     */
    public void onEvent(long playerId, int conditionId, long param, long value) {
        try {
            taskService.onProgress(playerId, conditionId, param, value);
        } catch (Exception e) {
            log.error("联盟事件处理失败 playerId={},conditionId={},param={},value={}", playerId, conditionId, param, value, e);
        }
    }

    /**
     * slots 旋转联动入口 (sim 在 SimManager.onSlotsSpin 处调用):
     * 消耗体力 + 中奖倍数 两类任务进度 + 对决积分掉落。
     * 赚金币类任务因旋转链路无金币值, 由游戏节点经 ToAllianceBridge 单独上报。
     *
     * @param gameType  slots 玩法类型
     * @param winTimes  本次中奖倍数
     * @param costPower 本次消耗体力
     */
    public void onSpin(long playerId, int gameType, int winTimes, int costPower, SpinStatInfo statInfo) {
        try {
            if (costPower > 0) {
                //需求: 任意常规玩法消耗体力均有概率掉落对决积分
                battleService.onPowerConsumed(playerId, costPower);
            }
            taskService.onSpin(playerId, gameType, winTimes, costPower, statInfo);
        } catch (Exception e) {
            log.error("联盟spin联动失败 playerId={},gameType={},winTimes={}", playerId, gameType, winTimes, e);
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
