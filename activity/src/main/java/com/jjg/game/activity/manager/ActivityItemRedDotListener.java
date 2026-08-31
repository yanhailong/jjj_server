package com.jjg.game.activity.manager;

import com.jjg.game.activity.common.data.ActivityType;
import com.jjg.game.activity.scratchcards.controller.ScratchCardsController;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.listener.ItemAddListener;
import com.jjg.game.core.listener.ItemConsumeListener;
import com.jjg.game.core.manager.RedDotManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import java.util.Map;

/** 外部道具增加/消费也刷新活动红点，不局限于活动自身的购买接口。 */
@Component
public class ActivityItemRedDotListener implements ItemAddListener, ItemConsumeListener {
    @Lazy @Autowired private ActivityManager activities;
    @Lazy @Autowired private ScratchCardsController scratchCards;
    @Autowired private RedDotManager redDots;

    public void refresh(long playerId, Map<Integer, Long> items) {
        try {
            var cost = scratchCards.getCostItem();
            if (cost != null && items != null && items.containsKey(cost.getId())) {
                redDots.updateRedDot(activities.initialize(playerId, ActivityType.SCRATCH_CARDS.getType()), playerId);
            }
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(getClass()).error("道具变更刷新活动红点失败 playerId={}", playerId, e);
        }
    }
    @Override public void onItemsAdded(long id, Map<Integer, Long> items, AddType type) { refresh(id, items); }
    @Override public void onItemsConsumed(long id, Map<Integer, Long> items, AddType type) { refresh(id, items); }
}
