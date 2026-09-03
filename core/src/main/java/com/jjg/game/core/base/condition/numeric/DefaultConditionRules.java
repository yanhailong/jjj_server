package com.jjg.game.core.base.condition.numeric;

import com.jjg.game.common.constant.CoreConst;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.ToLongBiFunction;

/**
 * condition.xlsx 的内置规则定义。
 * <p>
 * 12401-12408 是经营加成效果而非达成条件，且将从 condition 表移除，因此刻意不注册。
 * 表内说明文字出现错误 id 时，以规则 id（即 Excel 的 id 列）为准。
 */
final class DefaultConditionRules {
    private static final int FIXED_TARGET_ONE = -1;
    //Slots 节点上报的 WealthGodConstant.SpecialMode.WEALTH_COM 模式事实值。
    private static final int WEALTH_GOD_COM_MODE = 3;

    private DefaultConditionRules() {
    }

    static List<ConditionRule<?>> rules() {
        List<ConditionRule<?>> rules = new ArrayList<>();
        addPlayerStateRules(rules);
        addBaseGameRules(rules);
        addRechargeRules(rules);
        addEffectiveBetRules(rules);
        addSimulationRules(rules);
        addAllianceRules(rules);
        addCoopRules(rules);
        addSeasonRules(rules);
        addMiningRules(rules);
        return List.copyOf(rules);
    }

    private static void addPlayerStateRules(List<ConditionRule<?>> rules) {
        rules.add(state(1, 1, 0, StateConditionEvent.Type.PLAYER_LEVEL));
        rules.add(state(2, 1, 0, StateConditionEvent.Type.OPEN_SERVER_DAYS));
        rules.add(state(3, 1, 0, StateConditionEvent.Type.VIP_LEVEL));
        rules.add(state(4, 1, 0, StateConditionEvent.Type.PHONE_BOUND));
        rules.add(rule(5, StateConditionEvent.class, 3, 3, 1, ProgressMode.SET,
                (s, e) -> e.type() == StateConditionEvent.Type.REMAINING_ATTEMPTS
                        && optional(s.parameter(0), e.subjectId())
                        && (s.parameter(2) <= 0 || s.parameter(2) == e.secondaryValue()),
                (s, e) -> e.value(), nonNegativeParameters()));
    }

    private static void addBaseGameRules(List<ConditionRule<?>> rules) {
        rules.add(game(10001, 3, 3, 2, ProgressMode.ADD,
                (s, e) -> e.matchesGame(s.parameter(0)) && e.bet() >= s.parameter(1),
                (s, e) -> 1));
        rules.add(game(10002, 3, 3, 2, ProgressMode.ADD,
                (s, e) -> e.matchesGame(s.parameter(0)) && e.bet() >= s.parameter(1),
                (s, e) -> 1));
        //10003 总共 5 个数值（含 id），因此这里只允许 4 个参数。
        rules.add(game(10003, 4, 4, 2, ProgressMode.ADD,
                (s, e) -> e.matchesGame(s.parameter(0)) && optional(s.parameter(3), e.winItemId()),
                (s, e) -> e.bet() >= s.parameter(1) ? e.winOf(s.intParameter(3)) : 0));
    }

    private static void addRechargeRules(List<ConditionRule<?>> rules) {
        rules.add(rule(11001, RechargeConditionEvent.class, 3, 3, 1, ProgressMode.ADD,
                (s, e) -> e.amount() >= s.parameter(0) && e.matchesChannel(s.parameter(2)),
                (s, e) -> 1, nonNegativeParameters()));
        //11002 的唯一标准顺序：渠道_金额。
        rules.add(rule(11002, RechargeConditionEvent.class, 2, 2, 1, ProgressMode.ADD,
                (s, e) -> e.matchesChannel(s.parameter(0)),
                (s, e) -> Math.max(0, e.amount()), nonNegativeParameters()));
        //每日签到等旧业务用 11003_0_0 表示无需充值、立即满足；这是合法业务哨兵，不是错误目标。
        rules.add(ruleAllowingZeroTarget(11003, RechargeConditionEvent.class, 2, 2, 0, ProgressMode.ADD,
                (s, e) -> e.matchesChannel(s.parameter(1)),
                (s, e) -> Math.max(0, e.amount()), nonNegativeParameters()));
        rules.add(rule(11004, RechargeConditionEvent.class, 2, 2, 0, ProgressMode.ADD,
                (s, e) -> e.matchesChannel(s.parameter(1)),
                (s, e) -> Math.max(0, e.amount()), nonNegativeParameters()));
        rules.add(rule(11005, RechargeConditionEvent.class, 1, 1, 0, ProgressMode.ADD,
                (s, e) -> true, (s, e) -> Math.max(0, e.amount()), nonNegativeParameters()));
    }

