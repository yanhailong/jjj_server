package com.jjg.game.alliance.service;

import com.jjg.game.alliance.constant.AllianceConst;
import com.jjg.game.common.utils.RandomUtils;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 联盟结构化配置中心 (开发期: 死常量缺省)。
 * <p>
 * 需求中大量"根据配置/走配置"的数值集中在本类, 与 social 的 Cfg 死常量策略一致;
 * 上线前策划 Excel 落表后, 在本类实现 {@code ConfigExcelChangeListener} 并把各取数方法
 * 切到 {@code GameDataManager} 即可, 调用方零改动。标量配置见 {@link AllianceConst.Cfg}。
 *
 * @author 11
 * @date 2026/6/11
 */
@Component
public class AllianceConfigService {

    private static final ZoneId ZONE = ZoneId.systemDefault();

    // =====================================================================
    // 等级表: 索引 = level-1, 值 = 达到该等级所需累计声誉值 (声誉只增不耗)
    // =====================================================================
    private static final long[] LEVEL_REPUTATION = {
            0, 2_000, 5_000, 10_000, 18_000,
            30_000, 48_000, 72_000, 105_000, 150_000,
            210_000, 290_000, 390_000, 520_000, 690_000,
            900_000, 1_160_000, 1_480_000, 1_870_000, 2_340_000};

    /**
     * 按累计声誉推导等级 (1 ~ MAX_LEVEL)
     */
    public int levelOf(long reputation) {
        int level = 1;
        for (int i = LEVEL_REPUTATION.length - 1; i >= 0; i--) {
            if (reputation >= LEVEL_REPUTATION[i]) {
                level = i + 1;
                break;
            }
        }
        return Math.min(level, AllianceConst.Cfg.MAX_LEVEL);
    }

    /**
     * 升到下一级所需累计声誉; 已满级返回 -1
     */
    public long nextLevelReputation(int level) {
        if (level >= AllianceConst.Cfg.MAX_LEVEL || level >= LEVEL_REPUTATION.length) {
            return -1;
        }
        return LEVEL_REPUTATION[level];
    }

    /**
     * 等级对应的人数上限 (1级20人, 每级+10)
     */
    public int memberCap(int level) {
        int lv = Math.max(1, Math.min(level, AllianceConst.Cfg.MAX_LEVEL));
        return AllianceConst.Cfg.BASE_MEMBER_LIMIT + (lv - 1) * AllianceConst.Cfg.MEMBER_LIMIT_PER_LEVEL;
    }

    // =====================================================================
    // 任务表
    // =====================================================================

    /**
     * 联盟循环任务配置
     *
     * @param cfgId              配置 id
     * @param quality            品质 (AllianceConst.TaskQuality)
     * @param goalType           目标类型 (AllianceConst.TaskGoalType)
     * @param goalParam          目标参数 (EARN_GOLD=gameType(0不限) / WIN_TIMES=最低倍数)
     * @param goalCount          目标数量
     * @param durationMs         任务持续时间(ms)
     * @param weight             刷新权重
     * @param rewardContribution 完成奖励: 贡献值
     * @param rewardReputation   完成奖励: 联盟声誉值
     * @param maxHelp            可被盟友帮助的次数上限 (帮助计入进度; 0=不可求助)
     */
    public record TaskCfg(int cfgId, int quality, int goalType, long goalParam, long goalCount,
                          long durationMs, int weight, long rewardContribution, long rewardReputation, int maxHelp) {
    }

    private static final long HOUR = 3600_000L;
    //开发期示例任务表 (对应需求示例: 赚金币/N倍中奖/消耗体力); maxHelp=1: 任务求助只能由一名盟友帮助一次
    private static final List<TaskCfg> TASKS = List.of(
            new TaskCfg(1001, AllianceConst.TaskQuality.LOW, AllianceConst.TaskGoalType.EARN_GOLD, 0, 1_000_000, 24 * HOUR, 100, 10, 10, 1),
            new TaskCfg(1002, AllianceConst.TaskQuality.LOW, AllianceConst.TaskGoalType.COST_POWER, 0, 50, 24 * HOUR, 100, 10, 10, 1),
            new TaskCfg(1003, AllianceConst.TaskQuality.MID, AllianceConst.TaskGoalType.WIN_TIMES, 10, 3, 24 * HOUR, 60, 25, 25, 1),
            new TaskCfg(1004, AllianceConst.TaskQuality.MID, AllianceConst.TaskGoalType.EARN_GOLD, 0, 5_000_000, 24 * HOUR, 60, 25, 25, 1),
            new TaskCfg(1005, AllianceConst.TaskQuality.HIGH, AllianceConst.TaskGoalType.WIN_TIMES, 50, 1, 24 * HOUR, 30, 60, 60, 1),
            new TaskCfg(1006, AllianceConst.TaskQuality.HIGH, AllianceConst.TaskGoalType.COST_POWER, 0, 500, 24 * HOUR, 30, 60, 60, 1));

