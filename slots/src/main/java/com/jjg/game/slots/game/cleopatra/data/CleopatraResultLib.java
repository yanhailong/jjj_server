package com.jjg.game.slots.game.cleopatra.data;

import com.jjg.game.slots.data.SlotsResultLib;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author 11
 * @date 2025/8/1 17:30
 */
@Document
public class CleopatraResultLib extends SlotsResultLib<CleopatraAddColumnInfo> {
    //中奖图标的初始个数
    private List<Integer> winIconIndexList;
    //可以中奖的奖池id
    private List<Integer> jackpotIds;

    public List<Integer> getWinIconIndexList() {
        return winIconIndexList;
    }

    public void setWinIconIndexList(List<Integer> winIconIndexList) {
        this.winIconIndexList = winIconIndexList;
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
}
