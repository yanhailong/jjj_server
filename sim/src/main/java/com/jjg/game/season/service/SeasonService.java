package com.jjg.game.season.service;

import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.PlayerPack;
import com.jjg.game.core.pb.KVInfo;
import com.jjg.game.core.service.PlayerPackService;
import com.jjg.game.core.utils.ItemUtils;
import com.jjg.game.sampledata.bean.SeasonGemCfg;
import com.jjg.game.sampledata.bean.SeasonShopCfg;
import com.jjg.game.sampledata.bean.SeasonStartCfg;
import com.jjg.game.season.data.*;
import com.jjg.game.season.pb.res.*;
import com.jjg.game.season.pb.struct.*;
import com.jjg.game.sim.data.SimPlayerContext;
import com.jjg.game.sim.data.SpinStatInfo;
import com.jjg.game.sim.listener.SimPlayerTickListener;
import com.jjg.game.season.model.SeasonSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 赛季协议层门面，集中完成领域对象到客户端结构的转换。
 */
@Service
public class SeasonService implements SimPlayerTickListener {
    private static final Logger log = LoggerFactory.getLogger(SeasonService.class);

    private final SeasonLifecycleService lifecycleService;
    private final SeasonConfigService configService;
    private final SeasonShopService shopService;
    private final SeasonGemService gemService;
    private final SeasonMatchService matchService;
    private final SeasonDropService dropService;
    private final SeasonRankingService rankingService;
    private final PlayerPackService playerPackService;

    public SeasonService(SeasonLifecycleService lifecycleService, SeasonConfigService configService,
                         SeasonShopService shopService, SeasonGemService gemService,
                         SeasonMatchService matchService, SeasonDropService dropService,
                         SeasonRankingService rankingService, PlayerPackService playerPackService) {
        this.lifecycleService = lifecycleService;
        this.configService = configService;
        this.shopService = shopService;
        this.gemService = gemService;
        this.matchService = matchService;
        this.dropService = dropService;
        this.rankingService = rankingService;
        this.playerPackService = playerPackService;
    }

    public ResSeasonInfo info(SimPlayerContext ctx) {
        ResSeasonInfo response = new ResSeasonInfo(Code.SUCCESS);
        SeasonSnapshot snapshot = lifecycleService.ensureCurrent(ctx, System.currentTimeMillis());
        SeasonPlayerData data = ctx.getSeasonPlayerData();
        SeasonStartCfg cfg = configService.season(snapshot.seasonId());
        SeasonInfo info = new SeasonInfo();
        info.seasonId = snapshot.seasonId();
        info.phase = snapshot.phase().ordinal() + 1;
        info.cycleIndex = snapshot.cycleIndex();
        info.nameLanguageId = cfg == null ? 0 : cfg.getSeasonName();
        info.startTime = snapshot.startTime();
        info.endTime = snapshot.endTime();
        info.day = snapshot.day();
        info.gameType = cfg == null ? 0 : cfg.getAvailableGames();
        info.seasonCoin = data.getSeasonCoin();
        info.totalEarnedCoin = data.getTotalEarnedCoin();
        info.tierId = data.getTierId();
        info.dailyMatchCount = data.getDailyMatchCount();
        info.dailyWinAmount = data.getDailyWinAmount();
        info.dailyLossAmount = data.getDailyLossAmount();
        info.rank = rankingService.rankOf(data);
        info.trialStars = data.getTrialStars().entrySet().stream().map(entry -> {
            KVInfo kv = new KVInfo();
            kv.key = entry.getKey();
            kv.value = entry.getValue();
            return kv;
        }).toList();
        response.info = info;
        return response;
    }

    public ResSeasonShop shop(SimPlayerContext ctx) {
        lifecycleService.ensureCurrent(ctx, System.currentTimeMillis());
        SeasonPlayerData data = ctx.getSeasonPlayerData();
        ResSeasonShop response = new ResSeasonShop(Code.SUCCESS);
        response.items = configService.shops(data.seasonPhase()).stream().map(cfg -> shopInfo(cfg, data)).toList();
        response.seasonCoin = data.getSeasonCoin();
        return response;
    }

    public ResSeasonBuy buy(SimPlayerContext ctx, int shopId, int count) {
        lifecycleService.ensureCurrent(ctx, System.currentTimeMillis());
        CommonResult<Map<Integer, Long>> result = shopService.buy(ctx, shopId, count);
        ResSeasonBuy response = new ResSeasonBuy(result.code);
        if (result.data != null && !result.data.isEmpty()) {
            response.goods = ItemUtils.buildItemInfo(result.data);
        }
        response.seasonCoin = ctx.getSeasonPlayerData().getSeasonCoin();
        SeasonShopCfg cfg = configService.shop(shopId, ctx.getSeasonPlayerData().seasonPhase());
        response.purchased = cfg != null && cfg.getResetDaily()
                ? ctx.getSeasonPlayerData().getDailyShopPurchases().getOrDefault(shopId, 0)
                : ctx.getSeasonPlayerData().getShopPurchases().getOrDefault(shopId, 0);
        return response;
    }

