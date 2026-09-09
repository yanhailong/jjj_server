package com.jjg.game.season.service;

import com.jjg.game.core.base.condition.numeric.ActionConditionEvent;
import com.jjg.game.core.base.condition.numeric.ConditionEvent;
import com.jjg.game.core.base.condition.numeric.ConditionUpdate;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.Item;
import com.jjg.game.core.data.ItemOperationResult;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.service.MailService;
import com.jjg.game.core.service.PlayerPackService;
import com.jjg.game.core.service.PlayerStatService;
import com.jjg.game.core.utils.ItemUtils;
import com.jjg.game.sampledata.bean.PassDetailsCfg;
import com.jjg.game.sampledata.bean.ShopRechargeListCfg;
import com.jjg.game.season.constant.SeasonConstant;
import com.jjg.game.season.data.SeasonPlayerData;
import com.jjg.game.season.pb.res.NotifySeasonPassLevelUp;
import com.jjg.game.season.pb.res.ResSeasonPassClaim;
import com.jjg.game.season.pb.res.ResSeasonPassList;
import com.jjg.game.season.pb.struct.SeasonPassInfo;
import com.jjg.game.season.pb.struct.SeasonPassLevelInfo;
import com.jjg.game.season.pb.struct.SeasonPassRewardsInfo;
import com.jjg.game.sim.data.SimPlayerContext;
import com.jjg.game.sim.listener.SimConditionEventListener;
import com.jjg.game.sim.service.SimAutoSaveService;
import com.jjg.game.sim.service.SimPlayerStatService;
import com.jjg.game.social.service.SocialSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 通行证领域服务：事实事件累计、列表组装、领奖、付费轨解锁和赛季末补发。
 */
@Service
public class SeasonPassService implements SimConditionEventListener {
    private static final Logger log = LoggerFactory.getLogger(SeasonPassService.class);

    private final SeasonPassConfigService passConfig;
    private final PlayerPackService playerPackService;
    private final SimAutoSaveService autoSaveService;
    private final MailService mailService;
    private final SimPlayerStatService playerStatService;
    private final PlayerStatService lifetimeStatService;
    private final SocialSender socialSender;

    public SeasonPassService(SeasonPassConfigService passConfig, PlayerPackService playerPackService,
                             SimAutoSaveService autoSaveService, MailService mailService,
                             SimPlayerStatService playerStatService, PlayerStatService lifetimeStatService,
                             SocialSender socialSender) {
        this.passConfig = passConfig;
        this.playerPackService = playerPackService;
        this.autoSaveService = autoSaveService;
        this.mailService = mailService;
        this.playerStatService = playerStatService;
        this.lifetimeStatService = lifetimeStatService;
        this.socialSender = socialSender;
    }

    @Override
    public ConditionEvent onConditionEvent(SimPlayerContext ctx, ConditionEvent event) {
        if (ctx == null || event == null) {
            return null;
        }
        ConditionEvent triggered = null;
        if (passConfig.matchesConditionEvent(event)) {
            lifetimeStatService.recordPassCondition(ctx.playerId());
            triggered = new ActionConditionEvent(ActionConditionEvent.Type.PASS_CONDITION_TRIGGERED,
                    0, 0, 0, 1, 0, false);
        }
        SeasonPlayerData data = ctx.getSeasonPlayerData();
        if (data == null || data.getSeasonId() == 0) {
            return triggered;
        }
        boolean changed = false;
        Map<String, Long> progress = data.getPassProgress();
        for (SeasonPassConfigService.PassDefinition pass : passConfig.passes(data.getSeasonId())) {
            for (SeasonPassConfigService.ProgressDefinition channel : pass.progress()) {
                if (playerStatService.readsCurrentState(channel.condition())) {
                    continue;
                }
                long current = progress.getOrDefault(channel.key(), 0L);
                if (current >= channel.maxTarget()) {
                    continue;
                }
                ConditionUpdate update = channel.condition().evaluate(event);
                if (!update.matched()) {
                    continue;
                }
                long next = Math.min(channel.maxTarget(), update.apply(current));
                if (next != current) {
                    progress.put(channel.key(), next);
                    changed = true;
                    notifyCompletedLevels(ctx, pass, channel.key(), current, next);
                }
            }
        }
        if (changed) {
            ctx.setLastSaveTime(0);
        }
        return triggered;
    }

