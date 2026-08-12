package com.jjg.game.season.service;

import com.jjg.game.core.service.PlayerPackService;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.sampledata.bean.SeasonShopCfg;
import com.jjg.game.sim.data.SimPlayerContext;
import com.jjg.game.season.data.SeasonPlayerData;
import com.jjg.game.sim.service.SimAutoSaveService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 配置驱动的赛季商店。
 */
@Service
public class SeasonShopService {
    private static final Logger log = LoggerFactory.getLogger(SeasonShopService.class);

    private final SeasonConfigService configService;
    private final PlayerPackService playerPackService;
    private final SimAutoSaveService autoSaveService;

    public SeasonShopService(SeasonConfigService configService, PlayerPackService playerPackService,
                             SimAutoSaveService autoSaveService) {
        this.configService = configService;
        this.playerPackService = playerPackService;
        this.autoSaveService = autoSaveService;
    }

    public CommonResult<Map<Integer, Long>> buy(SimPlayerContext ctx, int shopId, int count) {
        if (count <= 0) {
            log.warn("赛季商店购买参数错误 playerId={},shopId={},count={}", ctx.playerId(), shopId, count);
            return new CommonResult<>(Code.PARAM_ERROR);
        }
        SeasonPlayerData data = ctx.getSeasonPlayerData();
        SeasonShopCfg cfg = configService.shop(shopId, data.seasonPhase());
        if (cfg == null) {
            log.warn("赛季商店配置不存在 playerId={},shopId={},phase={}", ctx.playerId(), shopId, data.getPhase());
            return new CommonResult<>(Code.NOT_FOUND);
        }
        if (!cfg.getIsEnabled()) {
            log.warn("赛季商店商品未开启 playerId={},shopId={},phase={}", ctx.playerId(), shopId, data.getPhase());
            return new CommonResult<>(Code.FORBID);
        }
        Map<Integer, Integer> purchaseCounters = cfg.getResetDaily()
                ? data.getDailyShopPurchases() : data.getShopPurchases();
        int purchased = purchaseCounters.getOrDefault(shopId, 0);
        if (cfg.getDailyPurchaseLimit() >= 0 && purchased + count > cfg.getDailyPurchaseLimit()) {
            log.warn("赛季商店超过限购 playerId={},shopId={},purchased={},count={},limit={}",
                    ctx.playerId(), shopId, purchased, count, cfg.getDailyPurchaseLimit());
            return new CommonResult<>(Code.BET_TO_LIMIT);
        }
        Map<Integer, Long> cost = multiply(cfg.getCost(), count);
        Map<Integer, Long> goods = multiply(cfg.getGoods(), count);
        int currencyId = configService.currencyItemId();
        long coinCost = cost.getOrDefault(currencyId, 0L);
        if (data.getSeasonCoin() < coinCost) {
            log.warn("赛季商店赛季币不足 playerId={},shopId={},need={},have={}",
                    ctx.playerId(), shopId, coinCost, data.getSeasonCoin());
            return new CommonResult<>(Code.NOT_ENOUGH);
        }
        Map<Integer, Long> packCost = new HashMap<>(cost);
        packCost.remove(currencyId);
        if (!packCost.isEmpty() && !playerPackService.removeItems(ctx.getPlayer(), packCost, AddType.ITEM_EXCHANGE,
                "season-shop:" + shopId).success()) {
            log.warn("赛季商店背包道具不足 playerId={},shopId={}", ctx.playerId(), shopId);
            return new CommonResult<>(Code.NOT_ENOUGH_ITEM);
        }
        data.setSeasonCoin(data.getSeasonCoin() - coinCost);
        CommonResult<?> addResult = playerPackService.addItems(ctx.playerId(), goods, AddType.ITEM_EXCHANGE,
                "season-shop:" + shopId, true);
        if (!addResult.success()) {
            data.setSeasonCoin(data.getSeasonCoin() + coinCost);
            if (!packCost.isEmpty()) {
                playerPackService.addItems(ctx.playerId(), packCost, AddType.FAIL_ROLLBACK,
                        "season-shop-rollback:" + shopId, true);
            }
            log.warn("赛季商店发货失败 playerId={},shopId={},code={}", ctx.playerId(), shopId, addResult.code);
            return new CommonResult<>(addResult.code);
        }
        purchaseCounters.put(shopId, purchased + count);
        autoSaveService.enqueueSave(data);
        return new CommonResult<>(Code.SUCCESS, goods);
    }

    private Map<Integer, Long> multiply(Map<Integer, Long> source, int count) {
        Map<Integer, Long> result = new HashMap<>();
        if (source == null) {
            return result;
        }
        source.forEach((id, value) -> {
            if (id != null && value != null && value > 0) {
                result.put(id, Math.multiplyExact(value, count));
            }
        });
        return result;
    }
}
