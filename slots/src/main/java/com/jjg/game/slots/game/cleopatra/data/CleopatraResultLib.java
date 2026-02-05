package com.jjg.game.slots.game.cleopatra.data;

import com.jjg.game.slots.data.SlotsResultLib;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author 11
 * @date 2025/8/1 17:30
 */
public class CleopatraResultLib extends SlotsResultLib<CleopatraAddColumnInfo> {
    //中奖图标id ->坐标id集合
    private Map<Integer,Set<Integer>> winIcons;
    //奖池图标坐标
    private Set<Integer> poolIconIndexSet;

    public Map<Integer, Set<Integer>> getWinIcons() {
        return winIcons;
    }

    public void setWinIcons(Map<Integer, Set<Integer>> winIcons) {
        this.winIcons = winIcons;
    }

    public void addWinIcon(int winIcon,Set<Integer> indexSet) {
        if(indexSet == null || indexSet.isEmpty()) {
            return;
        }
        if(this.winIcons == null) {
            this.winIcons = new HashMap<>();
        }
        this.winIcons.computeIfAbsent(winIcon, k -> new HashSet<>()).addAll(indexSet);
    }

    public Set<Integer> getPoolIconIndexSet() {
        return poolIconIndexSet;
    }

    public void setPoolIconIndexSet(Set<Integer> poolIconIndexSet) {
        this.poolIconIndexSet = poolIconIndexSet;
    }
}