    private static void addEffectiveBetRules(List<ConditionRule<?>> rules) {
        rules.add(game(12001, 2, 2, 1, ProgressMode.ADD,
                (s, e) -> e.matchesGame(s.parameter(0)), (s, e) -> Math.max(0, e.bet())));
        rules.add(game(12002, 1, 1, 0, ProgressMode.ADD,
                (s, e) -> e.roomType() < 10, (s, e) -> Math.max(0, e.bet())));
        rules.add(game(12003, 2, Integer.MAX_VALUE, 0, ProgressMode.ADD,
                (s, e) -> wildcardOrContains(s, 1, e.gameId()), (s, e) -> Math.max(0, e.bet())));
        rules.add(game(12004, 2, Integer.MAX_VALUE, 0, ProgressMode.ADD,
                (s, e) -> wildcardAt(s, 1) || !contains(s, 1, e.gameId()),
                (s, e) -> Math.max(0, e.bet())));
        rules.add(game(12005, 2, Integer.MAX_VALUE, 0, ProgressMode.ADD,
                (s, e) -> wildcardOrContains(s, 1, e.gameType()), (s, e) -> Math.max(0, e.bet())));
        rules.add(game(12006, 2, Integer.MAX_VALUE, 0, ProgressMode.ADD,
                (s, e) -> wildcardOrContains(s, 1, e.roomType()), (s, e) -> Math.max(0, e.bet())));
        rules.add(game(12007, 1, 1, 0, ProgressMode.ADD,
                (s, e) -> e.roomType() < 10, (s, e) -> Math.max(0, e.bet())));
        rules.add(action(12101, 2, 2, 1, ProgressMode.ADD, ActionConditionEvent.Type.ITEM_USE,
                (s, e) -> e.matchesSubject(s.parameter(0)), (s, e) -> Math.max(0, e.count())));
    }

