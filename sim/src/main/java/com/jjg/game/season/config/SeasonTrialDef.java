package com.jjg.game.season.config;

import com.jjg.game.core.base.condition.numeric.ConditionRuleRegistry;
import com.jjg.game.core.base.condition.numeric.ConditionSpec;
import com.jjg.game.core.base.condition.numeric.PreparedCondition;
import com.jjg.game.sampledata.bean.TaskCfg;

import java.util.List;

/**
 * 一个试炼关卡的静态定义: task 表 taskType=6 按 id 升序、品质 1/2/3 三行为一关推导。
 * 三行共享同一条件(类型与窗口参数一致), 仅末位目标值不同, 分别对应 1/2/3 星达标线。
 *
 * @param trialId        关卡序号 (1 起, 按 id 升序推导)
 * @param day            所属解锁天 (task 表 day 字段: 赛季开启后的第几天, 1=开启当天)
 * @param conditionId    condition 表 id (12601-12606 挑战型 / 11002 累计充值被动型)
 * @param gameType       条件绑定的游戏 id (0=任意; 11002 时无意义)
 * @param windowSpins    挑战窗口局数 (11002 被动型为 0)
 * @param param          条件附加参数: 12602=游戏模式id, 12603=中奖倍数, 12605=游戏元素id; 其余为 0
 * @param rechargeChannel 11002 充值渠道 (0=全部)
 * @param starTargets    1/2/3 星目标值 (升序)
 * @param starTasks      1/2/3 星对应的任务配置行 (奖励/描述等取自配置)
 * @param condition      已校验的通用条件，挑战热路径直接复用
 */
public record SeasonTrialDef(int trialId, int day, int conditionId, int gameType, int windowSpins,
                             long param, int rechargeChannel, long[] starTargets, List<TaskCfg> starTasks,
                             PreparedCondition condition) {

    public static final int STAR_COUNT = 3;

    /** 兼容原有构造签名；配置服务会传入已准备的 condition，避免生产路径重复构建。 */
    public SeasonTrialDef(int trialId, int day, int conditionId, int gameType, int windowSpins,
                          long param, int rechargeChannel, long[] starTargets, List<TaskCfg> starTasks) {
        this(trialId, day, conditionId, gameType, windowSpins, param, rechargeChannel,
                starTargets, starTasks, prepareLegacy(conditionId, gameType, windowSpins,
                        param, rechargeChannel, starTargets));
    }

    /**
     * 是否被动判定型 (累计充值, 无挑战窗口, 开面板时惰性判定)
     */
    public boolean passive() {
        return conditionId == 11002;
    }

    /**
     * 当前进度值可达的星级 (0=未达 1 星)
     */
    public int starsOf(long progress) {
        int stars = 0;
        for (long target : starTargets) {
            if (progress >= target) {
                stars++;
            }
        }
        return stars;
    }

    /**
     * 满星目标值 (进度达到即可提前结算)
     */
    public long topTarget() {
        return starTargets[starTargets.length - 1];
    }

    private static PreparedCondition prepareLegacy(int conditionId, int gameType, int windowSpins,
                                                     long param, int rechargeChannel, long[] targets) {
        if (targets == null || targets.length == 0) {
            throw new IllegalArgumentException("season trial must contain at least one target");
        }
        long target = targets[0];
        List<Long> values = switch (conditionId) {
            case 11002 -> List.of(11002L, (long) rechargeChannel, target);
            case 12602, 12603, 12605 -> List.of((long) conditionId, (long) gameType,
                    (long) windowSpins, param, target);
            case 12601, 12604, 12606 -> List.of((long) conditionId, (long) gameType,
                    (long) windowSpins, target);
            default -> throw new IllegalArgumentException("unsupported season trial condition id: " + conditionId);
        };
        return ConditionRuleRegistry.standard().prepare(ConditionSpec.from(values));
    }
}
