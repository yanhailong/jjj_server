package com.jjg.game.slots.listener;

import com.jjg.game.sim.constant.SimConstant;
import com.jjg.game.sim.listener.SimSpecialItemBalanceListener;
import com.jjg.game.slots.data.SlotsPlayerGameData;
import com.jjg.game.slots.manager.SlotsFactoryManager;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class SlotsSeasonCoinBalanceListener implements SimSpecialItemBalanceListener {
    private final SlotsFactoryManager slotsFactoryManager;

    public SlotsSeasonCoinBalanceListener(@Lazy SlotsFactoryManager slotsFactoryManager) {
        this.slotsFactoryManager = slotsFactoryManager;
    }

    @Override
    public boolean support(int itemId) {
        return itemId == SimConstant.Item.ID_SEASON_COIN;
    }

    @Override
    public void onBalanceChanged(long playerId, int itemId, long balance) {
        SlotsPlayerGameData data = slotsFactoryManager.getPlayerGameData(playerId);
        if (data != null && data.isSeason()) {
            data.setSeasonCoinBalance(balance);
        }
    }
}