    private static void addSimulationRules(List<ConditionRule<?>> rules) {
        rules.add(game(12201, 2, 2, 1, ProgressMode.MAX,
                (s, e) -> e.matchesGame(s.parameter(0)), (s, e) -> Math.max(0, e.win())));
        rules.add(game(12202, 3, 3, 2, ProgressMode.ADD,
                (s, e) -> e.matchesGame(s.parameter(0)), (s, e) -> e.itemGain(s.intParameter(1))));
        rules.add(game(12203, 2, 2, 1, ProgressMode.ADD,
                (s, e) -> optional(s.parameter(0), e.awardType()), (s, e) -> 1));
        //奖池按档位独立计数: 同一次旋转命中多档/同档多次都如实推进 (参数0 <=0 时不限档位)
        rules.add(game(12204, 2, 2, 1, ProgressMode.ADD,
                (s, e) -> e.jackpotCount(s.intParameter(0)) > 0,
                (s, e) -> e.jackpotCount(s.intParameter(0))));
        rules.add(game(12205, 1, 1, 0, ProgressMode.ADD,
                (s, e) -> e.freeGameTriggers() > 0, (s, e) -> e.freeGameTriggers()));
        rules.add(action(12206, 2, 2, 1, ProgressMode.ADD, ActionConditionEvent.Type.BUILDING_UPGRADE,
                (s, e) -> e.matchesSubject(s.parameter(0)), (s, e) -> positiveCount(e)));
        rules.add(action(12207, 2, 2, 1, ProgressMode.MAX, ActionConditionEvent.Type.BUILDING_LEVEL,
                (s, e) -> e.matchesSubject(s.parameter(0)), (s, e) -> e.value()));
        rules.add(action(12208, 2, 2, 0, ProgressMode.SET, ActionConditionEvent.Type.BUILDING_COUNT,
                (s, e) -> e.qualifier() >= s.parameter(1), (s, e) -> e.value()));
        rules.add(action(12209, 1, 1, 0, ProgressMode.ADD, ActionConditionEvent.Type.AD_WATCH,
                (s, e) -> true, (s, e) -> positiveCount(e)));
        rules.add(action(12210, 2, 2, 1, ProgressMode.ADD, ActionConditionEvent.Type.AD_REWARD,
                (s, e) -> e.matchesSubject(s.parameter(0)), (s, e) -> e.value()));
        rules.add(action(12211, 2, 2, 1, ProgressMode.ADD, ActionConditionEvent.Type.EMPLOYEE_RECRUIT,
                (s, e) -> e.matchesSubject(s.parameter(0)), (s, e) -> positiveCount(e)));
        rules.add(action(12212, 3, 3, 1, ProgressMode.SET, ActionConditionEvent.Type.EMPLOYEE_COUNT,
                (s, e) -> e.matchesSubject(s.parameter(0)) && e.qualifier() >= s.parameter(2),
                (s, e) -> e.value()));
        rules.add(action(12213, 2, 2, 1, ProgressMode.ADD, ActionConditionEvent.Type.GUEST_RECRUIT,
                (s, e) -> (s.parameter(0) > 0) == e.paid(), (s, e) -> positiveCount(e)));
        rules.add(action(12214, 2, 2, 0, ProgressMode.SET, ActionConditionEvent.Type.GUEST_COUNT,
                (s, e) -> e.qualifier() >= s.parameter(1), (s, e) -> e.value()));
        rules.add(action(12215, 2, 2, 1, ProgressMode.ADD, ActionConditionEvent.Type.PRODUCTION_INCOME,
                (s, e) -> e.matchesSubject(s.parameter(0)), (s, e) -> e.value()));
        //12216 是建筑解锁的游戏总数, 事件携带当前总数, SET 覆盖;
        //技能研究次数是 12304, 两者语义不同, 不能共用 GAME_RESEARCH 事件。
        rules.add(action(12216, 1, 1, 0, ProgressMode.SET, ActionConditionEvent.Type.GAME_UNLOCK,
                (s, e) -> true, (s, e) -> e.value()));
        rules.add(action(12217, 1, 1, 0, ProgressMode.ADD, ActionConditionEvent.Type.VISIT,
                (s, e) -> true, (s, e) -> positiveCount(e)));
        rules.add(action(12218, 1, 1, 0, ProgressMode.ADD, ActionConditionEvent.Type.LOGIN,
                (s, e) -> true, (s, e) -> positiveCount(e)));
        rules.add(action(12219, 1, 1, 0, ProgressMode.SET, ActionConditionEvent.Type.CASINO_UNLOCK,
                (s, e) -> true, (s, e) -> e.value()));
        rules.add(action(12220, 2, 2, 1, ProgressMode.ADD, ActionConditionEvent.Type.ITEM_CONSUME,
                (s, e) -> e.matchesSubject(s.parameter(0)), (s, e) -> e.value()));
        rules.add(game(12221, 2, 2, 1, ProgressMode.ADD,
                (s, e) -> e.matchesGame(s.parameter(0)), (s, e) -> 1));
        rules.add(rule(12222, GuestInviteConditionEvent.class, 2, 2, 1, ProgressMode.ADD,
                (s, e) -> e.matchesGuest(s.parameter(0)), (s, e) -> 1, nonNegativeParameters()));
        rules.add(action(12223, 1, 1, 0, ProgressMode.ADD, ActionConditionEvent.Type.DOUXIAN_SETTLEMENT,
                (s, e) -> true, (s, e) -> positiveCount(e)));
        rules.add(action(12224, 1, 1, 0, ProgressMode.ADD, ActionConditionEvent.Type.ACHIEVEMENT_REWARD,
                (s, e) -> true, (s, e) -> positiveCount(e)));
        rules.add(rule(12225, GameWinEvent.class, 3, 3, 2, ProgressMode.ADD,
                (s, e) -> e.matchesGame(s.parameter(0)) && e.winItemId() == s.parameter(1),
                (s, e) -> Math.max(0, e.win()), nonNegativeParameters()));
        rules.add(action(12226, 1, 1, 0, ProgressMode.ADD, ActionConditionEvent.Type.SEASON_SHOP_BUY,
                (s, e) -> true, (s, e) -> positiveCount(e)));
        rules.add(action(12227, 1, 1, 0, ProgressMode.ADD, ActionConditionEvent.Type.SEASON_GEM_EQUIP,
                (s, e) -> true, (s, e) -> positiveCount(e)));

        //12251-12283 与上面的接取型条件判定口径一致，进度由玩家统计提供而非任务计数器。
        rules.add(game(12251, 3, 3, 2, ProgressMode.ADD,
                (s, e) -> e.matchesGame(s.parameter(0)), (s, e) -> e.itemGain(s.intParameter(1))));
        rules.add(game(12252, 3, 3, 2, ProgressMode.ADD,
                (s, e) -> e.matchesGame(s.parameter(0)) && optional(s.parameter(1), e.awardType()),
                (s, e) -> 1));
        rules.add(game(12253, 3, 3, 2, ProgressMode.ADD,
                (s, e) -> e.matchesGame(s.parameter(0)) && e.jackpotCount(s.intParameter(1)) > 0,
                (s, e) -> e.jackpotCount(s.intParameter(1))));
        rules.add(game(12254, 2, 2, 1, ProgressMode.ADD,
                (s, e) -> e.matchesGame(s.parameter(0)) && e.freeGameTriggers() > 0,
                (s, e) -> e.freeGameTriggers()));
        rules.add(action(12255, 2, 2, 1, ProgressMode.ADD, ActionConditionEvent.Type.BUILDING_UPGRADE,
                (s, e) -> e.matchesSubject(s.parameter(0)), (s, e) -> positiveCount(e)));
        rules.add(action(12256, 2, 2, 0, ProgressMode.SET, ActionConditionEvent.Type.BUILDING_COUNT,
                (s, e) -> e.qualifier() >= s.parameter(1), (s, e) -> e.value()));
        rules.add(action(12257, 1, 1, 0, ProgressMode.ADD, ActionConditionEvent.Type.AD_WATCH,
                (s, e) -> true, (s, e) -> positiveCount(e)));
        //12258 已删除，不注册。
        rules.add(action(12259, 3, 3, 1, ProgressMode.SET, ActionConditionEvent.Type.EMPLOYEE_COUNT,
                (s, e) -> e.matchesSubject(s.parameter(0)) && e.qualifier() >= s.parameter(2),
                (s, e) -> e.value()));
        rules.add(action(12260, 2, 2, 1, ProgressMode.ADD, ActionConditionEvent.Type.GUEST_RECRUIT,
                (s, e) -> (s.parameter(0) > 0) == e.paid(), (s, e) -> positiveCount(e)));
        rules.add(action(12261, 2, 2, 0, ProgressMode.SET, ActionConditionEvent.Type.GUEST_COUNT,
                (s, e) -> e.qualifier() >= s.parameter(1), (s, e) -> e.value()));
        rules.add(action(12262, 2, 2, 1, ProgressMode.ADD, ActionConditionEvent.Type.PRODUCTION_INCOME,
                (s, e) -> e.matchesSubject(s.parameter(0)), (s, e) -> e.value()));
        rules.add(action(12263, 1, 1, 0, ProgressMode.SET, ActionConditionEvent.Type.GAME_UNLOCK,
                (s, e) -> true, (s, e) -> e.value()));
        rules.add(action(12264, 1, 1, 0, ProgressMode.ADD, ActionConditionEvent.Type.VISIT,
                (s, e) -> true, (s, e) -> positiveCount(e)));
        rules.add(action(12265, 1, 1, 0, ProgressMode.ADD, ActionConditionEvent.Type.LOGIN,
                (s, e) -> true, (s, e) -> positiveCount(e)));
        rules.add(action(12266, 1, 1, 0, ProgressMode.SET, ActionConditionEvent.Type.CASINO_UNLOCK,
                (s, e) -> true, (s, e) -> e.value()));
        rules.add(action(12267, 2, 2, 1, ProgressMode.ADD, ActionConditionEvent.Type.ITEM_CONSUME,
                (s, e) -> e.matchesSubject(s.parameter(0)), (s, e) -> e.value()));
        rules.add(action(12268, 2, 2, 1, ProgressMode.SET, ActionConditionEvent.Type.SCENE_TOTAL_LEVEL,
                (s, e) -> e.matchesSubject(s.parameter(0)), (s, e) -> e.value()));
        rules.add(action(12269, 2, 2, 1, ProgressMode.SET, ActionConditionEvent.Type.GAME_RESEARCH,
                (s, e) -> e.matchesSubject(s.parameter(0)), (s, e) -> e.value()));
        rules.add(action(12270, 1, 1, 0, ProgressMode.ADD, ActionConditionEvent.Type.GUEST_POOL_DRAW,
                (s, e) -> true, (s, e) -> positiveCount(e)));
        rules.add(action(12271, 1, 1, 0, ProgressMode.ADD, ActionConditionEvent.Type.EMPLOYEE_POOL_DRAW,
                (s, e) -> true, (s, e) -> positiveCount(e)));
        rules.add(game(12272, 2, 2, 1, ProgressMode.ADD,
                (s, e) -> e.matchesGame(s.parameter(0)), (s, e) -> 1));
        rules.add(action(12273, 2, 2, FIXED_TARGET_ONE, ProgressMode.SET,
                ActionConditionEvent.Type.BUILDING_LEVEL,
                (s, e) -> e.subjectId() == s.parameter(1), (s, e) -> 1));
        rules.add(game(12274, 3, 3, 2, ProgressMode.ADD,
                (s, e) -> e.matchesGame(s.parameter(0)) && e.winItemId() == s.parameter(1),
                (s, e) -> Math.max(0, e.win())));
        rules.add(game(12275, 1, 1, 0, ProgressMode.ADD,
                (s, e) -> e.gameType() == CoreConst.GameType.WEALTH_GOD
                        && e.containsMode(WEALTH_GOD_COM_MODE),
                (s, e) -> 1));
        rules.add(action(12276, 2, 2, 1, ProgressMode.SET, ActionConditionEvent.Type.GAME_RESEARCH,
                (s, e) -> e.matchesSubject(s.parameter(0)), (s, e) -> e.value()));
        rules.add(action(12277, 3, 3, 2, ProgressMode.SET, ActionConditionEvent.Type.GAME_RESEARCH,
                (s, e) -> e.subjectId() == s.parameter(0) && e.relatedId() == s.parameter(1),
                (s, e) -> e.value()));
        rules.add(action(12278, 1, 1, 0, ProgressMode.ADD, ActionConditionEvent.Type.ALLIANCE_DONATE,
                (s, e) -> true, (s, e) -> positiveCount(e)));
        rules.add(action(12279, 1, 1, 0, ProgressMode.ADD, ActionConditionEvent.Type.VISIT_GIFT,
                (s, e) -> true, (s, e) -> positiveCount(e)));
        rules.add(action(12280, 2, 2, 1, ProgressMode.ADD, ActionConditionEvent.Type.VISIT_SLOT_SPIN,
                (s, e) -> e.matchesSubject(s.parameter(0)), (s, e) -> positiveCount(e)));
        rules.add(action(12281, 1, 1, 0, ProgressMode.ADD, ActionConditionEvent.Type.DOUXIAN_SETTLEMENT,
                (s, e) -> true, (s, e) -> positiveCount(e)));
        rules.add(action(12282, 2, 2, 1, ProgressMode.ADD, ActionConditionEvent.Type.SEASON_GEM_CRAFT,
                (s, e) -> e.matchesSubject(s.parameter(0)), (s, e) -> positiveCount(e)));
        rules.add(action(12283, 1, 1, 0, ProgressMode.ADD, ActionConditionEvent.Type.CARD_POOL_DRAW,
                (s, e) -> true, (s, e) -> positiveCount(e)));
    }