    private void notifyCompletedLevels(SimPlayerContext ctx,
                                       SeasonPassConfigService.PassDefinition pass,
                                       String progressKey, long previous, long current) {
        for (SeasonPassConfigService.LevelDefinition level : pass.levels()) {
            long target = level.condition().target();
            if (!progressKey.equals(level.progressKey()) || previous >= target || current < target) {
                continue;
            }
            NotifySeasonPassLevelUp notify = new NotifySeasonPassLevelUp(Code.SUCCESS);
            notify.passId = pass.id();
            notify.level = level.config().getLevel();
            try {
                socialSender.sendTo(ctx.playerId(), notify);
            } catch (RuntimeException e) {
                log.warn("通行证等级达成通知失败 playerId={},passId={},level={}",
                        ctx.playerId(), notify.passId, notify.level, e);
            }
        }
    }

    public ResSeasonPassList list(SimPlayerContext ctx) {
        ResSeasonPassList response = new ResSeasonPassList(Code.SUCCESS);
        SeasonPlayerData data = ctx.getSeasonPlayerData();
        if (data == null) {
            response.code = Code.NOT_FOUND;
            return response;
        }
        response.passes = passConfig.passes(data.getSeasonId()).stream()
                .map(pass -> assemble(ctx, pass))
                .toList();
        return response;
    }

    public ResSeasonPassClaim claim(SimPlayerContext ctx, int passId) {
        ResSeasonPassClaim response = new ResSeasonPassClaim(Code.SUCCESS);
        response.passId = passId;
        SeasonPlayerData data = ctx.getSeasonPlayerData();
        SeasonPassConfigService.PassDefinition pass = data == null ? null
                : passConfig.activePass(data.getSeasonId(), passId);
        if (pass == null) {
            response.code = Code.NOT_FOUND;
            return response;
        }

        Map<Integer, Integer> claimed = data.getPassClaimedTracks();
        int purchased = data.getPassPurchasedTracks().getOrDefault(passId, 0);
        Map<Integer, Integer> updatedClaims = new LinkedHashMap<>();
        Map<Integer, Long> rewards = new LinkedHashMap<>();
        for (SeasonPassConfigService.LevelDefinition level : pass.levels()) {
            if (progress(ctx, level) < level.condition().target()) {
                continue;
            }
            int detailId = level.config().getId();
            int oldMask = claimed.getOrDefault(detailId, 0);
            int newMask = collectLevelRewards(level.config(), oldMask, purchased, rewards);
            if (newMask != oldMask) {
                updatedClaims.put(detailId, newMask);
            }
        }
        if (rewards.isEmpty()) {
            response.code = Code.REPEAT_OP;
            response.pass = assemble(ctx, pass);
            response.rewards = List.of();
            return response;
        }

        CommonResult<ItemOperationResult> addResult = playerPackService.addItems(ctx.playerId(),
                rewards, AddType.ACTIVITY, "season-pass:" + data.getSeasonKey() + ":" + passId, true);
        if (addResult == null || !addResult.success()) {
            response.code = addResult == null ? Code.EXCEPTION : addResult.code;
            return response;
        }
        claimed.putAll(updatedClaims);
        saveOwner(ctx);
        response.rewards = ItemUtils.buildItemInfo(rewards);
        response.pass = assemble(ctx, pass);
        log.info("领取通行证奖励成功 playerId={},seasonKey={},passId={},rewards={}",
                ctx.playerId(), data.getSeasonKey(), passId, rewards);
        return response;
    }

    /** 赛季结束时补发当前赛季已达成的未领奖励。 */
    public void settleUnclaimed(SimPlayerContext ctx, SeasonPlayerData data) {
        Map<Integer, Long> rewards = new LinkedHashMap<>();
        for (SeasonPassConfigService.PassDefinition pass : passConfig.passes(data.getSeasonId())) {
            int purchased = data.getPassPurchasedTracks().getOrDefault(pass.id(), 0);
            Map<Integer, Integer> claimed = data.getPassClaimedTracks();
            for (SeasonPassConfigService.LevelDefinition level : pass.levels()) {
                if (progress(ctx, level) < level.condition().target()) {
                    continue;
                }
                collectLevelRewards(level.config(), claimed.getOrDefault(level.config().getId(), 0),
                        purchased, rewards);
            }
        }
        if (!rewards.isEmpty()) {
            List<Item> items = new ArrayList<>(rewards.size());
            rewards.forEach((itemId, count) -> items.add(new Item(itemId, count)));
            try {
                mailService.addMailIfAbsent(ctx.playerId(), "赛季通行证奖励",
                        "上赛季已达成但未领取的通行证奖励已补发，请查收。", items,
                        AddType.ACTIVITY, "season-pass-settle:" + data.getSeasonKey() + ":" + ctx.playerId());
            } catch (Exception e) {
                log.error("赛季通行证奖励邮件发放失败 playerId={},seasonKey={}",
                        ctx.playerId(), data.getSeasonKey(), e);
            }
        }
    }