    public ResSeasonGems gems(SimPlayerContext ctx) {
        lifecycleService.ensureCurrent(ctx, System.currentTimeMillis());
        ResSeasonGems response = new ResSeasonGems(Code.SUCCESS);
        PlayerPack pack = playerPackService.getFromAllDB(ctx.playerId());
        Map<Integer, Integer> equipped = ctx.getSeasonPlayerData().getEquippedGems();
        response.gems = configService.gems().stream().map(cfg -> gemInfo(cfg, pack, equipped)).toList();
        response.slots = slots(equipped);
        return response;
    }

    public ResSeasonEquipGem equip(SimPlayerContext ctx, int slot, int itemId) {
        lifecycleService.ensureCurrent(ctx, System.currentTimeMillis());
        CommonResult<Map<Integer, Integer>> result = gemService.equip(ctx, slot, itemId);
        ResSeasonEquipGem response = new ResSeasonEquipGem(result.code);
        response.slots = slots(result.data == null ? ctx.getSeasonPlayerData().getEquippedGems() : result.data);
        return response;
    }

    public ResSeasonCraftGem craft(SimPlayerContext ctx, List<Integer> itemIds, int keepItemId) {
        lifecycleService.ensureCurrent(ctx, System.currentTimeMillis());
        CommonResult<SeasonCraftResult> result = gemService.craft(ctx, itemIds, keepItemId);
        ResSeasonCraftGem response = new ResSeasonCraftGem(result.code);
        if (result.data != null) {
            response.success = result.data.isSuccess();
            response.resultItemId = result.data.getResultItemId();
            response.resultCount = result.data.getResultCount();
            response.keptItemId = result.data.getKeptItemId();
        }
        response.seasonCoin = ctx.getSeasonPlayerData().getSeasonCoin();
        return response;
    }

    public ResSeasonMatch match(SimPlayerContext ctx, int gameType, long stake) {
        lifecycleService.ensureCurrent(ctx, System.currentTimeMillis());
        CommonResult<SeasonMatchSession> result = matchService.start(ctx, gameType, stake, System.currentTimeMillis());
        ResSeasonMatch response = new ResSeasonMatch(result.code);
        if (result.data != null) {
            response.matchId = result.data.getMatchId();
            response.opponentId = result.data.getOpponentId();
            response.gameType = result.data.getGameType();
            response.stake = result.data.getStake();
            response.expectedSpins = result.data.getExpectedSpins();
            response.opponentSpinWins = result.data.getOpponentSpinWins();
        }
        response.seasonCoin = ctx.getSeasonPlayerData().getSeasonCoin();
        return response;
    }

    public ResSeasonMatchHistory history(SimPlayerContext ctx) {
        lifecycleService.ensureCurrent(ctx, System.currentTimeMillis());
        ResSeasonMatchHistory response = new ResSeasonMatchHistory(Code.SUCCESS);
        response.records = ctx.getSeasonPlayerData().getMatchHistory().stream().map(this::recordInfo).toList();
        return response;
    }

    public ResSeasonRank rank(SimPlayerContext ctx, int requestedLimit) {
        lifecycleService.ensureCurrent(ctx, System.currentTimeMillis());
        SeasonPlayerData data = ctx.getSeasonPlayerData();
        int configuredLimit = configService.rankingLimit(data.seasonPhase());
        int limit = requestedLimit <= 0 ? configuredLimit : Math.min(requestedLimit, configuredLimit);
        ResSeasonRank response = new ResSeasonRank(Code.SUCCESS);
        response.entries = rankingService.ranking(data.getSeasonKey(), Math.max(1, limit)).stream()
                .map(this::rankInfo).toList();
        response.selfRank = rankingService.rankOf(data);
        return response;
    }

    /**
     * slots 普通旋转成功后的赛季联动；无对局结算时不产生通知。
     */
    public NotifySeasonMatchResult onSpin(SimPlayerContext ctx, int gameType, SpinStatInfo statInfo) {
        dropService.onSpin(ctx, gameType);
        CommonResult<SeasonMatchResult> result = matchService.onSpin(ctx, gameType, statInfo, System.currentTimeMillis());
        if (result.data == null) {
            //非本局游戏/无对局/重复结算等场景静默跳过, 不向客户端下发错误通知
            if (!result.success()) {
                log.warn("赛季对局旋转结算跳过 playerId={},gameType={},code={}", ctx.playerId(), gameType, result.code);
            }
            return null;
        }
        return matchNotify(result.data);
    }

