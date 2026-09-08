package com.jjg.game.season.service;

import com.jjg.game.core.base.condition.numeric.ConditionEvent;
import com.jjg.game.core.base.condition.numeric.ConditionRuleRegistry;
import com.jjg.game.core.base.condition.numeric.ConditionSpec;
import com.jjg.game.core.base.condition.numeric.ConditionUpdate;
import com.jjg.game.core.base.condition.numeric.PreparedCondition;
import com.jjg.game.core.service.PlayerStatService;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BaseCfgBean;
import com.jjg.game.sampledata.bean.PassDetailsCfg;
import com.jjg.game.sampledata.bean.PassListCfg;
import com.jjg.game.sampledata.bean.SeasonStartCfg;
import com.jjg.game.sampledata.bean.ShopRechargeListCfg;
import com.jjg.game.season.constant.SeasonConstant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 通行证配置索引。条件只在索引重建时解析，事件热路径复用 PreparedCondition。
 */
@Service
public class SeasonPassConfigService {
    private static final Logger log = LoggerFactory.getLogger(SeasonPassConfigService.class);

    private final ConditionRuleRegistry conditionRules;
    private final List<PassListCfg> passes;
    private final List<PassDetailsCfg> details;
    private final List<SeasonStartCfg> seasons;
    private final List<ShopRechargeListCfg> shops;
    private volatile ConfigIndex index;

    @Autowired
    public SeasonPassConfigService(ConditionRuleRegistry conditionRules) {
        this(conditionRules, null, null, null, null);
    }

    SeasonPassConfigService(ConditionRuleRegistry conditionRules, List<PassListCfg> passes,
                            List<PassDetailsCfg> details, List<SeasonStartCfg> seasons,
                            List<ShopRechargeListCfg> shops) {
        this.conditionRules = conditionRules;
        this.passes = copy(passes);
        this.details = copy(details);
        this.seasons = copy(seasons);
        this.shops = copy(shops);
    }

    public List<PassDefinition> passes(int seasonId) {
        ConfigIndex index = index();
        SeasonStartCfg season = index.seasonById.get(seasonId);
        if (season == null || season.getSeasonPass() == null || season.getSeasonPass().isEmpty()) {
            return List.of();
        }
        List<PassDefinition> result = new ArrayList<>(season.getSeasonPass().size());
        for (Integer passId : season.getSeasonPass()) {
            PassDefinition pass = passId == null ? null : index.passById.get(passId);
            if (pass != null) {
                result.add(pass);
            }
        }
        return List.copyOf(result);
    }

    public PassDefinition activePass(int seasonId, int passId) {
        return passes(seasonId).stream().filter(pass -> pass.id() == passId).findFirst().orElse(null);
    }

    /** 只匹配配置中的事实条件，不依赖玩家赛季或通行证进度；一次事件至多命中一次。 */
    public boolean matchesConditionEvent(ConditionEvent event) {
        for (PreparedCondition condition : index().eventConditions) {
            ConditionUpdate update = condition.evaluate(event);
            if (update.matched() && update.value() > 0) {
                return true;
            }
        }
        return false;
    }

    public ShopRechargeListCfg shop(PassDefinition pass, int track) {
        if (pass == null) {
            return null;
        }
        int shopId = switch (track) {
            case SeasonConstant.PassTrack.BASIC -> pass.config().getShopRechargeListID();
            case SeasonConstant.PassTrack.PREMIUM -> pass.config().getShopRechargeList1ID();
            default -> 0;
        };
        return shopId <= 0 ? null : index().shopById.get(shopId);
    }

    private ConfigIndex index() {
        Object passSource = source(PassListCfg.class, passes);
        Object detailSource = source(PassDetailsCfg.class, details);
        Object seasonSource = source(SeasonStartCfg.class, seasons);
        Object shopSource = source(ShopRechargeListCfg.class, shops);
        ConfigIndex cached = index;
        if (cached == null || cached.passSource != passSource || cached.detailSource != detailSource
                || cached.seasonSource != seasonSource || cached.shopSource != shopSource) {
            cached = new ConfigIndex(passSource, detailSource, seasonSource, shopSource);
            index = cached;
        }
        return cached;
    }

    private Object source(Class<? extends BaseCfgBean> type, List<?> fixedConfigs) {
        // getCfgBeanList 每次返回新列表；热加载替换的是容器，不能用列表引用判断版本。
        return fixedConfigs == null ? GameDataManager.getInstance().getCfgContainer(type) : fixedConfigs;
    }

    private final class ConfigIndex {
        final Object passSource;
        final Object detailSource;
        final Object seasonSource;
        final Object shopSource;
        final Map<Integer, PassDefinition> passById = new HashMap<>();
        final Map<Integer, SeasonStartCfg> seasonById = new HashMap<>();
        final Map<Integer, ShopRechargeListCfg> shopById = new HashMap<>();
        final List<PreparedCondition> eventConditions;