    public String orderToken(SeasonPlayerData data, SeasonPassConfigService.PassDefinition pass, int track) {
        return data.getSeasonKey() + ':' + pass.id() + ':' + track;
    }

    public PurchaseToken parseOrderToken(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        int trackSeparator = value.lastIndexOf(':');
        int passSeparator = trackSeparator <= 0 ? -1 : value.lastIndexOf(':', trackSeparator - 1);
        if (passSeparator <= 0 || trackSeparator >= value.length() - 1) {
            return null;
        }
        try {
            String seasonKey = value.substring(0, passSeparator);
            int passId = Integer.parseInt(value.substring(passSeparator + 1, trackSeparator));
            int track = Integer.parseInt(value.substring(trackSeparator + 1));
            if (seasonKey.isBlank() || passId <= 0 || !validTrack(track)) {
                return null;
            }
            return new PurchaseToken(seasonKey, passId, track);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public boolean unlock(SimPlayerContext ctx, PurchaseToken token) {
        SeasonPlayerData data = ctx == null ? null : ctx.getSeasonPlayerData();
        if (data == null || token == null) {
            return false;
        }
        if (!token.seasonKey().equals(data.getSeasonKey())) {
            return false;
        }
        SeasonPassConfigService.PassDefinition pass = passConfig.activePass(data.getSeasonId(), token.passId());
        if (pass == null) {
            return false;
        }
        Map<Integer, Integer> purchased = data.getPassPurchasedTracks();
        int bit = trackBit(token.track());
        int oldMask = purchased.getOrDefault(pass.id(), 0);
        if ((oldMask & bit) != 0) {
            return true;
        }
        purchased.put(pass.id(), oldMask | bit);
        saveOwner(ctx);
        log.info("通行证付费轨解锁成功 playerId={},seasonKey={},passId={},track={}",
                ctx.playerId(), token.seasonKey(), token.passId(), token.track());
        return true;
    }

    public boolean hasTrackRewards(SeasonPassConfigService.PassDefinition pass, int track) {
        if (pass == null || !validTrack(track)) {
            return false;
        }
        return pass.levels().stream().map(SeasonPassConfigService.LevelDefinition::config)
                .map(level -> track == SeasonConstant.PassTrack.BASIC
                        ? level.getBasicPaidRewards() : level.getPremiumPaidRewards())
                .anyMatch(rewards -> rewards != null && !rewards.isEmpty());
    }

    private SeasonPassInfo assemble(SimPlayerContext ctx, SeasonPassConfigService.PassDefinition pass) {
        SeasonPassInfo info = new SeasonPassInfo();
        info.passId = pass.id();
        SeasonPlayerData data = ctx.getSeasonPlayerData();
        int purchased = data.getPassPurchasedTracks().getOrDefault(pass.id(), 0);
        Map<Integer, Integer> claimed = data.getPassClaimedTracks();
        ShopRechargeListCfg basicShop = passConfig.shop(pass, SeasonConstant.PassTrack.BASIC);
        ShopRechargeListCfg premiumShop = passConfig.shop(pass, SeasonConstant.PassTrack.PREMIUM);
        info.levels = pass.levels().stream().map(level -> {
            SeasonPassLevelInfo levelInfo = new SeasonPassLevelInfo();
            levelInfo.detailId = level.config().getId();
            levelInfo.level = level.config().getLevel();
            levelInfo.conditionId = level.config().getLanguage();
            levelInfo.progress = progress(ctx, level);
            levelInfo.target = level.condition().target();
            levelInfo.completed = levelInfo.progress >= levelInfo.target;
            int mask = claimed.getOrDefault(levelInfo.detailId, 0);
            levelInfo.freeRewards = rewardInfo(level.config().getFreeRewards(), mask,
                    SeasonConstant.PassClaim.FREE, true, null, ctx.getPlayer());
            levelInfo.beginnerRewards = rewardInfo(level.config().getBasicPaidRewards(), mask,
                    SeasonConstant.PassClaim.BASIC,
                    (purchased & SeasonConstant.PassClaim.BASIC) != 0, basicShop, ctx.getPlayer());
            levelInfo.advanceRewards = rewardInfo(level.config().getPremiumPaidRewards(), mask,
                    SeasonConstant.PassClaim.PREMIUM,
                    (purchased & SeasonConstant.PassClaim.PREMIUM) != 0, premiumShop, ctx.getPlayer());
            return levelInfo;
        }).toList();
        return info;
    }

    private SeasonPassRewardsInfo rewardInfo(Map<Integer, Long> rewards, int claimed,
                                             int claimBit, boolean unlocked,
                                             ShopRechargeListCfg shop, Player player) {
        if (rewards == null || rewards.isEmpty()) {
            return null;
        }
        SeasonPassRewardsInfo info = new SeasonPassRewardsInfo();
        info.items = ItemUtils.buildItemInfo(rewards);
        info.status = (claimed & claimBit) != 0
                ? SeasonConstant.PassRewardStatus.CLAIMED
                : unlocked ? SeasonConstant.PassRewardStatus.UNLOCKED : SeasonConstant.PassRewardStatus.LOCKED;
        if (shop != null) {
            info.price = shop.getPrice() == null ? null : shop.getPrice().toPlainString();
            info.channelProductId = channelProductId(player, shop);
        }
        return info;
    }

    private long progress(SimPlayerContext ctx, SeasonPassConfigService.LevelDefinition level) {
        if (playerStatService.readsCurrentState(level.condition())) {
            return Math.max(0, playerStatService.progress(ctx, level.condition()));
        }
        return ctx.getSeasonPlayerData().getPassProgress().getOrDefault(level.progressKey(), 0L);
    }

    private int collectLevelRewards(PassDetailsCfg level, int claimed, int purchased,
                                    Map<Integer, Long> rewards) {
        int result = claimed;
        if ((claimed & SeasonConstant.PassClaim.FREE) == 0 && merge(rewards, level.getFreeRewards())) {
            result |= SeasonConstant.PassClaim.FREE;
        }
        if ((purchased & SeasonConstant.PassClaim.BASIC) != 0
                && (claimed & SeasonConstant.PassClaim.BASIC) == 0
                && merge(rewards, level.getBasicPaidRewards())) {
            result |= SeasonConstant.PassClaim.BASIC;
        }
        if ((purchased & SeasonConstant.PassClaim.PREMIUM) != 0
                && (claimed & SeasonConstant.PassClaim.PREMIUM) == 0
                && merge(rewards, level.getPremiumPaidRewards())) {
            result |= SeasonConstant.PassClaim.PREMIUM;
        }
        return result;
    }

    private boolean merge(Map<Integer, Long> target, Map<Integer, Long> source) {
        if (source == null || source.isEmpty()) {
            return false;
        }
        boolean merged = false;
        for (Map.Entry<Integer, Long> entry : source.entrySet()) {
            if (entry.getKey() != null && entry.getKey() > 0 && entry.getValue() != null && entry.getValue() > 0) {
                target.merge(entry.getKey(), entry.getValue(), Math::addExact);
                merged = true;
            }
        }
        return merged;
    }

    private void saveOwner(SimPlayerContext ctx) {
        ctx.setLastSaveTime(0);
        autoSaveService.enqueueSave(ctx.getSeasonPlayerData());
    }

    public static String channelProductId(Player player, ShopRechargeListCfg shop) {
        if (player == null || shop == null || player.getChannel() == null) {
            return null;
        }
        return player.getChannel() == com.jjg.game.core.data.ChannelType.APPLE
                ? shop.getIosShopId() : shop.getGoogleShopId();
    }

    public static boolean validTrack(int track) {
        return track == SeasonConstant.PassTrack.BASIC || track == SeasonConstant.PassTrack.PREMIUM;
    }

    public static int trackBit(int track) {
        return track == SeasonConstant.PassTrack.BASIC
                ? SeasonConstant.PassClaim.BASIC : SeasonConstant.PassClaim.PREMIUM;
    }

    public record PurchaseToken(String seasonKey, int passId, int track) {
    }
}
