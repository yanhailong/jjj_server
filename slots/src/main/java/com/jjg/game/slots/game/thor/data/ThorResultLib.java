package com.jjg.game.slots.game.thor.data;

import com.jjg.game.slots.data.SlotsResultLib;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Set;

/**
 * @author 11
 * @date 2025/12/1 18:01
 */
@Document
public class ThorResultLib extends SlotsResultLib<ThorAwardLineInfo> {
    //本次触发的jackpotId
    private int jackpotId;
    //冻结的wild
    private Set<Integer> freezeWildSet;

    public int getJackpotId() {
        return jackpotId;
    }

    public void setJackpotId(int jackpotId) {
        this.jackpotId = jackpotId;
    }

    public Set<Integer> getFreezeWildSet() {
        return freezeWildSet;
    }

    public void setFreezeWildSet(Set<Integer> freezeWildSet) {
        this.freezeWildSet = freezeWildSet;
    }
}