    private static void addAllianceRules(List<ConditionRule<?>> rules) {
        rules.add(game(12301, 4, 4, 3, ProgressMode.ADD,
                (s, e) -> e.matchesGame(s.parameter(0)) && e.bet() >= s.parameter(1)
                        && e.multiple() >= s.parameter(2), (s, e) -> 1));
        rules.add(game(12302, 3, 3, 2, ProgressMode.ADD,
                (s, e) -> e.matchesGame(s.parameter(0)) && e.bet() >= s.parameter(1) && e.energyConsumed(),
                (s, e) -> 1));
        rules.add(action(12303, 2, 2, 1, ProgressMode.ADD, ActionConditionEvent.Type.CARD_POOL_DRAW,
                (s, e) -> e.matchesSubject(s.parameter(0)), (s, e) -> positiveCount(e)));
        rules.add(action(12304, 2, 2, 1, ProgressMode.ADD, ActionConditionEvent.Type.GAME_RESEARCH,
                (s, e) -> e.matchesSubject(s.parameter(0)), (s, e) -> positiveCount(e)));
        rules.add(action(12305, 1, 1, 0, ProgressMode.ADD, ActionConditionEvent.Type.ALLIANCE_DONATE,
                (s, e) -> true, (s, e) -> positiveCount(e)));
        rules.add(game(12306, 4, 4, 3, ProgressMode.ADD,
                (s, e) -> e.matchesGame(s.parameter(0)) && e.bet() >= s.parameter(1)
                        && e.energyConsumed() && optional(s.parameter(2), e.winItemId()),
                (s, e) -> e.winOf(s.intParameter(2))));
        rules.add(rule(12307, RechargeConditionEvent.class, 2, 2, 1, ProgressMode.ADD,
                (s, e) -> e.matchesChannel(s.parameter(0)), (s, e) -> e.amount(), nonNegativeParameters()));
    }

