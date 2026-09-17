package com.jjg.game.sim.service;

import com.alibaba.fastjson.JSON;
import com.jjg.game.core.base.reddot.IRedDotService;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.listener.ItemAddListener;
import com.jjg.game.core.listener.ItemConsumeListener;
import com.jjg.game.core.manager.RedDotManager;
import com.jjg.game.core.pb.reddot.RedDotDetails;
import com.jjg.game.core.service.PlayerPackService;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BuildingAreaTableCfg;
import com.jjg.game.sim.data.SimPlayerContext;
import com.jjg.game.sim.data.SimSkillsData;
import com.jjg.game.sim.listener.SimPlayerTickListener;
import com.jjg.game.sim.manager.SimPlayerContextRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import java.util.*;

/** 建筑升级按建筑计数；没有可升建筑时，可升级技能保留一个入口提示；extra提供按钮明细。 */
@Service
public class SimBuildingRedDotService implements IRedDotService, SimPlayerTickListener, ItemAddListener, ItemConsumeListener {
    private static final Logger log = LoggerFactory.getLogger(SimBuildingRedDotService.class);
    @Autowired private SimPlayerContextRegistry contexts;
    @Lazy @Autowired private SimBuildingService buildings;
    @Lazy @Autowired private SimSkillService skills;
    @Lazy @Autowired private PlayerPackService packs;
    @Autowired private RedDotManager manager;

    @Override public RedDotDetails.RedDotModule getModule() { return RedDotDetails.RedDotModule.BUILDING; }
    @Override public List<Integer> getSubmodules() { return List.of(0, 1); }

    @Override public List<RedDotDetails> initialize(long id, int submodule) {
        if (submodule != 0 && submodule != 1) return List.of();
        SimPlayerContext ctx = contexts.getContext(id);
        // 未加载模拟经营上下文时不发伪造的0，进入场景后由tick补发。
        return ctx == null || ctx.getPlayer() == null || ctx.getCurrentCasino() == null
                || ctx.getCurrentCasino().getBuildingData() == null ? List.of() : calculate(ctx, true);
    }

    private List<RedDotDetails> calculate(SimPlayerContext ctx, boolean force) {
        List<Offer> offers = new ArrayList<>();
        SimSkillsData globalSkills = ctx.getSkillData(0);
        ctx.getCurrentCasino().getBuildingData().keySet().stream().sorted().forEach(id -> {
            Map<Integer, Long> cost = buildings.redDotUpgradeCost(ctx, id);
            if (cost != null) offers.add(new Offer(id, 0, cost));
            BuildingAreaTableCfg cfg = GameDataManager.getBuildingAreaTableCfg(id);
            if (hasSkillEntry(cfg)) {
                // 技能页同时展示当前游戏技能和全局技能；只映射到真正有技能入口的建筑。
                addSkillOffers(offers, ctx, id, ctx.getSkillData(cfg.getUnlockGameId()));
                addSkillOffers(offers, ctx, id, globalSkills);
            }
        });
        // 内存条件未变化且没有道具事件，不重复读背包；60秒兜底后台改数据。
        String input = offers.toString() + ":" + ctx.getCurrentCasino().getCasinoId() + ":"
                + ctx.getPlayer().getGold() + ":" + ctx.getPlayer().getDiamond() + ":" + ctx.getPlayer().getShell();
        long now = System.currentTimeMillis();
        if (!force && !ctx.isBuildingRedDotDirty() && input.equals(ctx.getBuildingRedDotInput())
                && now - ctx.getBuildingRedDotCheckTime() < 60_000) return List.of();
        Set<Integer> eligible = packs.findSatisfiedItemRequirements(ctx.getPlayer(), offers.stream().map(Offer::cost).toList());
        Set<Integer> ids = new TreeSet<>(), buildingIds = new TreeSet<>();
        Map<Integer, Set<Integer>> skillIds = new TreeMap<>();
        for (int i : eligible) {
            Offer offer = offers.get(i);
            ids.add(offer.buildingId());
            if (offer.propId() == 0) buildingIds.add(offer.buildingId());
            else skillIds.computeIfAbsent(offer.buildingId(), key -> new TreeSet<>()).add(offer.propId());
        }
        int skillCount = skillIds.isEmpty() ? 0 : 1;
        int count = upgradeEntryCount(buildingIds.size(), skillCount);
        RedDotDetails dot = manager.buildRedDotDetails(getModule(), 1, count, RedDotDetails.RedDotType.COUNT);
        dot.setExtra(JSON.toJSONString(Map.of("ids", ids, "buildingIds", buildingIds, "skillIds", skillIds,
                "buildingCount", buildingIds.size(), "skillCount", skillCount,
                "casinoId", ctx.getCurrentCasino().getCasinoId())));
        String snapshot = JSON.toJSONString(dot);
        boolean changed = !snapshot.equals(ctx.getBuildingRedDotSnapshot());
        if (changed) {
            log.info("建筑升级红点刷新 playerId={},casinoId={},count={},buildingIds={},skillIds={}",
                    ctx.playerId(), ctx.getCurrentCasino().getCasinoId(), count, buildingIds, skillIds);
        }
        ctx.setBuildingRedDotDirty(false);
        ctx.setBuildingRedDotInput(input);
        ctx.setBuildingRedDotCheckTime(now);
        ctx.setBuildingRedDotSnapshot(snapshot);
        return force || changed ? List.of(dot) : List.of();
    }

    static int upgradeEntryCount(int buildingCount, int skillCount) {
        return buildingCount > 0 ? buildingCount : skillCount;
    }

    static boolean hasSkillEntry(BuildingAreaTableCfg cfg) {
        return cfg != null && cfg.getUnlockGameId() > 0;
    }

    private void addSkillOffers(List<Offer> offers, SimPlayerContext ctx, int buildingId, SimSkillsData data) {
        if (data == null || data.getSkillsMap() == null) return;
        data.getSkillsMap().keySet().stream().sorted().forEach(propId -> {
            Map<Integer, Long> cost = skills.redDotUpgradeCost(ctx, data.getGameType(), propId);
            if (cost != null) offers.add(new Offer(buildingId, propId, cost));
        });
    }

    @Override public void onTick(SimPlayerContext ctx, long now) {
        if (ctx.getPlayer() == null || ctx.getCurrentCasino() == null || ctx.getCurrentCasino().getBuildingData() == null) return;
        try { manager.updateRedDot(calculate(ctx, false), ctx.playerId()); }
        catch (Exception e) { org.slf4j.LoggerFactory.getLogger(getClass()).error("刷新建筑红点失败 playerId={}", ctx.playerId(), e); }
    }

    public void invalidate(long id) {
        SimPlayerContext ctx = contexts.getContext(id);
        if (ctx != null) ctx.setBuildingRedDotDirty(true);
    }
    public void refresh(SimPlayerContext ctx) {
        if (ctx.getPlayer() == null || ctx.getCurrentCasino() == null
                || ctx.getCurrentCasino().getBuildingData() == null) return;
        manager.updateRedDot(calculate(ctx, true), ctx.playerId());
    }
    @Override public void onItemsAdded(long id, Map<Integer, Long> items, AddType type) { invalidate(id); }
    @Override public void onItemsConsumed(long id, Map<Integer, Long> items, AddType type) { invalidate(id); }
    private record Offer(int buildingId, int propId, Map<Integer, Long> cost) {}
}
