package com.jjg.game.slots.game.moneyrabbit.data;

import com.jjg.game.slots.data.SlotsResultLib;
import org.springframework.stereotype.Component;

@Component
public class MoneyRabbitResultLib extends SlotsResultLib<MoneyRabbitAwardLineInfo> {
    //本次触发的jackpotId
    private int jackpotId;

    public int getJackpotId() {
        return jackpotId;
    }

    public void setJackpotId(int jackpotId) {
        this.jackpotId = jackpotId;
    }
}
