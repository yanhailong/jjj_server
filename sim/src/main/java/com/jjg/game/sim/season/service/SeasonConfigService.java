package com.jjg.game.sim.season.service;

import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.SeasondropDetailedCfg;
import com.jjg.game.sampledata.bean.SeasonGemCfg;
import com.jjg.game.sampledata.bean.SeasonGemCraftCfg;
import com.jjg.game.sampledata.bean.SeasonGemDropCfg;
import com.jjg.game.sampledata.bean.SeasonMatchCfg;
import com.jjg.game.sampledata.bean.SeasonRankingCfg;
import com.jjg.game.sampledata.bean.SeasonShopCfg;
import com.jjg.game.sampledata.bean.SeasonStartCfg;
import com.jjg.game.sampledata.bean.SeasonTierCfg;
import com.jjg.game.sim.season.config.SeasonDefinition;
import com.jjg.game.sim.season.model.SeasonPhase;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Season 配置表的类型安全访问入口。
 */
@Service
public class SeasonConfigService {
    private final List<SeasonStartCfg> starts;
    private final List<SeasonGemCfg> gems;
    private final List<SeasonGemCraftCfg> crafts;
    private final List<SeasonGemDropCfg> drops;
    private final List<SeasonMatchCfg> matches;
    private final List<SeasonRankingCfg> rankings;
    private final List<SeasonShopCfg> shops;
    private final List<SeasonTierCfg> tiers;
    private final List<SeasondropDetailedCfg> detailedDrops;

    public SeasonConfigService() {
        this.starts = null;
        this.gems = null;
        this.crafts = null;
        this.drops = null;
        this.matches = null;
        this.rankings = null;
        this.shops = null;
        this.tiers = null;
        this.detailedDrops = null;
    }

    public SeasonConfigService(List<SeasonStartCfg> starts, List<SeasonGemCfg> gems,
                               List<SeasonGemCraftCfg> crafts, List<SeasonGemDropCfg> drops,
                               List<SeasonMatchCfg> matches, List<SeasonRankingCfg> rankings,
                               List<SeasonShopCfg> shops, List<SeasonTierCfg> tiers,
                               List<SeasondropDetailedCfg> detailedDrops) {
        this.starts = copy(starts);
        this.gems = copy(gems);
        this.crafts = copy(crafts);
        this.drops = copy(drops);
        this.matches = copy(matches);
        this.rankings = copy(rankings);
        this.shops = copy(shops);
        this.tiers = copy(tiers);
        this.detailedDrops = copy(detailedDrops);
    }

    public List<SeasonDefinition> definitions() {
        return startConfigs().stream()
                .map(cfg -> new SeasonDefinition(cfg.getId(), cfg.getSeasonDuration(), cfg.getLoopSequence()))
                .toList();
    }

    public SeasonStartCfg season(int seasonId) {
        return startConfigs().stream().filter(cfg -> cfg.getId() == seasonId).findFirst().orElse(null);
    }

    public SeasonMatchCfg matchForDay(int day) {
        return index().matchByDay.get(day);
    }

    public SeasonTierCfg tierFor(SeasonPhase phase, long totalEarned) {
        int type = tierType(phase);
        if (type == 0) {
            return null;
        }
        return tierConfigs().stream()
                .filter(cfg -> cfg.getRanktype() == type && SeasonPolicy.contains(cfg.getRankRange(), totalEarned))
                .min(Comparator.comparingInt(SeasonTierCfg::getId))
                .orElse(null);
    }

    public List<SeasonTierCfg> tiers(SeasonPhase phase) {
        int type = tierType(phase);
        return tierConfigs().stream().filter(cfg -> cfg.getRanktype() == type)
                .sorted(Comparator.comparingInt(SeasonTierCfg::getId)).toList();
    }

    public SeasonRankingCfg rankingReward(SeasonPhase phase, int rank) {
        int type = rankingType(phase);
        return rankingConfigs().stream()
                .filter(cfg -> cfg.getType() == type && SeasonPolicy.contains(cfg.getRanking(), rank))
                .findFirst().orElse(null);
    }

    public int rankingLimit(SeasonPhase phase) {
        int type = rankingType(phase);
        return rankingConfigs().stream().filter(cfg -> cfg.getType() == type && cfg.getRanking() != null)
                .flatMap(cfg -> cfg.getRanking().stream()).mapToInt(Integer::intValue).max().orElse(0);
    }

    public List<SeasonShopCfg> shops(SeasonPhase phase) {
        int type = shopType(phase);
        return shopConfigs().stream().filter(cfg -> cfg.getType() == type)
                .sorted(Comparator.comparingInt(SeasonShopCfg::getOrder)).toList();
    }

    public SeasonShopCfg shop(int id, SeasonPhase phase) {
        int type = shopType(phase);
        return shopConfigs().stream().filter(cfg -> cfg.getId() == id && cfg.getType() == type).findFirst().orElse(null);
    }

    public SeasonGemCfg gem(int cfgId) {
        return index().gemById.get(cfgId);
    }

