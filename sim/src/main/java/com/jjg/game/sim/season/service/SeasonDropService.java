package com.jjg.game.sim.season.service;

import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.common.utils.WeightRandom;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.sampledata.bean.SeasondropDetailedCfg;
import com.jjg.game.sampledata.bean.SeasonGemCfg;
import com.jjg.game.sampledata.bean.SeasonGemDropCfg;
import com.jjg.game.sampledata.bean.SeasonStartCfg;
import com.jjg.game.sim.data.SimPlayerContext;
import com.jjg.game.sim.season.data.SeasonPlayerData;
import com.jjg.game.sim.service.SimAutoSaveService;
import com.jjg.game.sim.service.SimPackService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.IntPredicate;

/**
 * slots 旋转后的赛季宝石掉落。
 */
@Service
public class SeasonDropService {
    private static final Logger log = LoggerFactory.getLogger(SeasonDropService.class);

    private final SeasonConfigService configService;
    private final SimPackService simPackService;
    private final SimAutoSaveService autoSaveService;
    private final IntPredicate chance;
    private final Function<List<List<Integer>>, List<Integer>> selector;

    @Autowired
    public SeasonDropService(SeasonConfigService configService, SimPackService simPackService,
                             SimAutoSaveService autoSaveService) {
        this(configService, simPackService, autoSaveService, RandomUtils::getRandomBoolean10000,
                SeasonDropService::weightedRow);
    }

    public SeasonDropService(SeasonConfigService configService, SimPackService simPackService,
                             SimAutoSaveService autoSaveService, IntPredicate chance,
                             Function<List<List<Integer>>, List<Integer>> selector) {
        this.configService = configService;
        this.simPackService = simPackService;
        this.autoSaveService = autoSaveService;
        this.chance = chance;
        this.selector = selector;
    }

    public Map<Integer, Long> onSpin(SimPlayerContext ctx, int gameType) {
        SeasonPlayerData data = ctx.getSeasonPlayerData();
        if (data == null) {
            return Map.of();
        }
        //只有当前赛季开放的游戏才参与赛季掉落
        SeasonStartCfg season = configService.season(data.getSeasonId());
        if (season == null || season.getAvailableGames() != gameType) {
            return Map.of();
        }
        SeasonGemDropCfg drop = configService.gemDrop(data.getSeasonId());
        if (drop == null || drop.getDropRate() <= 0 || drop.getDropCount() <= data.getDailyGemDropCount()
                || !chance.test(drop.getDropRate())) {
            return Map.of();
        }
        List<Integer> pool = selector.apply(drop.getDropItem());
        if (pool == null || pool.size() < 3) {
            return Map.of();
        }
        SeasondropDetailedCfg detail = configService.detailedDrop(pool.get(1));
        if (detail == null) {
            return Map.of();
        }
        Map<Integer, Long> rewards = new HashMap<>();
        int poolCount = Math.max(1, pool.get(2));
        for (int i = 0; i < poolCount; i++) {
            List<Integer> gemRow = selector.apply(detail.getDetailedDropItem());
            if (gemRow == null || gemRow.size() < 3) {
                continue;
            }
            SeasonGemCfg gem = configService.gem(gemRow.get(1));
            if (gem != null) {
                rewards.merge(gem.getGemName(), (long) Math.max(1, gemRow.get(2)), Long::sum);
            }
        }
        if (rewards.isEmpty()) {
            return Map.of();
        }
        var result = simPackService.addItems(ctx, rewards, AddType.SIM_SLOTS_DROP,
                "season-gem-drop:" + data.getSeasonId(), true);
        if (!result.success()) {
            log.warn("赛季宝石掉落入账失败 playerId={},seasonId={},code={}",
                    ctx.playerId(), data.getSeasonId(), result.code);
            return Map.of();
        }
        data.setDailyGemDropCount(data.getDailyGemDropCount() + 1);
        autoSaveService.enqueueSave(data);
        return rewards;
    }

    private static List<Integer> weightedRow(List<List<Integer>> rows) {
        if (rows == null || rows.isEmpty()) {
            return null;
        }
        WeightRandom<List<Integer>> random = WeightRandom.create();
        for (List<Integer> row : rows) {
            if (row != null && row.size() >= 3 && row.get(0) > 0) {
                random.add(row, row.get(0));
            }
        }
        return random.next();
    }
}
