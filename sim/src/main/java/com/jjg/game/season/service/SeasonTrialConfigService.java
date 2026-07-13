package com.jjg.game.season.service;

import com.jjg.game.core.constant.TaskConstant;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.TaskCfg;
import com.jjg.game.season.config.SeasonTrialDef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 赛季试炼关卡配置: 由 task 表 taskType=6 推导。
 * <p>
 * 表内无"关卡分组"字段, 约定按 id 升序、品质(Quality) 1/2/3 连续三行为一关;
 * 关卡所属天取自 day 字段 (赛季开启后的第几天, 1=开启当天), 同关三行必须一致。
 * 同关三行必须共享同一条件(类型与前置参数一致), 仅末位目标值升序, 否则整关跳过并告警。
 */
@Service
public class SeasonTrialConfigService {
    private static final Logger log = LoggerFactory.getLogger(SeasonTrialConfigService.class);
    private static final int CONDITION_RECHARGE = 11002;

    /**
     * 测试注入用配置源; null 时走 GameDataManager
     */
    private final List<TaskCfg> tasks;

    public SeasonTrialConfigService() {
        this.tasks = null;
    }

    public SeasonTrialConfigService(List<TaskCfg> tasks) {
        this.tasks = tasks == null ? List.of() : List.copyOf(tasks);
    }

    public List<SeasonTrialDef> trials() {
        return index().trials;
    }

    public SeasonTrialDef trial(int trialId) {
        return index().byId.get(trialId);
    }

    // ---------------------------------------------------------------------
    // 索引 (按配置列表引用做 identity 缓存, 热更整体换引用时懒重建)
    // ---------------------------------------------------------------------

    private volatile TrialIndex index;

    private TrialIndex index() {
        List<TaskCfg> source = taskConfigs();
        TrialIndex cached = index;
        if (cached == null || cached.source != source) {
            cached = new TrialIndex(source, build(source));
            index = cached;
        }
        return cached;
    }

    private static final class TrialIndex {
        final List<TaskCfg> source;
        final List<SeasonTrialDef> trials;
        final Map<Integer, SeasonTrialDef> byId = new HashMap<>();

        TrialIndex(List<TaskCfg> source, List<SeasonTrialDef> trials) {
            this.source = source;
            this.trials = trials;
            trials.forEach(def -> byId.put(def.trialId(), def));
        }
    }

    private List<SeasonTrialDef> build(List<TaskCfg> source) {
        List<TaskCfg> rows = new ArrayList<>();
        for (TaskCfg cfg : source) {
            if (cfg.getTaskType() == TaskConstant.TaskType.SEASON_TRIAL) {
                rows.add(cfg);
            }
        }
        rows.sort(Comparator.comparingInt(TaskCfg::getId));

        List<SeasonTrialDef> trials = new ArrayList<>();
        List<TaskCfg> group = new ArrayList<>(SeasonTrialDef.STAR_COUNT);
        for (TaskCfg cfg : rows) {
            //品质回到 1 视为新关卡起点
            if (cfg.getQuality() == 1 && !group.isEmpty()) {
                addTrial(trials, group);
                group = new ArrayList<>(SeasonTrialDef.STAR_COUNT);
            }
            group.add(cfg);
        }
        if (!group.isEmpty()) {
            addTrial(trials, group);
        }
        return List.copyOf(trials);
    }

    private void addTrial(List<SeasonTrialDef> trials, List<TaskCfg> group) {
        int trialId = trials.size() + 1;
        SeasonTrialDef def = parse(trialId, group);
        if (def != null) {
            trials.add(def);
        }
    }

    /**
     * 一关三行 -> 试炼定义。条件格式 (taskConditionId):
     * <pre>
     * 12601: [id, 游戏id, 局数, 累计赢奖]
     * 12602: [id, 游戏id, 局数, 游戏模式id, 触发次数]
     * 12603: [id, 游戏id, 局数, 中奖倍数, 达标次数]
     * 12604: [id, 游戏id, 局数, 中奖次数]
     * 12605: [id, 游戏id, 局数, 游戏元素id, 出现次数]
     * 12606: [id, 游戏id, 局数, 单局中奖金额]
     * 11002: [id, 渠道id, 累计充值金额] (被动型)
     * </pre>
     * 目标值统一取末位, 前面部分三行必须一致。
     */
    private SeasonTrialDef parse(int trialId, List<TaskCfg> group) {
        if (group.size() != SeasonTrialDef.STAR_COUNT) {
            log.warn("赛季试炼配置无效: 关卡行数不是 {} 行 trialId={},taskIds={}",
                    SeasonTrialDef.STAR_COUNT, trialId, ids(group));
            return null;
        }
        List<Long> base = group.getFirst().getTaskConditionId();
        if (base == null || base.size() < 3) {
            log.warn("赛季试炼配置无效: 条件参数不足 trialId={},taskIds={}", trialId, ids(group));
            return null;
        }
        int day = group.getFirst().getDay();
        if (day < 1) {
            log.warn("赛季试炼配置无效: day 字段缺失或非法 trialId={},taskIds={}", trialId, ids(group));
            return null;
        }
        long[] targets = new long[SeasonTrialDef.STAR_COUNT];
        for (int i = 0; i < group.size(); i++) {
            TaskCfg cfg = group.get(i);
            List<Long> cond = cfg.getTaskConditionId();
            if (cfg.getQuality() != i + 1 || cond == null || cond.size() != base.size()
                    || !cond.subList(0, cond.size() - 1).equals(base.subList(0, base.size() - 1))) {
                log.warn("赛季试炼配置无效: 星级/条件前缀不一致 trialId={},taskIds={}", trialId, ids(group));
                return null;
            }
            if (cfg.getDay() != day) {
                log.warn("赛季试炼配置无效: 同关卡 day 不一致 trialId={},taskIds={}", trialId, ids(group));
                return null;
            }
            targets[i] = cond.getLast();
            if (i > 0 && targets[i] <= targets[i - 1]) {
                log.warn("赛季试炼配置无效: 星级目标未递增 trialId={},taskIds={}", trialId, ids(group));
                return null;
            }
        }
        int conditionId = base.getFirst().intValue();
        if (conditionId == CONDITION_RECHARGE) {
            return new SeasonTrialDef(trialId, day, conditionId, 0, 0, 0,
                    base.get(1).intValue(), targets, List.copyOf(group));
        }
        int gameType = base.get(1).intValue();
        int windowSpins = base.get(2).intValue();
        long param = base.size() > 4 ? base.get(3) : 0;
        if (windowSpins <= 0) {
            log.warn("赛季试炼配置无效: 挑战窗口局数非法 trialId={},taskIds={}", trialId, ids(group));
            return null;
        }
        return new SeasonTrialDef(trialId, day, conditionId, gameType, windowSpins, param, 0,
                targets, List.copyOf(group));
    }

    private static List<Integer> ids(List<TaskCfg> group) {
        return group.stream().map(TaskCfg::getId).toList();
    }

    private List<TaskCfg> taskConfigs() {
        if (tasks != null) {
            return tasks;
        }
        List<TaskCfg> list = GameDataManager.getTaskCfgList();
        return list == null ? List.of() : list;
    }
}
