package com.jjg.game.core.base.condition.numeric;

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
    private DefaultConditionRules() {
    }

    static List<ConditionRule<?>> rules() {
        List<ConditionRule<?>> rules = new ArrayList<>(66);
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
        rules.add(rule(11003, RechargeConditionEvent.class, 2, 2, 0, ProgressMode.ADD,
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
                (s, e) -> containsAny(s, 1, e.gameId(), e.gameType()), (s, e) -> Math.max(0, e.bet())));
        rules.add(game(12004, 2, Integer.MAX_VALUE, 0, ProgressMode.ADD,
                (s, e) -> !containsAny(s, 1, e.gameId(), e.gameType()), (s, e) -> Math.max(0, e.bet())));
        rules.add(game(12005, 2, Integer.MAX_VALUE, 0, ProgressMode.ADD,
                (s, e) -> contains(s, 1, e.gameType()), (s, e) -> Math.max(0, e.bet())));
        rules.add(game(12006, 2, Integer.MAX_VALUE, 0, ProgressMode.ADD,
                (s, e) -> contains(s, 1, e.roomType()), (s, e) -> Math.max(0, e.bet())));
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
        rules.add(game(12204, 2, 2, 1, ProgressMode.ADD,
                (s, e) -> optional(s.parameter(0), e.jackpotType()), (s, e) -> 1));
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
        rules.add(action(12216, 1, 1, 0, ProgressMode.ADD, ActionConditionEvent.Type.GAME_RESEARCH,
                (s, e) -> true, (s, e) -> positiveCount(e)));
        rules.add(action(12217, 1, 1, 0, ProgressMode.ADD, ActionConditionEvent.Type.VISIT,
                (s, e) -> true, (s, e) -> positiveCount(e)));
        rules.add(action(12218, 1, 1, 0, ProgressMode.ADD, ActionConditionEvent.Type.LOGIN,
                (s, e) -> true, (s, e) -> positiveCount(e)));
        rules.add(action(12219, 1, 1, 0, ProgressMode.SET, ActionConditionEvent.Type.CASINO_UNLOCK,
                (s, e) -> true, (s, e) -> e.value()));
        rules.add(action(12220, 2, 2, 1, ProgressMode.ADD, ActionConditionEvent.Type.ITEM_CONSUME,
                (s, e) -> e.matchesSubject(s.parameter(0)), (s, e) -> e.value()));
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
        rules.add(action(12305, 2, 2, 1, ProgressMode.ADD, ActionConditionEvent.Type.ALLIANCE_DONATE,
                (s, e) -> e.value() >= s.parameter(0), (s, e) -> positiveCount(e)));
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
        rules.add(game(12608, 2, 2, 1, ProgressMode.ADD,
                (s, e) -> e.matchesGame(s.parameter(0)),
                (s, e) -> sumPositiveItemGains(e)));
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
                mode, predicate, value, extraValidator);
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

    private static boolean containsAny(ConditionSpec spec, int start, long first, long second) {
        return contains(spec, start, first) || contains(spec, start, second);
    }

    private static long positiveCount(ActionConditionEvent event) {
        return event.count() > 0 ? event.count() : 1;
    }

    private static long sumPositiveItemGains(GameConditionEvent event) {
        long sum = 0;
        for (Long value : event.itemGains().values()) {
            if (value == null || value <= 0) {
                continue;
            }
            sum = sum > Long.MAX_VALUE - value ? Long.MAX_VALUE : sum + value;
        }
        return sum;
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
            Consumer<ConditionSpec> extraValidator) implements ConditionRule<E> {

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
            if (target(spec) <= 0) {
                throw new IllegalArgumentException("condition " + id + " target must be positive");
            }
        }

        @Override
        public long target(ConditionSpec spec) {
            return spec.parameter(targetIndex);
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