    private static void addCoopRules(List<ConditionRule<?>> rules) {
        rules.add(game(12501, 4, 4, 3, ProgressMode.ADD,
                (s, e) -> e.matchesGame(s.parameter(0)) && e.energyConsumed() && e.normalSpin()
                        && e.containsMode(s.intParameter(2)), (s, e) -> 1));
        rules.add(game(12502, 3, 3, 2, ProgressMode.ADD,
                (s, e) -> e.matchesGame(s.parameter(0)) && e.energyConsumed(),
                (s, e) -> Math.max(0, e.win())));
        rules.add(game(12503, 4, 4, 3, ProgressMode.ADD,
                (s, e) -> e.matchesGame(s.parameter(0)) && e.energyConsumed()
                        && e.multiple() >= s.parameter(2), (s, e) -> 1));
        rules.add(game(12504, 3, 3, 2, ProgressMode.ADD,
                (s, e) -> e.matchesGame(s.parameter(0)) && e.energyConsumed(),
                (s, e) -> Math.max(0, e.bet())));
    }

    private static void addSeasonRules(List<ConditionRule<?>> rules) {
        rules.add(game(12601, 3, 3, 2, ProgressMode.ADD,
                (s, e) -> e.matchesGame(s.parameter(0)), (s, e) -> Math.max(0, e.win())));
        rules.add(game(12602, 4, 4, 3, ProgressMode.ADD,
                (s, e) -> e.matchesGame(s.parameter(0)) && e.containsMode(s.intParameter(2)),
                (s, e) -> 1));
        rules.add(game(12603, 4, 4, 3, ProgressMode.ADD,
                (s, e) -> e.matchesGame(s.parameter(0)) && e.multiple() >= s.parameter(2),
                (s, e) -> 1));
        rules.add(game(12604, 3, 3, 2, ProgressMode.ADD,
                (s, e) -> e.matchesGame(s.parameter(0)) && e.win() > 0, (s, e) -> 1));
        rules.add(game(12605, 4, 4, 3, ProgressMode.ADD,
                (s, e) -> e.matchesGame(s.parameter(0)), (s, e) -> e.iconCount(s.intParameter(2))));
        rules.add(game(12606, 3, 3, 2, ProgressMode.MAX,
                (s, e) -> e.matchesGame(s.parameter(0)), (s, e) -> Math.max(0, e.win())));
        rules.add(action(12607, 2, 2, 1, ProgressMode.ADD, ActionConditionEvent.Type.COMPETITIVE_MATCH,
                (s, e) -> e.matchesSubject(s.parameter(0)), (s, e) -> positiveCount(e)));
        rules.add(action(12608, 2, 2, 1, ProgressMode.ADD, ActionConditionEvent.Type.SEASON_GEM_DROP,
                (s, e) -> e.matchesSubject(s.parameter(0)), (s, e) -> positiveCount(e)));
        rules.add(game(12609, 4, 4, 3, ProgressMode.ADD,
                (s, e) -> e.matchesGame(s.parameter(0)) && e.bet() >= s.parameter(1)
                        && optional(s.parameter(2), e.winItemId()),
                (s, e) -> e.winOf(s.intParameter(2))));
        rules.add(game(12610, 4, 4, 3, ProgressMode.ADD,
                (s, e) -> e.matchesGame(s.parameter(0)) && e.bet() >= s.parameter(1)
                        && optional(s.parameter(2), e.betItemId()), (s, e) -> 1));
    }

