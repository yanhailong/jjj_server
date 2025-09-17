package com.jjg.game.slots.game.cleopatra.data;

import com.jjg.game.slots.data.SlotsResultLib;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.*;

/**
 * @author 11
 * @date 2025/8/1 17:30
 */
@Document
public class CleopatraResultLib extends SlotsResultLib<CleopatraAddColumnInfo> {
    //中奖图标id ->坐标id集合
    private Map<Integer,Set<Integer>> winIcons;
    //可以中奖的奖池id
    private List<Integer> jackpotIds;
    //奖池图标坐标
    private Set<Integer> poolIconIndexSet;

    public Map<Integer, Set<Integer>> getWinIcons() {
        return winIcons;
    }

    public void setWinIcons(Map<Integer, Set<Integer>> winIcons) {
        this.winIcons = winIcons;
    }

    public List<Integer> getJackpotIds() {
        return jackpotIds;
    }

    public void setJackpotIds(List<Integer> jackpotIds) {
        this.jackpotIds = jackpotIds;
    }

    public void addJackpotId(int jackpotId) {
        if(this.jackpotIds == null) {
            this.jackpotIds = new ArrayList<>();
        }
        this.jackpotIds.add(jackpotId);
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
