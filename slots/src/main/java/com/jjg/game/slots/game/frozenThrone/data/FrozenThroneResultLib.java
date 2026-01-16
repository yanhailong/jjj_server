package com.jjg.game.slots.game.frozenThrone.data;

import com.jjg.game.slots.data.SlotsResultLib;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Set;

/**
 * @author lihaocao
 * @date 2025/12/2 17:30
 */
@Document
public class FrozenThroneResultLib extends SlotsResultLib<FrozenThroneAwardLineInfo> {
    //增加的免费次数
    private int addFreeCount;

    public int getAddFreeCount() {
        return addFreeCount;
    }

    public void setAddFreeCount(int addFreeCount) {
        this.addFreeCount = addFreeCount;
    }
}