    private static void addMiningRules(List<ConditionRule<?>> rules) {
        rules.add(action(12701, 2, 2, 1, ProgressMode.ADD, ActionConditionEvent.Type.GRID_MINED,
                (s, e) -> e.matchesSubject(s.parameter(0)), (s, e) -> positiveCount(e)));
        rules.add(action(12702, 2, 2, 1, ProgressMode.MAX, ActionConditionEvent.Type.DEPTH_REACHED,
                (s, e) -> e.matchesSubject(s.parameter(0)), (s, e) -> e.value()));
        rules.add(action(12703, 3, 3, 2, ProgressMode.ADD, ActionConditionEvent.Type.ITEM_USE,
                (s, e) -> e.matchesSubject(s.parameter(0)) && e.matchesRelated(s.parameter(1)),
                (s, e) -> positiveCount(e)));
        rules.add(action(12704, 3, 3, 2, ProgressMode.ADD, ActionConditionEvent.Type.ITEM_EXCHANGE,
                (s, e) -> e.matchesSubject(s.parameter(0)) && e.matchesRelated(s.parameter(1)),
                (s, e) -> positiveCount(e)));
    }

    private static ConditionRule<StateConditionEvent> state(int id, int parameterCount, int targetIndex,
                                                             StateConditionEvent.Type type) {
        return rule(id, StateConditionEvent.class, parameterCount, parameterCount, targetIndex, ProgressMode.SET,
                (s, e) -> e.type() == type, (s, e) -> e.value(), nonNegativeParameters());
    }

