package com.jjg.game.core.base.condition.numeric;

/**
 * 非旋转类玩法动作事件。
 * <p>
 * 通用字段的具体含义由 {@link Type} 固定：subjectId 是动作主体（建筑/员工/道具/游戏等），
 * relatedId 是第二过滤维度，value 是金额或最新状态值，count 是本次数量，qualifier 是星级等
 * 附加门槛，paid 表示付费招募等二态属性。
 */
public record ActionConditionEvent(Type type, int subjectId, int relatedId, long value,
                                   long count, long qualifier, boolean paid) implements ConditionEvent {

    public enum Type {
        BUILDING_UPGRADE,
        BUILDING_LEVEL,
        BUILDING_COUNT,
        AD_WATCH,
        AD_REWARD,
        EMPLOYEE_RECRUIT,
        EMPLOYEE_COUNT,
        GUEST_RECRUIT,
        GUEST_COUNT,
        PRODUCTION_INCOME,
        GAME_RESEARCH,
        GAME_UNLOCK,
        VISIT,
        VISIT_GIFT,
        VISIT_SLOT_SPIN,
        LOGIN,
        CASINO_UNLOCK,
        SCENE_TOTAL_LEVEL,
        ITEM_CONSUME,
        DOUXIAN_SETTLEMENT,
        ACHIEVEMENT_REWARD,
        CARD_POOL_DRAW,
        GUEST_POOL_DRAW,
        EMPLOYEE_POOL_DRAW,
        ALLIANCE_DONATE,
        COMPETITIVE_MATCH,
        SEASON_GEM_DROP,
        GRID_MINED,
        DEPTH_REACHED,
        ITEM_USE,
        ITEM_EXCHANGE,
        SEASON_SHOP_BUY,
        SEASON_GEM_EQUIP,
        SEASON_GEM_CRAFT,
        DAILY_SIGN_IN,
        PASS_CONDITION_TRIGGERED
    }

    public boolean matchesSubject(long expected) {
        return expected <= 0 || expected == subjectId;
    }

    public boolean matchesRelated(long expected) {
        return expected <= 0 || expected == relatedId;
    }
}
