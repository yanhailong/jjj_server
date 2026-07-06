package com.jjg.game.sim.service;

import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.PlayerPack;
import com.jjg.game.core.data.RankChange;
import com.jjg.game.core.data.RankEntry;
import com.jjg.game.core.service.PlayerPackService;
import com.jjg.game.core.service.RankService;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.ItemCfg;
import com.jjg.game.sampledata.bean.MedalBuffCfg;
import com.jjg.game.sampledata.bean.MedalListCfg;
import com.jjg.game.sim.constant.BonusType;
import com.jjg.game.sim.constant.SimConstant;
import com.jjg.game.sim.data.SimPlayerContext;
import com.jjg.game.sim.pb.res.ResMedalPanel;
import com.jjg.game.sim.pb.struct.MedalQualityInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 成就勋章服务: 勋章激活判定、品质统计、全服排行、品质加成 (MedalBuff)。
 * <p>
 * 勋章"激活"= 玩家背包持有 {@link MedalListCfg#getNeedItemId()} 勋章道具 (成就任务链末节点奖励);
 * 勋章"品质"取自勋章道具 {@link ItemCfg#getQuality()} (1精英/2大亨/3富翁/4神豪)。
 * 全服排行以"已激活勋章数"为分值, 惰性写入 (仅打开面板/领奖后且分值变化时更新), 避免登录全量写 Redis。
 *
 * @author 11
 * @date 2026/7/2
 */
@Service
public class SimMedalService {
    private static final Logger log = LoggerFactory.getLogger(SimMedalService.class);

    //全服勋章榜 (累计, 不分赛季不重置); 分值=已激活勋章数
    private static final String MEDAL_RANK_KEY = "sim:medal:rank";
    //勋章品质区间: 1精英 2大亨 3富翁 4神豪
    private static final int QUALITY_MIN = 1;
    private static final int QUALITY_MAX = 4;
    private static final int PERMIL_BASE = 10000;

    //勋章加成指向 condition 表 id (加成属性定义, 单位千分比 ‰; value/1000 = 倍率, 与雇员加成同基数)
    private static final int COND_REST_POWER = 12404;    //休息建筑-能量增加收益
    private static final int COND_GOLD_BUILDING = 12405;  //所有产金币的建筑增加收益
    private static final int COND_AWARENESS = 12406;      //知名度增加
    private static final int COND_AD_GOLD = 12408;        //广告金币收益增加

    @Autowired
    private PlayerPackService playerPackService;
    @Autowired
    private RankService rankService;

    // =====================================================================
    // 勋章激活
    // =====================================================================

    /**
     * 玩家已激活勋章配置id列表 (按配置顺序)。激活 = 背包持有该勋章的 NeedItemId 道具。
     */
    public List<Integer> getActivatedMedalIds(long playerId) {
        List<MedalListCfg> configs = GameDataManager.getMedalListCfgList();
        if (configs == null || configs.isEmpty()) {
            return Collections.emptyList();
        }
        PlayerPack pack = playerPackService.getFromAllDB(playerId);
        if (pack == null) {
            return Collections.emptyList();
        }
        List<Integer> result = new ArrayList<>();
        for (MedalListCfg cfg : configs) {
            if (isActivated(cfg, pack)) {
                result.add(cfg.getId());
            }
        }
        return result;
    }

    private boolean isActivated(MedalListCfg cfg, PlayerPack pack) {
        return cfg != null && cfg.getIsOpen() && cfg.getNeedItemId() > 0
                && pack.getItemCount(cfg.getNeedItemId()) > 0;
    }

    /**
     * 勋章品质 (取自勋章道具品质); 配置缺失返回 0。
     */
    private int qualityOf(MedalListCfg cfg) {
        if (cfg == null || cfg.getNeedItemId() <= 0) {
            return 0;
        }
        ItemCfg item = GameDataManager.getItemCfg(cfg.getNeedItemId());
        return item == null ? 0 : item.getQuality();
    }

    // =====================================================================
    // 勋章面板 (成就-勋章子页)
    // =====================================================================

    /**
     * 组装勋章面板: 已激活列表 + 达成统计 + 各品质统计与加成档 + 全服排名百分比。
     * 单次背包读取内完成激活判定与品质归类, 避免多次查库。
     */
    public ResMedalPanel buildMedalPanel(SimPlayerContext ctx) {
        ResMedalPanel res = new ResMedalPanel(Code.SUCCESS);
        long playerId = ctx.playerId();
        List<MedalListCfg> configs = GameDataManager.getMedalListCfgList();
        List<Integer> activated = new ArrayList<>();
        int[] qualityCount = new int[QUALITY_MAX + 1];
        int total = 0;
        if (configs != null && !configs.isEmpty()) {
            PlayerPack pack = playerPackService.getFromAllDB(playerId);
            for (MedalListCfg cfg : configs) {
                if (cfg == null || !cfg.getIsOpen()) {
                    continue;
                }
                total++;
                if (pack != null && isActivated(cfg, pack)) {
                    activated.add(cfg.getId());
                    int q = qualityOf(cfg);
                    if (q >= QUALITY_MIN && q <= QUALITY_MAX) {
                        qualityCount[q]++;
                    }
                }
            }
        }
        res.activatedMedalIds = activated;
        res.totalMedalCount = total;
        res.qualityInfos = buildQualityInfos(qualityCount);
        res.rankPermil = syncAndGetRankPermil(playerId, activated.size());
        res.medalShowMax = GameDataManager.getGlobalConfigCfg(SimConstant.Common.MEDAL_SHOW_MAX_ID).getIntValue();
        return res;
    }

    /**
     * 按品质输出统计与当前激活的加成档 (MedalBuff)。始终输出 4 个品质, 便于客户端固定展示。
     */
    private List<MedalQualityInfo> buildQualityInfos(int[] qualityCount) {
        List<MedalQualityInfo> list = new ArrayList<>(QUALITY_MAX);
        for (int q = QUALITY_MIN; q <= QUALITY_MAX; q++) {
            MedalQualityInfo info = new MedalQualityInfo();
            info.quality = q;
            info.collectedCount = qualityCount[q];
            info.activatedBuffCfgId = activatedBuffCfg(q, qualityCount[q]);
            list.add(info);
        }
        return list;
    }

    /**
     * 某品质在已收集 collected 个勋章时激活的加成档: 取 MedalBuff 中该品质、CollectNum≤collected 的最高档。
     *
     * @return 激活的 MedalBuff 配置id; 未达任何档返回 0
     */
    private int activatedBuffCfg(int quality, int collected) {
        List<MedalBuffCfg> buffs = GameDataManager.getMedalBuffCfgList();
        if (buffs == null || collected <= 0) {
            return 0;
        }
        int bestCfgId = 0;
        int bestCollectNum = -1;
        for (MedalBuffCfg buff : buffs) {
            if (buff == null || buff.getMedalType() != quality) {
                continue;
            }
            if (buff.getCollectNum() <= collected && buff.getCollectNum() > bestCollectNum) {
                bestCollectNum = buff.getCollectNum();
                bestCfgId = buff.getId();
            }
        }
        return bestCfgId;
    }

    // =====================================================================
    // 全服排行 (已激活勋章数)
    // =====================================================================

    /**
     * 领奖后勋章数可能变化时惰性刷新榜单分值 (仅在与现有分值不一致时写入)。
     */
    public void refreshRankScore(long playerId) {
        try {
            syncRankScore(playerId, getActivatedMedalIds(playerId).size());
        } catch (Exception e) {
            log.warn("刷新勋章榜分值失败 playerId={}", playerId, e);
        }
    }

    /**
     * 同步榜单分值并返回"全服排名超过"的万分比。分值不一致才写入, 0 分不上榜。
     *
     * @return 万分比 (9000 = 超过 90.00%)
     */
    private int syncAndGetRankPermil(long playerId, int activatedCount) {
        syncRankScore(playerId, activatedCount);
        if (activatedCount <= 0) {
            return 0;
        }
        RankEntry my = rankService.getRank(MEDAL_RANK_KEY, playerId);
        int total = rankService.size(MEDAL_RANK_KEY);
        if (my == null || total <= 0) {
            return 0;
        }
        //(1 - 名次/总人数) * 100%, 名次从1开始
        double ratio = 1.0 - (double) my.getRank() / total;
        int permil = (int) Math.round(ratio * PERMIL_BASE);
        return Math.max(0, Math.min(PERMIL_BASE, permil));
    }

    /**
     * 仅当榜单现有分值与目标不同才覆盖写入; 目标为 0 不上榜 (避免 0 分成员污染排名基数)。
     */
    private void syncRankScore(long playerId, int activatedCount) {
        if (activatedCount <= 0) {
            return;
        }
        long cur = rankService.getPoints(MEDAL_RANK_KEY, playerId);
        if (cur != activatedCount) {
            rankService.batchSetPoints(MEDAL_RANK_KEY, List.of(new RankChange(playerId, activatedCount)));
        }
    }

    // =====================================================================
    // 品质加成 (MedalBuff) —— 加成生效
    // =====================================================================

    /**
     * 计算玩家当前由勋章品质加成激活的效果 (condition 表 id -> 千分比值), 供收益计算取用。
     * 每品质取"达标最高档", 合并其 {@code MedalBuff.BuffId} (key 指向 condition 表的加成属性定义)。
     * 例: 精英收集5→12405(产金币建筑收益+1000‰), 神豪收集8→12408(广告金币收益+2000‰)。
     */
    public Map<Integer, Integer> calcActiveMedalBuffs(long playerId) {
        Map<Integer, Integer> merged = new HashMap<>();
        List<MedalListCfg> configs = GameDataManager.getMedalListCfgList();
        if (configs == null || configs.isEmpty()) {
            return merged;
        }
        PlayerPack pack = playerPackService.getFromAllDB(playerId);
        if (pack == null) {
            return merged;
        }
        int[] qualityCount = new int[QUALITY_MAX + 1];
        for (MedalListCfg cfg : configs) {
            if (isActivated(cfg, pack)) {
                int q = qualityOf(cfg);
                if (q >= QUALITY_MIN && q <= QUALITY_MAX) {
                    qualityCount[q]++;
                }
            }
        }
        for (int q = QUALITY_MIN; q <= QUALITY_MAX; q++) {
            int cfgId = activatedBuffCfg(q, qualityCount[q]);
            if (cfgId <= 0) {
                continue;
            }
            MedalBuffCfg buff = GameDataManager.getMedalBuffCfg(cfgId);
            if (buff != null && buff.getBuffId() != null) {
                buff.getBuffId().forEach((buffId, value) -> merged.merge(buffId, value, Integer::sum));
            }
        }
        return merged;
    }

    /**
     * 刷新玩家勋章加成缓存 (登录 / 成就领奖后调用)。缓存供收益计算零 IO 读取, 避免高频查背包。
     */
    public void refreshMedalBonusCache(SimPlayerContext ctx) {
        if (ctx == null) {
            return;
        }
        try {
            ctx.setMedalBuffMap(calcActiveMedalBuffs(ctx.playerId()));
        } catch (Exception e) {
            log.warn("刷新勋章加成缓存失败 playerId={}", ctx.playerId(), e);
        }
    }

    /**
     * 把玩家勋章品质加成 (缓存) 合并进 sim 加成汇总 Map, 仅合并可映射到 {@link BonusType} 的项
     * (知名度/休息区能量/产金币建筑); 单位千分比, 与雇员加成同基数, 由各产出点统一按 /1000 生效。
     * 广告金币收益 (12408) 无对应 BonusType, 由离线收益领取点单独取用。
     */
    public void mergeMedalBonus(SimPlayerContext ctx, Map<BonusType, Integer> bonusesMap) {
        if (ctx == null || bonusesMap == null) {
            return;
        }
        Map<Integer, Integer> medal = ctx.getMedalBuffMap();
        if (medal == null || medal.isEmpty()) {
            return;
        }
        for (Map.Entry<Integer, Integer> en : medal.entrySet()) {
            BonusType type = bonusTypeOf(en.getKey());
            if (type != null && en.getValue() != null) {
                bonusesMap.merge(type, en.getValue(), Integer::sum);
            }
        }
    }

    /**
     * 勋章-广告金币收益加成 (千分比, condition 12408); 无对应 BonusType, 看广告领取离线收益时叠加到金币部分。
     */
    public int getAdGoldBonusPermil(SimPlayerContext ctx) {
        if (ctx == null) {
            return 0;
        }
        Map<Integer, Integer> medal = ctx.getMedalBuffMap();
        Integer v = medal == null ? null : medal.get(COND_AD_GOLD);
        return v == null ? 0 : v;
    }

    /**
     * 勋章加成 condition id -> sim 加成体系 {@link BonusType}; 无对应 (如广告 12408/曝光度 12407) 返回 null。
     */
    private static BonusType bonusTypeOf(int conditionId) {
        return switch (conditionId) {
            case COND_AWARENESS -> BonusType.AWARENESS;
            case COND_REST_POWER -> BonusType.REST_AREA;
            case COND_GOLD_BUILDING -> BonusType.GAME_AREA;
            default -> null;
        };
    }
}