    private static ConditionRule<GameConditionEvent> game(int id, int minParameters, int maxParameters,
                                                           int targetIndex, ProgressMode mode,
                                                           BiPredicate<ConditionSpec, GameConditionEvent> predicate,
                                                           ToLongBiFunction<ConditionSpec, GameConditionEvent> value) {
        return rule(id, GameConditionEvent.class, minParameters, maxParameters, targetIndex, mode,
                predicate, value, nonNegativeParameters());
    }

    private static ConditionRule<ActionConditionEvent> action(int id, int minParameters, int maxParameters,
                                                               int targetIndex, ProgressMode mode,
                                                               ActionConditionEvent.Type type,
                                                               BiPredicate<ConditionSpec, ActionConditionEvent> predicate,
                                                               ToLongBiFunction<ConditionSpec, ActionConditionEvent> value) {
        return rule(id, ActionConditionEvent.class, minParameters, maxParameters, targetIndex, mode,
                (s, e) -> e.type() == type && predicate.test(s, e), value, nonNegativeParameters());
    }

    private static <E extends ConditionEvent> ConditionRule<E> rule(
            int id, Class<E> eventType, int minParameters, int maxParameters, int targetIndex,
            ProgressMode mode, BiPredicate<ConditionSpec, E> predicate,
            ToLongBiFunction<ConditionSpec, E> value, Consumer<ConditionSpec> extraValidator) {
        return new SimpleRule<>(id, eventType, minParameters, maxParameters, targetIndex,
                mode, predicate, value, extraValidator, false);
    }

