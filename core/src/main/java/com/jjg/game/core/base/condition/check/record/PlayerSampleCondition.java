package com.jjg.game.core.base.condition.check.record;

import java.util.List;

/**
 * @author lm
 * @date 2025/10/16 18:03
 */
public class PlayerSampleCondition extends BaseCheckCondition {
    /**
     * id列表 具体内容看使用类
     */
    private List<Integer> ids;
    /**
     * 道具id
     */
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