    public TaskCfg taskCfg(int cfgId) {
        for (TaskCfg cfg : TASKS) {
            if (cfg.cfgId() == cfgId) {
                return cfg;
            }
        }
        return null;
    }

    /**
     * 按权重随机抽 n 条任务配置 (任务可重复出现 —— 需求明确)
     */
    public List<TaskCfg> randomTasks(int n) {
        if (n <= 0 || TASKS.isEmpty()) {
            return Collections.emptyList();
        }
        int total = 0;
        for (TaskCfg cfg : TASKS) {
            total += cfg.weight();
        }
        List<TaskCfg> result = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            int rand = RandomUtils.randomInt(total);
            int acc = 0;
            for (TaskCfg cfg : TASKS) {
                acc += cfg.weight();
                if (rand < acc) {
                    result.add(cfg);
                    break;
                }
            }
        }
        return result;
    }

    // =====================================================================
    // 商店表
    // =====================================================================

    /**
     * 联盟商店商品配置
     *
     * @param goodsId     商品 id
     * @param itemId      道具 id
     * @param count       单次兑换数量
     * @param price       价格 (贡献值)
     * @param dailyLimit  每日限购次数
     * @param unlockLevel 解锁所需联盟等级
     */
    public record ShopGoodsCfg(int goodsId, int itemId, long count, long price, int dailyLimit, int unlockLevel) {
    }

    //开发期示例商品 (金币/钻石 itemId 需运行时从道具表解析, 懒构建; 上线前以 Excel 商店表为准)
    private volatile List<ShopGoodsCfg> shopGoods;

    public List<ShopGoodsCfg> shopGoods() {
        List<ShopGoodsCfg> goods = this.shopGoods;
        if (goods == null) {
            int gold = com.jjg.game.core.utils.ItemUtils.getGoldItemId();
            int diamond = com.jjg.game.core.utils.ItemUtils.getDiamondItemId();
            goods = List.of(
                    new ShopGoodsCfg(1, gold, 100_000, 10, 5, 1),
                    new ShopGoodsCfg(2, gold, 1_000_000, 80, 3, 1),
                    new ShopGoodsCfg(3, diamond, 10, 50, 3, 3),
                    new ShopGoodsCfg(4, diamond, 100, 400, 1, 5));
            this.shopGoods = goods;
        }
        return goods;
    }

    public ShopGoodsCfg shopGoods(int goodsId) {
        for (ShopGoodsCfg cfg : shopGoods()) {
            if (cfg.goodsId() == goodsId) {
                return cfg;
            }
        }
        return null;
    }

    // =====================================================================
    // 捐献表
    // =====================================================================

    /**
     * 捐献档位配置
     *
     * @param donateId           档位 id
     * @param costItemId         消耗道具 (0=免费)
     * @param costCount          消耗数量
     * @param rewardContribution 奖励贡献值
     * @param rewardReputation   给联盟的声誉值
     * @param firstFree          当日首次是否免费
     */
    public record DonateCfg(int donateId, int costItemId, long costCount,
                            long rewardContribution, long rewardReputation, boolean firstFree) {
    }

    //开发期示例: 单档钻石捐献, 当日首次免费 (钻石 itemId 运行时解析, 懒构建)
    private volatile List<DonateCfg> donates;

    public List<DonateCfg> donateCfgs() {
        List<DonateCfg> cfgs = this.donates;
        if (cfgs == null) {
            cfgs = List.of(new DonateCfg(1, com.jjg.game.core.utils.ItemUtils.getDiamondItemId(), 20, 50, 100, true));
            this.donates = cfgs;
        }
        return cfgs;
    }

    public DonateCfg donateCfg(int donateId) {
        for (DonateCfg cfg : donateCfgs()) {
            if (cfg.donateId() == donateId) {
                return cfg;
            }
        }
        return null;
    }

    // =====================================================================
    // 对决: 阶段时间 (按 ISO 周推算, 创建期文档时固化)
    // =====================================================================

    /**
     * 期号: ISO 年+周, 如 2026W24
     */
    public String battlePeriod(long now) {
        LocalDate date = LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(now), ZONE).toLocalDate();
        WeekFields wf = WeekFields.ISO;
        return date.get(wf.weekBasedYear()) + "W" + String.format("%02d", date.get(wf.weekOfWeekBasedYear()));
    }

    /**
     * 本周指定 星期/时分 的毫秒时间戳
     */
    public long timeOfWeek(long now, DayOfWeek day, int hour, int minute) {
        LocalDateTime base = LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(now), ZONE);
        LocalDate monday = base.toLocalDate().with(DayOfWeek.MONDAY);
        return monday.plusDays(day.getValue() - 1L).atTime(hour, minute)
                .atZone(ZONE).toInstant().toEpochMilli();
    }

    //报名: 周三 9:00 - 21:00
    public long battleSignupStart(long now) {
        return timeOfWeek(now, DayOfWeek.WEDNESDAY, 9, 0);
    }

    public long battleSignupEnd(long now) {
        return timeOfWeek(now, DayOfWeek.WEDNESDAY, 21, 0);
    }

    //匹配: 周四 10:00 执行 (需求窗口 10-12 点, 匹配完成也要等到 12 点统一开战)
    public long battleMatchTime(long now) {
        return timeOfWeek(now, DayOfWeek.THURSDAY, 10, 0);
    }

    //对决: 周四 12:00 - 周日 12:00
    public long battleFightStart(long now) {
        return timeOfWeek(now, DayOfWeek.THURSDAY, 12, 0);
    }

    public long battleFightEnd(long now) {
        return timeOfWeek(now, DayOfWeek.SUNDAY, 12, 0);
    }

    // =====================================================================
    // 对决: 奖励
    // =====================================================================

    /**
     * 胜利奖励 (平局按胜利发) itemId -> count; 开发期占位: 金币
     */
    public Map<Integer, Long> battleWinRewards() {
        return Map.of(com.jjg.game.core.utils.ItemUtils.getGoldItemId(), 1_000_000L);
    }

    /**
     * 失败安慰奖
     */
    public Map<Integer, Long> battleLoseRewards() {
        return Map.of(com.jjg.game.core.utils.ItemUtils.getGoldItemId(), 200_000L);
    }

    /**
     * 对决阶段奖励 (个人): 个人比赛积分达到 scoreThreshold 可领取
     *
     * @param stage   阶段序号 (从 0 开始, 对应玩家领取位图的 bit 位)
     * @param rewards itemId -> count
     */
    public record BattleStageCfg(int stage, long scoreThreshold, Map<Integer, Long> rewards) {
    }

    private volatile List<BattleStageCfg> battleStages;

    public List<BattleStageCfg> battleStages() {
        List<BattleStageCfg> stages = this.battleStages;
        if (stages == null) {
            int gold = com.jjg.game.core.utils.ItemUtils.getGoldItemId();
            int diamond = com.jjg.game.core.utils.ItemUtils.getDiamondItemId();
            stages = List.of(
                    new BattleStageCfg(0, 10, Map.of(gold, 50_000L)),
                    new BattleStageCfg(1, 50, Map.of(gold, 200_000L)),
                    new BattleStageCfg(2, 200, Map.of(gold, 500_000L, diamond, 10L)));
            this.battleStages = stages;
        }
        return stages;
    }

    // =====================================================================
    // 排行榜奖励
    // =====================================================================

    /**
     * 贡献度周榜奖励 (盟内, 每周结算): 按名次区间取
     */
    public Map<Integer, Long> contribRankRewards(int rank) {
        int gold = com.jjg.game.core.utils.ItemUtils.getGoldItemId();
        int diamond = com.jjg.game.core.utils.ItemUtils.getDiamondItemId();
        //开发期占位: 1-3 名 / 4-10 名 / 11-50 名
        if (rank <= 3) {
            return Map.of(gold, 500_000L, diamond, 50L);
        }
        if (rank <= 10) {
            return Map.of(gold, 200_000L, diamond, 20L);
        }
        if (rank <= AllianceConst.Cfg.CONTRIB_RANK_SHOW) {
            return Map.of(gold, 50_000L);
        }
        return Map.of();
    }

    /**
     * 联盟赛季榜奖励 (全服, 每自然月结算, 发给联盟全体成员)
     */
    public Map<Integer, Long> seasonRankRewards(int rank) {
        int gold = com.jjg.game.core.utils.ItemUtils.getGoldItemId();
        int diamond = com.jjg.game.core.utils.ItemUtils.getDiamondItemId();
        //开发期占位: 第 1 名 / 前 10 名
        if (rank == 1) {
            return Map.of(gold, 2_000_000L, diamond, 200L);
        }
        if (rank <= AllianceConst.Cfg.ALLIANCE_RANK_SHOW) {
            return Map.of(gold, 500_000L, diamond, 50L);
        }
        return Map.of();
    }
}
