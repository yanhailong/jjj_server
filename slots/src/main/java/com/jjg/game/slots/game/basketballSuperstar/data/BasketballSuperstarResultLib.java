package com.jjg.game.slots.game.basketballSuperstar.data;

import com.jjg.game.slots.data.SlotsResultLib;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

/**
 * @author lihaocao
 * @date 2025/12/2 17:30
 */
@Document
public class BasketballSuperstarResultLib extends SlotsResultLib<BasketballSuperstarAwardLineInfo> {
    //本次触发的jackpotId
    private int jackpotId;
    //增加的免费次数
    private int addFreeCount;

    public int getJackpotId() {
        return jackpotId;
    }

    public void setJackpotId(int jackpotId) {
        this.jackpotId = jackpotId;
    }

    public int getAddFreeCount() {
        return addFreeCount;
    }

    public void setAddFreeCount(int addFreeCount) {
        this.addFreeCount = addFreeCount;
    }
}
