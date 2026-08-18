package com.jjg.game.core.base.condition.numeric;

import java.util.Set;

/** 一次成功的特殊游客邀请请求；一键邀请中的游客统一属于同一次事件。 */
public record GuestInviteConditionEvent(Set<Integer> guestIds) implements ConditionEvent {
    public GuestInviteConditionEvent {
        guestIds = guestIds == null ? Set.of() : Set.copyOf(guestIds);
    }

    public boolean matchesGuest(long guestId) {
        return guestId <= 0 ? !guestIds.isEmpty() : guestIds.contains((int) guestId);
    }
}
