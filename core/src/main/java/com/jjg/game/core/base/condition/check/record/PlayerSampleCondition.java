package com.jjg.game.core.base.condition.check.record;

import java.util.List;

/**
 * @author lm
 * @date 2025/10/16 18:03
 */
public class PlayerSampleCondition extends BaseCheckCondition {
    private List<Integer> ids;
    private int itemId;

    public List<Integer> getIds() {
        return ids;
    }

    public void setIds(List<Integer> ids) {
        this.ids = ids;
    }


    public int getItemId() {
        return itemId;
    }

    public void setItemId(int itemId) {
        this.itemId = itemId;
    }
}