        ConfigIndex(Object passSource, Object detailSource, Object seasonSource, Object shopSource) {
            this.passSource = passSource;
            this.detailSource = detailSource;
            this.seasonSource = seasonSource;
            this.shopSource = shopSource;
            seasonConfigs().forEach(cfg -> seasonById.putIfAbsent(cfg.getId(), cfg));
            shopConfigs().forEach(cfg -> shopById.putIfAbsent(cfg.getId(), cfg));

            Map<Integer, List<PassDetailsCfg>> detailsByPass = new HashMap<>();
            Map<Integer, PreparedCondition> conditionsByDetail = new HashMap<>();
            Map<String, PreparedCondition> uniqueConditions = new LinkedHashMap<>();
            for (PassDetailsCfg detail : detailConfigs()) {
                detailsByPass.computeIfAbsent(detail.getPassID(), ignored -> new ArrayList<>()).add(detail);
                try {
                    PreparedCondition condition = conditionRules.prepare(ConditionSpec.from(detail.getCompletionCondition()));
                    conditionsByDetail.put(detail.getId(), condition);
                    // 派生统计本身不能再次产生该统计，避免配置自引用。
                    if (condition.spec().id() != PlayerStatService.DAILY_PASS_CONDITION) {
                        uniqueConditions.putIfAbsent(progressKey(0, condition), condition);
                    }
                } catch (RuntimeException e) {
                    log.error("通行证条件配置无效 detailId={}", detail.getId(), e);
                }
            }
            eventConditions = List.copyOf(uniqueConditions.values());
            for (PassListCfg cfg : passConfigs()) {
                if (!cfg.getIsOpen() || passById.containsKey(cfg.getId())) {
                    continue;
                }
                try {
                    PassDefinition pass = buildPass(cfg, detailsByPass.getOrDefault(cfg.getId(), List.of()),
                            conditionsByDetail);
                    passById.put(cfg.getId(), pass);
                } catch (RuntimeException e) {
                    log.error("通行证配置无效，已停用 passId={}", cfg.getId(), e);
                }
            }
        }
    }

    private PassDefinition buildPass(PassListCfg pass, List<PassDetailsCfg> source,
                                     Map<Integer, PreparedCondition> conditionsByDetail) {
        if (source.isEmpty()) {
            throw new IllegalArgumentException("PassDetails is empty");
        }
        List<PassDetailsCfg> sorted = source.stream()
                .sorted(Comparator.comparingInt(PassDetailsCfg::getLevel)
                        .thenComparingInt(PassDetailsCfg::getId))
                .toList();
        List<LevelDefinition> levels = new ArrayList<>(sorted.size());
        Map<String, ChannelBuilder> channelBuilders = new LinkedHashMap<>();
        int previousLevel = 0;
        for (PassDetailsCfg detail : sorted) {
            if (detail.getLevel() <= previousLevel) {
                throw new IllegalArgumentException("duplicate or unordered level " + detail.getLevel());
            }
            previousLevel = detail.getLevel();
            PreparedCondition condition = conditionsByDetail.get(detail.getId());
            if (condition == null) {
                throw new IllegalArgumentException("invalid condition for detail " + detail.getId());
            }
            String progressKey = progressKey(pass.getId(), condition);
            levels.add(new LevelDefinition(detail, condition, progressKey));
            channelBuilders.compute(progressKey, (ignored, builder) -> {
                if (builder == null) {
                    return new ChannelBuilder(progressKey, condition);
                }
                builder.maxTarget = Math.max(builder.maxTarget, condition.target());
                return builder;
            });
        }
        List<ProgressDefinition> progress = channelBuilders.values().stream()
                .map(ChannelBuilder::build).toList();
        return new PassDefinition(pass, List.copyOf(levels), progress);
    }

    private static String progressKey(int passId, PreparedCondition condition) {
        StringBuilder key = new StringBuilder().append(passId).append(':')
                .append(condition.spec().id());
        for (Long parameter : condition.progressParameters()) {
            key.append(':').append(parameter);
        }
        return key.toString();
    }

    private static final class ChannelBuilder {
        final String key;
        final PreparedCondition condition;
        long maxTarget;

        ChannelBuilder(String key, PreparedCondition condition) {
            this.key = key;
            this.condition = condition;
            this.maxTarget = condition.target();
        }

        ProgressDefinition build() {
            return new ProgressDefinition(key, condition, maxTarget);
        }
    }

    public record PassDefinition(PassListCfg config, List<LevelDefinition> levels,
                                 List<ProgressDefinition> progress) {
        public int id() {
            return config.getId();
        }
    }

    public record LevelDefinition(PassDetailsCfg config, PreparedCondition condition, String progressKey) {
    }

    public record ProgressDefinition(String key, PreparedCondition condition, long maxTarget) {
    }

    private List<PassListCfg> passConfigs() {
        return passes == null ? safe(GameDataManager.getPassListCfgList()) : passes;
    }

    private List<PassDetailsCfg> detailConfigs() {
        return details == null ? safe(GameDataManager.getPassDetailsCfgList()) : details;
    }

    private List<SeasonStartCfg> seasonConfigs() {
        return seasons == null ? safe(GameDataManager.getSeasonStartCfgList()) : seasons;
    }

    private List<ShopRechargeListCfg> shopConfigs() {
        return shops == null ? safe(GameDataManager.getShopRechargeListCfgList()) : shops;
    }

    private static <T> List<T> copy(List<T> source) {
        return source == null ? null : List.copyOf(source);
    }

    private static <T> List<T> safe(List<T> source) {
        return source == null ? List.of() : source;
    }
}
