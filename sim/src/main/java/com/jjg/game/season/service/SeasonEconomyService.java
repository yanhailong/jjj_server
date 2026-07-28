package com.jjg.game.season.service;

import com.jjg.game.core.service.PlayerPackService;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.sampledata.bean.SeasonTierCfg;
import com.jjg.game.sim.data.SimPlayerContext;
import com.jjg.game.season.data.SeasonPlayerData;
import com.jjg.game.season.pb.res.NotifySeasonTierUp;
import com.jjg.game.social.service.SocialSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 赛季币与段位晋升的统一入口。
 */
@Service
public class SeasonEconomyService {
    private static final Logger log = LoggerFactory.getLogger(SeasonEconomyService.class);

    private final SeasonConfigService configService;
    private final PlayerPackService playerPackService;
    private final SocialSender socialSender;

    public SeasonEconomyService(SeasonConfigService configService, PlayerPackService playerPackService,
                                SocialSender socialSender) {
        this.configService = configService;
        this.playerPackService = playerPackService;
        this.socialSender = socialSender;
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

    /**
     * 从赛季进入的 slots 下注扣币: amount<=0 只读当前余额; 余额不足返回 -1; 成功返回扣后余额。
     */
    public long spendForSlots(SeasonPlayerData data, long amount) {
        if (amount <= 0) {
            return data.getSeasonCoin();
        }
        if (data.getSeasonCoin() < amount) {
            return -1;
        }
        data.setSeasonCoin(data.getSeasonCoin() - amount);
        return data.getSeasonCoin();
    }

    /**
     * 从赛季进入的 slots 中奖加币: 仅增加可用余额, 不计入 totalEarnedCoin/段位
     * (段位由 PK 匹配驱动; 排行榜按 seasonCoin 余额排序, 中奖自然体现)。
     * amount<=0 只读当前余额; 返回加后余额。
     */
    public long addSlotsWinCoin(SeasonPlayerData data, long amount) {
        if (amount > 0) {
            data.setSeasonCoin(Math.addExact(data.getSeasonCoin(), amount));
        }
        return data.getSeasonCoin();
    }

    private void updateTier(SimPlayerContext ctx) {
        SeasonPlayerData data = ctx.getSeasonPlayerData();
        SeasonTierCfg targetTier = configService.tierFor(data.seasonPhase(), data.getTotalEarnedCoin());
        if (targetTier == null || targetTier.getId() == data.getTierId()) {
            return;
        }
        List<SeasonTierCfg> tiers = configService.tiers(data.seasonPhase());
        int currentIndex = indexOf(tiers, data.getTierId());
        int targetIndex = indexOf(tiers, targetTier.getId());
        if (targetIndex < 0 || (currentIndex >= 0 && targetIndex < currentIndex)) {
            return;
        }
        for (int index = currentIndex + 1; index <= targetIndex; index++) {
            grantTierReward(ctx, data, tiers.get(index));
        }
        int oldTierId = data.getTierId();
        data.setTierId(targetTier.getId());
        NotifySeasonTierUp notify = new NotifySeasonTierUp(Code.SUCCESS);
        notify.oldTierId = oldTierId;
        notify.newTierId = targetTier.getId();
        notify.totalEarnedCoin = data.getTotalEarnedCoin();
        notify.nextTierNeedCoin = nextTierNeedCoin(tiers, targetIndex);
        socialSender.sendTo(ctx.playerId(), notify);
    }

    /**
     * 下一段位的累计赛季币门槛 (RankRange 下限); 已是最高段位时返回 0。
     */
    private long nextTierNeedCoin(List<SeasonTierCfg> tiers, int targetIndex) {
        int nextIndex = targetIndex + 1;
        if (nextIndex >= tiers.size()) {
            return 0L;
        }
        List<Integer> range = tiers.get(nextIndex).getRankRange();
        return range == null || range.isEmpty() ? 0L : range.get(0);
    }

    private void grantTierReward(SimPlayerContext ctx, SeasonPlayerData data, SeasonTierCfg tier) {
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
            var result = playerPackService.addItems(ctx.playerId(), packReward, AddType.ACTIVITY,
                    "season-tier:" + tier.getId(), true);
            if (!result.success()) {
                log.warn("赛季段位奖励发放失败 playerId={},tierId={},code={}",
                        ctx.playerId(), tier.getId(), result.code);
            }
        }
    }

    private int indexOf(List<SeasonTierCfg> tiers, int tierId) {
        for (int index = 0; index < tiers.size(); index++) {
            if (tiers.get(index).getId() == tierId) {
                return index;
            }
        }
        return -1;
    }
}