    /**
     * 玩家 tick: 超时对局按弃赛结算并通知, 玩家不旋转/不再匹配时押金也能按时释放。
     */
    @Override
    public void onTick(SimPlayerContext ctx, long now) {
        SeasonMatchResult result = matchService.settleIfExpired(ctx, now);
        if (result != null && ctx.getPlayerController() != null) {
            ctx.send(matchNotify(result));
        }
    }

    /**
     * 先于生命周期切季 (order=100) 结算超时对局, 避免残留对局被切季直接清掉。
     */
    @Override
    public int order() {
        return 90;
    }

    private NotifySeasonMatchResult matchNotify(SeasonMatchResult result) {
        NotifySeasonMatchResult notify = new NotifySeasonMatchResult(Code.SUCCESS);
        notify.matchId = result.getMatchId();
        notify.result = result.getResult();
        notify.playerTotalWin = result.getPlayerTotalWin();
        notify.opponentTotalWin = result.getOpponentTotalWin();
        notify.coinChange = result.getCoinChange();
        notify.seasonCoin = result.getSeasonCoin();
        return notify;
    }

    private SeasonShopItemInfo shopInfo(SeasonShopCfg cfg, SeasonPlayerData data) {
        SeasonShopItemInfo info = new SeasonShopItemInfo();
        info.id = cfg.getId();
        info.order = cfg.getOrder();
        info.goods = cfg.getGoods() == null ? List.of() : ItemUtils.buildItemInfo(cfg.getGoods());
        info.cost = cfg.getCost() == null ? List.of() : ItemUtils.buildItemInfo(cfg.getCost());
        info.resetDaily = cfg.getResetDaily();
        info.purchaseLimit = cfg.getDailyPurchaseLimit();
        info.purchased = (cfg.getResetDaily() ? data.getDailyShopPurchases() : data.getShopPurchases())
                .getOrDefault(cfg.getId(), 0);
        info.languageId = cfg.getLanguage();
        info.icon = cfg.getIcon();
        return info;
    }

    private SeasonGemInfo gemInfo(SeasonGemCfg cfg, PlayerPack pack, Map<Integer, Integer> equipped) {
        SeasonGemInfo info = new SeasonGemInfo();
        info.configId = cfg.getId();
        info.itemId = cfg.getGemName();
        info.type = cfg.getType();
        info.genre = cfg.getGenre();
        info.rarity = cfg.getRarity();
        info.buff = cfg.getBuff();
        info.count = pack == null ? 0 : pack.getItemCount(cfg.getGemName());
        info.equippedCount = (int) equipped.values().stream().filter(item -> item == cfg.getGemName()).count();
        info.nameLanguageId = cfg.getGemName();
        info.descLanguageId = cfg.getGemDesc();
        info.icon = cfg.getIcon();
        return info;
    }

    private List<SeasonGemSlotInfo> slots(Map<Integer, Integer> equipped) {
        List<SeasonGemSlotInfo> result = new ArrayList<>(equipped.size());
        equipped.forEach((slot, itemId) -> {
            SeasonGemSlotInfo info = new SeasonGemSlotInfo();
            info.slot = slot;
            info.itemId = itemId;
            result.add(info);
        });
        result.sort(java.util.Comparator.comparingInt(info -> info.slot));
        return result;
    }

    private SeasonMatchRecordInfo recordInfo(SeasonMatchRecord record) {
        SeasonMatchRecordInfo info = new SeasonMatchRecordInfo();
        info.matchId = record.getMatchId();
        info.opponentId = record.getOpponentId();
        info.gameType = record.getGameType();
        info.stake = record.getStake();
        info.playerSpinWins = record.getPlayerSpinWins();
        info.opponentSpinWins = record.getOpponentSpinWins();
        info.result = record.getResult();
        info.coinChange = record.getCoinChange();
        info.finishTime = record.getFinishTime();
        return info;
    }

    private SeasonRankInfo rankInfo(SeasonRankEntry entry) {
        SeasonRankInfo info = new SeasonRankInfo();
        info.rank = entry.getRank();
        info.playerId = entry.getPlayerId();
        info.seasonCoin = entry.getSeasonCoin();
        info.totalEarnedCoin = entry.getTotalEarnedCoin();
        info.tierId = entry.getTierId();
        return info;
    }
}
