package com.jjg.game.slots.game.thor.data;

import com.jjg.game.slots.data.SlotsResultLib;

import java.util.Set;

/**
 * @author 11
 * @date 2025/12/1 18:01
 */
public class ThorResultLib extends SlotsResultLib<ThorAwardLineInfo> {
    //冻结的wild
    private Set<Integer> freezeWildSet;

    public Set<Integer> getFreezeWildSet() {
        return freezeWildSet;
    }

    public void setFreezeWildSet(Set<Integer> freezeWildSet) {
        this.freezeWildSet = freezeWildSet;
    }
}
