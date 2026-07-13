package com.jjg.game.season.config;

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
 */
public record SeasonTrialDef(int trialId, int day, int conditionId, int gameType, int windowSpins,
                             long param, int rechargeChannel, long[] starTargets, List<TaskCfg> starTasks) {

    public static final int STAR_COUNT = 3;

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
}
