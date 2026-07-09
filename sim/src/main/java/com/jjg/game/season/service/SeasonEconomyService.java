package com.jjg.game.season.service;

import com.jjg.game.core.constant.AddType;
import com.jjg.game.sampledata.bean.SeasonTierCfg;
import com.jjg.game.sim.data.SimPlayerContext;
import com.jjg.game.season.data.SeasonPlayerData;
import com.jjg.game.sim.service.SimPackService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 赛季币与段位晋升的统一入口。
 */
@Service
public class SeasonEconomyService {
    private static final Logger log = LoggerFactory.getLogger(SeasonEconomyService.class);

    private final SeasonConfigService configService;
    private final SimPackService simPackService;

    public SeasonEconomyService(SeasonConfigService configService, SimPackService simPackService) {
        this.configService = configService;
        this.simPackService = simPackService;
    }

    public void addEarnedCoin(SimPlayerContext ctx, long amount) {
        if (amount <= 0) {
            return;
        }
        SeasonPlayerData data = ctx.getSeasonPlayerData();
        data.setSeasonCoin(Math.addExact(data.getSeasonCoin(), amount));
        data.setTotalEarnedCoin(Math.addExact(data.getTotalEarnedCoin(), amount));
        updateTier(ctx);
    }

    public boolean spend(SeasonPlayerData data, long amount) {
        if (amount < 0 || data.getSeasonCoin() < amount) {
            return false;
        }
        data.setSeasonCoin(data.getSeasonCoin() - amount);
        return true;
    }

    private void updateTier(SimPlayerContext ctx) {
        SeasonPlayerData data = ctx.getSeasonPlayerData();
        SeasonTierCfg tier = configService.tierFor(data.seasonPhase(), data.getTotalEarnedCoin());
        if (tier == null || tier.getId() == data.getTierId()) {
            return;
        }
        data.setTierId(tier.getId());
        Map<Integer, Long> reward = tier.getRankUpReward();
        if (reward == null || reward.isEmpty()) {
            return;
        }
        int currencyId = configService.currencyItemId();
        Map<Integer, Long> packReward = new HashMap<>(reward);
        Long coin = packReward.remove(currencyId);
        if (coin != null && coin > 0) {
            data.setSeasonCoin(Math.addExact(data.getSeasonCoin(), coin));
        }
        if (!packReward.isEmpty()) {
            var result = simPackService.addItems(ctx, packReward, AddType.ACTIVITY,
                    "season-tier:" + tier.getId(), true);
            if (!result.success()) {
                log.warn("赛季段位奖励发放失败 playerId={},tierId={},code={}",
                        ctx.playerId(), tier.getId(), result.code);
            }
        }
    }
}