    public SeasonGemCfg gemByItemId(int itemId) {
        return index().gemByItemId.get(itemId);
    }

    public List<SeasonGemCfg> gems() {
        return gemConfigs();
    }

    public SeasonGemCraftCfg craftForRarity(int rarity) {
        return craftConfigs().stream().filter(cfg -> cfg.getSynthesisGemQuality() == rarity).findFirst().orElse(null);
    }

    public SeasonGemDropCfg gemDrop(int seasonId) {
        return dropConfigs().stream().filter(cfg -> cfg.getSeasonID() == seasonId).findFirst().orElse(null);
    }

    public SeasondropDetailedCfg detailedDrop(int id) {
        return detailedDropConfigs().stream().filter(cfg -> cfg.getId() == id).findFirst().orElse(null);
    }

    public int currencyItemId() {
        return index().currencyItemId;
    }

    private int tierType(SeasonPhase phase) {
        return phase == SeasonPhase.ADVANCED ? 1 : phase == SeasonPhase.LOOP ? 2 : 0;
    }

    private int shopType(SeasonPhase phase) {
        return phase == SeasonPhase.ADVANCED ? 1 : phase == SeasonPhase.LOOP ? 2 : 0;
    }

    private int rankingType(SeasonPhase phase) {
        return switch (phase) {
            case NOVICE -> 1;
            case ADVANCED -> 2;
            case LOOP -> 3;
        };
    }

    private static <T> List<T> copy(List<T> source) {
        return source == null ? List.of() : List.copyOf(source);
    }

    /**
     * 热路径查询 (旋转结算/涨币升段/掉落) 的索引缓存, 按配置列表引用判断失效:
     * 配置热更会整体替换列表引用, 此时懒重建; 并发重建无害 (结果等价)。
     */
    private volatile ConfigIndex index;

    private ConfigIndex index() {
        List<SeasonMatchCfg> matchSource = matchConfigs();
        List<SeasonGemCfg> gemSource = gemConfigs();
        List<SeasonShopCfg> shopSource = shopConfigs();
        ConfigIndex cached = index;
        if (cached == null || cached.matchSource != matchSource
                || cached.gemSource != gemSource || cached.shopSource != shopSource) {
            cached = new ConfigIndex(matchSource, gemSource, shopSource);
            index = cached;
        }
        return cached;
    }

    private static final class ConfigIndex {
        final List<SeasonMatchCfg> matchSource;
        final List<SeasonGemCfg> gemSource;
        final List<SeasonShopCfg> shopSource;
        final Map<Integer, SeasonMatchCfg> matchByDay = new HashMap<>();
        final Map<Integer, SeasonGemCfg> gemById = new HashMap<>();
        final Map<Integer, SeasonGemCfg> gemByItemId = new HashMap<>();
        final int currencyItemId;

        ConfigIndex(List<SeasonMatchCfg> matchSource, List<SeasonGemCfg> gemSource,
                    List<SeasonShopCfg> shopSource) {
            this.matchSource = matchSource;
            this.gemSource = gemSource;
            this.shopSource = shopSource;
            matchSource.forEach(cfg -> matchByDay.putIfAbsent(cfg.getDays(), cfg));
            gemSource.forEach(cfg -> {
                gemById.putIfAbsent(cfg.getId(), cfg);
                gemByItemId.putIfAbsent(cfg.getGemName(), cfg);
            });
            this.currencyItemId = shopSource.stream().filter(cfg -> cfg.getCost() != null)
                    .flatMap(cfg -> cfg.getCost().keySet().stream()).findFirst().orElse(0);
        }
    }

    private List<SeasonStartCfg> startConfigs() { return starts == null ? safe(GameDataManager.getSeasonStartCfgList()) : starts; }
    private List<SeasonGemCfg> gemConfigs() { return gems == null ? safe(GameDataManager.getSeasonGemCfgList()) : gems; }
    private List<SeasonGemCraftCfg> craftConfigs() { return crafts == null ? safe(GameDataManager.getSeasonGemCraftCfgList()) : crafts; }
    private List<SeasonGemDropCfg> dropConfigs() { return drops == null ? safe(GameDataManager.getSeasonGemDropCfgList()) : drops; }
    private List<SeasonMatchCfg> matchConfigs() { return matches == null ? safe(GameDataManager.getSeasonMatchCfgList()) : matches; }
    private List<SeasonRankingCfg> rankingConfigs() { return rankings == null ? safe(GameDataManager.getSeasonRankingCfgList()) : rankings; }
    private List<SeasonShopCfg> shopConfigs() { return shops == null ? safe(GameDataManager.getSeasonShopCfgList()) : shops; }
    private List<SeasonTierCfg> tierConfigs() { return tiers == null ? safe(GameDataManager.getSeasonTierCfgList()) : tiers; }
    private List<SeasondropDetailedCfg> detailedDropConfigs() { return detailedDrops == null ? safe(GameDataManager.getSeasondropDetailedCfgList()) : detailedDrops; }
    private static <T> List<T> safe(List<T> source) { return source == null ? List.of() : source; }
}