    private static <E extends ConditionEvent> ConditionRule<E> ruleAllowingZeroTarget(
            int id, Class<E> eventType, int minParameters, int maxParameters, int targetIndex,
            ProgressMode mode, BiPredicate<ConditionSpec, E> predicate,
            ToLongBiFunction<ConditionSpec, E> value, Consumer<ConditionSpec> extraValidator) {
        return new SimpleRule<>(id, eventType, minParameters, maxParameters, targetIndex,
                mode, predicate, value, extraValidator, true);
    }

    private static Consumer<ConditionSpec> nonNegativeParameters() {
        return spec -> {
            for (long parameter : spec.parameters()) {
                if (parameter < 0) {
                    throw new IllegalArgumentException("condition " + spec.id() + " contains a negative parameter");
                }
            }
        };
    }

    private static boolean optional(long expected, long actual) {
        return expected <= 0 || expected == actual;
    }

    private static boolean contains(ConditionSpec spec, int start, long actual) {
        for (int i = start; i < spec.parameters().size(); i++) {
            if (spec.parameter(i) == actual) {
                return true;
            }
        }
        return false;
    }

    /** 12003-12006 的旧配置约定：过滤列表首项为 0 时表示任意范围。 */
    private static boolean wildcardAt(ConditionSpec spec, int start) {
        return spec.parameter(start) == 0;
    }

    private static boolean wildcardOrContains(ConditionSpec spec, int start, long actual) {
        return wildcardAt(spec, start) || contains(spec, start, actual);
    }

    private static long positiveCount(ActionConditionEvent event) {
        return event.count() > 0 ? event.count() : 1;
    }

    private record SimpleRule<E extends ConditionEvent>(
            int id,
            Class<E> eventType,
            int minParameters,
            int maxParameters,
            int targetIndex,
            ProgressMode mode,
            BiPredicate<ConditionSpec, E> predicate,
            ToLongBiFunction<ConditionSpec, E> valueFunction,
            Consumer<ConditionSpec> extraValidator,
            boolean zeroTargetAllowed) implements ConditionRule<E> {

        @Override
        public void validate(ConditionSpec spec) {
            if (spec.id() != id) {
                throw new IllegalArgumentException("condition rule/id mismatch: " + id + "/" + spec.id());
            }
            int size = spec.parameters().size();
            if (size < minParameters || size > maxParameters) {
                String expected = minParameters == maxParameters
                        ? String.valueOf(minParameters) : minParameters + ".." + maxParameters;
                throw new IllegalArgumentException("condition " + id + " expects " + expected
                        + " parameters but got " + size);
            }
            extraValidator.accept(spec);
            long target = target(spec);
            if (target < 0) {
                throw new IllegalArgumentException("condition " + id + " target must not be negative");
            }
            if (!zeroTargetAllowed && target == 0) {
                throw new IllegalArgumentException("condition " + id + " target must be positive");
            }
        }

        @Override
        public long target(ConditionSpec spec) {
            return targetIndex == FIXED_TARGET_ONE ? 1 : spec.parameter(targetIndex);
        }

        @Override
        public ConditionUpdate evaluate(ConditionSpec spec, E event) {
            long target = target(spec);
            if (!predicate.test(spec, event)) {
                return ConditionUpdate.ignored(target);
            }
            return ConditionUpdate.matched(mode, valueFunction.applyAsLong(spec, event), target);
        }
    }
}
