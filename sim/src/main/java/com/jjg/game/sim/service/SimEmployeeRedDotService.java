package com.jjg.game.sim.service;

import com.jjg.game.core.base.reddot.IRedDotService;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.listener.ItemAddListener;
import com.jjg.game.core.listener.ItemConsumeListener;
import com.jjg.game.core.manager.RedDotManager;
import com.jjg.game.core.pb.reddot.RedDotDetails;
import com.jjg.game.core.service.CorePlayerService;
import com.jjg.game.core.service.PlayerPackService;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.EmployeeLevelCfg;
import com.jjg.game.sampledata.bean.EmployeeProfileCfg;
import com.jjg.game.sampledata.bean.EmployeeStarCfg;
import com.jjg.game.sampledata.bean.PoolListCfg;
import com.jjg.game.sampledata.bean.VisitorBondsCfg;
import com.jjg.game.sampledata.bean.VisitorQuestCfg;
import com.jjg.game.sampledata.bean.VisitorStarCfg;
import com.jjg.game.sim.constant.SimConstant;
import com.jjg.game.sim.dao.SimCasinoDao;
import com.jjg.game.sim.dao.SimEmployeeDao;
import com.jjg.game.sim.dao.SimPlayerGameDao;
import com.jjg.game.sim.data.GuestData;
import com.jjg.game.sim.data.SimCasinoData;
import com.jjg.game.sim.data.SimEmployeeData;
import com.jjg.game.sim.data.SimPlayerContext;
import com.jjg.game.sim.listener.SimPlayerTickListener;
import com.jjg.game.sim.manager.SimPlayerContextRegistry;
import com.jjg.game.sim.pb.struct.RecruitPoolInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 雇员入口及其卡池、游客、雇员页签的红点判定。 */
@Service
public class SimEmployeeRedDotService implements IRedDotService, ItemAddListener, ItemConsumeListener,
        SimPlayerTickListener {
    public static final int NEW_EMPLOYEE = 4;
    public static final int NEW_BOND = 5;
    private static final String GUEST_POOL_IDS = "guestPoolIds";
    private static final String EMPLOYEE_POOL_IDS = "employeePoolIds";
    private static final Logger log = LoggerFactory.getLogger(SimEmployeeRedDotService.class);
    private static final List<Integer> RED_DOT_SUBMODULES = List.of(
            SimConstant.Employee.RED_DOT_RECRUIT_POOL,
            SimConstant.Employee.RED_DOT_GUEST_STAR_UP,
            SimConstant.Employee.RED_DOT_EMPLOYEE_GROWTH, NEW_EMPLOYEE, NEW_BOND,
            SimConstant.Employee.RED_DOT_VISITOR_ENTRY);

    @Autowired
    private SimPlayerContextRegistry contextRegistry;
    @Autowired
    private SimPlayerGameDao simPlayerGameDao;
    @Autowired
    private SimCasinoDao simCasinoDao;
    @Autowired
    private SimEmployeeDao simEmployeeDao;
    @Autowired
    private SimConfigCacheService configCache;
    @Autowired
    private CorePlayerService corePlayerService;
    @Lazy
    @Autowired
    private PlayerPackService playerPackService;
    @Autowired
    private RedDotManager redDotManager;
    @Autowired
    private com.jjg.game.core.dao.RedDotReadDao redDotReadDao;

    private volatile ActivePoolSnapshot activePoolSnapshot = ActivePoolSnapshot.empty();
    private volatile long activePoolScanSecond = -1;

    @Override
    public RedDotDetails.RedDotModule getModule() {
        return RedDotDetails.RedDotModule.EMPLOYEE;
    }

    @Override
    public List<Integer> getSubmodules() {
        return RED_DOT_SUBMODULES;
    }

    @Override
    public List<RedDotDetails> initialize(long playerId, int submodule) {
        List<Integer> submodules = submodule == 0 ? RED_DOT_SUBMODULES : List.of(submodule);
        if (submodule != 0 && !RED_DOT_SUBMODULES.contains(submodule)) {
            return List.of();
        }
        SimPlayerContext ctx = contextRegistry.getContext(playerId);
        Player player = corePlayerService.get(playerId);

        boolean includesPool = submodules.contains(SimConstant.Employee.RED_DOT_RECRUIT_POOL);
        ActivePoolSnapshot poolSnapshot = includesPool ? currentActivePools(System.currentTimeMillis()) : null;
        boolean includesEmployeeSummary = submodules.contains(SimConstant.Employee.RED_DOT_EMPLOYEE_GROWTH)
                || submodules.contains(NEW_EMPLOYEE);
        Set<Integer> unreadEmployeeIds = includesEmployeeSummary
                ? redDotReadDao.unread(playerId, readScope(playerId, ctx, NEW_EMPLOYEE))
                : Set.of();
        if (unreadEmployeeIds == null) {
            unreadEmployeeIds = Set.of();
        }
        List<RedDotDetails> details = new ArrayList<>(submodules.size());
        for (int currentSubmodule : submodules) {
            if (currentSubmodule == SimConstant.Employee.RED_DOT_VISITOR_ENTRY) {
                details.add(buildVisitorEntryRedDot(playerId, ctx, player));
                continue;
            }
            if (currentSubmodule == NEW_EMPLOYEE || currentSubmodule == NEW_BOND) {
                Set<Integer> ids = currentSubmodule == NEW_EMPLOYEE
                        ? unreadEmployeeIds
                        : redDotReadDao.unread(playerId, readScope(playerId, ctx, currentSubmodule));
                RedDotDetails dot = redDotManager.buildRedDotDetails(getModule(), currentSubmodule, ids.isEmpty() ? 0 : 1);
                dot.setExtra(com.alibaba.fastjson.JSON.toJSONString(Map.of("ids", ids.stream().sorted().toList())));
                details.add(dot);
                continue;
            }
            List<Requirement> requirements = switch (currentSubmodule) {
                case SimConstant.Employee.RED_DOT_RECRUIT_POOL -> poolSnapshot.requirements();
                case SimConstant.Employee.RED_DOT_GUEST_STAR_UP -> guestStarRequirements(playerId, ctx);
                case SimConstant.Employee.RED_DOT_EMPLOYEE_GROWTH -> employeeGrowthRequirements(playerId, ctx);
                default -> null;
            };
            if (requirements == null) {
                continue;
            }
            Set<Integer> satisfied = playerPackService.findSatisfiedItemRequirements(player,
                    requirements.stream().map(Requirement::cost).toList());
            Set<Integer> ids = new java.util.TreeSet<>();
            Map<String, Set<Integer>> actions = new java.util.TreeMap<>();
            for (int i : satisfied) {
                Requirement requirement = requirements.get(i);
                ids.add(requirement.id());
                actions.computeIfAbsent(requirement.action(), key -> new java.util.TreeSet<>()).add(requirement.id());
            }
            int displayCount = ids.size();
            Map<String, Object> extra = new java.util.TreeMap<>();
            extra.putAll(actions);
            extra.put("ids", ids);
            if (currentSubmodule == SimConstant.Employee.RED_DOT_EMPLOYEE_GROWTH) {
                // 外层入口没有雇员列表上下文，只能使用count汇总；与内层页签保持同一口径，
                // 按“可培养 + 新获得”雇员ID去重计数，同时保留ids为可培养明细供卡片使用。
                Set<Integer> summaryIds = new java.util.TreeSet<>(ids);
                summaryIds.addAll(unreadEmployeeIds);
                displayCount = summaryIds.size();
                extra.put("summaryIds", summaryIds);
            }
            RedDotDetails dot = redDotManager.buildRedDotDetails(getModule(), currentSubmodule,
                    displayCount, RedDotDetails.RedDotType.COUNT);
            if (currentSubmodule == SimConstant.Employee.RED_DOT_RECRUIT_POOL) {
                Set<Integer> guestPoolIds = actions.getOrDefault(GUEST_POOL_IDS, Set.of());
                Set<Integer> employeePoolIds = actions.getOrDefault(EMPLOYEE_POOL_IDS, Set.of());
                //保留旧字段兼容总入口；新增分组字段供游客/雇员页签分别显示。
                extra.put("poolIds", ids);
                extra.put(GUEST_POOL_IDS, guestPoolIds);
                extra.put(EMPLOYEE_POOL_IDS, employeePoolIds);
                extra.put("guestPoolCount", guestPoolIds.size());
                extra.put("employeePoolCount", employeePoolIds.size());
            }
            dot.setExtra(com.alibaba.fastjson.JSON.toJSONString(extra));
            details.add(dot);
        }
        if (ctx != null && includesPool) {
            ctx.setEmployeePoolRedDotVersion(poolSnapshot.version());
        }
        return details;
    }

    @Override
    public void onTick(SimPlayerContext ctx, long now) {
        if (ctx.isEmployeeRedDotDirty()) {
            ctx.setEmployeeRedDotDirty(false);
            updateRedDots(ctx.playerId(), SimConstant.Employee.RED_DOT_RECRUIT_POOL,
                    SimConstant.Employee.RED_DOT_GUEST_STAR_UP, SimConstant.Employee.RED_DOT_EMPLOYEE_GROWTH);
        }
        ActivePoolSnapshot snapshot = currentActivePools(now);
        if (ctx.getEmployeePoolRedDotVersion() == snapshot.version()) {
            return;
        }
        ctx.setEmployeePoolRedDotVersion(snapshot.version());
        updateRedDots(ctx.playerId(), SimConstant.Employee.RED_DOT_RECRUIT_POOL);
    }

    @Override
    public void onItemsAdded(long playerId, Map<Integer, Long> items, AddType addType) {
        onPackItemsChanged(playerId, items, addType);
    }

    @Override
    public void onItemsConsumed(long playerId, Map<Integer, Long> items, AddType addType) {
        onPackItemsChanged(playerId, items, addType);
    }

    /** 普通背包变更及跨节点转发统一从这里刷新受影响的页签。 */
    public void onPackItemsChanged(long playerId, Map<Integer, Long> items, AddType addType) {
        SimPlayerContext ctx = contextRegistry.getContext(playerId);
        if (ctx == null || items == null || items.isEmpty()) {
            return;
        }
        // 扣道具回调早于等级/星级更新，延迟到同玩家下一次tick合并刷新，避免使用旧等级。
        // 不再忽略招募/升星来源：它们可能同时影响其他页签的可操作数量。
        ctx.setEmployeeRedDotDirty(true);
    }

    public void updateRedDots(long playerId, int... submodules) {
        Set<Integer> updates = new LinkedHashSet<>(submodules.length + 1);
        for (int submodule : submodules) {
            updates.add(submodule);
        }
        if (updates.contains(SimConstant.Employee.RED_DOT_GUEST_STAR_UP) || updates.contains(NEW_BOND)) {
            updates.add(SimConstant.Employee.RED_DOT_VISITOR_ENTRY);
        }
        if (updates.contains(NEW_EMPLOYEE)) {
            updates.add(SimConstant.Employee.RED_DOT_EMPLOYEE_GROWTH);
        }
        updateRedDots(playerId, new ArrayList<>(updates));
    }

    private void updateRedDots(long playerId, List<Integer> submodules) {
        try {
            redDotManager.updateRedDotByInitialize(getModule(), submodules, playerId);
        } catch (Exception e) {
            log.error("刷新雇员红点异常 playerId={},submodules={}", playerId, submodules, e);
        }
    }

    private SimCasinoData guestCasino(long playerId, SimPlayerContext ctx) {
        SimCasinoData casino = ctx == null ? null : ctx.getCurrentCasino();
        if (casino == null) {
            int casinoId = simPlayerGameDao.findCurrentCasinoId(playerId);
            casino = simCasinoDao.findGuestGrowthRedDotData(playerId, casinoId);
        }
        return casino;
    }

    private List<Requirement> guestStarRequirements(long playerId, SimPlayerContext ctx) {
        return guestStarRequirements(guestCasino(playerId, ctx));
    }

    private List<Requirement> guestStarRequirements(SimCasinoData casino) {
        if (casino == null || casino.getGuestMap() == null || casino.getGuestMap().isEmpty()) {
            return List.of();
        }

        List<Requirement> requirements = new ArrayList<>();
        for (GuestData guest : casino.getGuestMap().values()) {
            VisitorStarCfg currentCfg = configCache.getVisitorStarCfgByGuest(guest.getId(), guest.getStar());
            VisitorStarCfg nextCfg = configCache.getVisitorStarCfgByGuest(guest.getId(), guest.getStar() + 1);
            VisitorQuestCfg guestCfg = GameDataManager.getVisitorQuestCfg(guest.getId());
            List<Integer> shard = guestCfg == null ? null : guestCfg.getDuplicatetoShard();
            if (currentCfg != null && nextCfg != null && currentCfg.getAscend() > 0
                    && shard != null && shard.size() > 1 && shard.get(1) != null && shard.get(1) > 0) {
                requirements.add(new Requirement(guest.getId(), "starUpIds", Map.of(shard.get(1), (long) currentCfg.getAscend())));
            }
        }
        return requirements;
    }

    /** 游客入口按可升星或有未读羁绊的已拥有游客去重计数，与卡片红点列表一致。 */
    private RedDotDetails buildVisitorEntryRedDot(long playerId, SimPlayerContext ctx, Player player) {
        SimCasinoData casino = guestCasino(playerId, ctx);
        List<Requirement> requirements = guestStarRequirements(casino);
        Set<Integer> satisfied = playerPackService.findSatisfiedItemRequirements(player,
                requirements.stream().map(Requirement::cost).toList());
        Set<Integer> starUpIds = new java.util.TreeSet<>();
        for (int index : satisfied) {
            if (index >= 0 && index < requirements.size()) {
                starUpIds.add(requirements.get(index).id());
            }
        }
        Set<Integer> newBondIds = redDotReadDao.unread(playerId, readScope(playerId, ctx, NEW_BOND));
        Set<Integer> bondGuestIds = new java.util.TreeSet<>();
        if (casino != null && casino.getGuestMap() != null) {
            for (Integer bondId : newBondIds) {
                VisitorBondsCfg bondCfg = GameDataManager.getVisitorBondsCfg(bondId);
                if (bondCfg == null || bondCfg.getMembers() == null) {
                    continue;
                }
                for (Integer guestId : bondCfg.getMembers()) {
                    if (guestId != null && casino.getGuestMap().get(guestId) != null) {
                        bondGuestIds.add(guestId);
                    }
                }
            }
        }
        // 同一游客可同时升星、拥有多个新羁绊，页签仍只计一个红点。
        Set<Integer> ids = new java.util.TreeSet<>(starUpIds);
        ids.addAll(bondGuestIds);
        RedDotDetails dot = redDotManager.buildRedDotDetails(getModule(),
                SimConstant.Employee.RED_DOT_VISITOR_ENTRY, ids.size(), RedDotDetails.RedDotType.COUNT);
        dot.setExtra(com.alibaba.fastjson.JSON.toJSONString(Map.of(
                "ids", ids,
                "starUpIds", starUpIds,
                "bondGuestIds", bondGuestIds,
                "newBondIds", newBondIds.stream().sorted().toList())));
        return dot;
    }

    private List<Requirement> employeeGrowthRequirements(long playerId, SimPlayerContext ctx) {
        Collection<SimEmployeeData> employees = ctx == null
                ? simEmployeeDao.findGrowthRedDotData(playerId)
                : ctx.getEmployeeMap().values();
        if (employees == null || employees.isEmpty()) {
            return List.of();
        }

        List<Requirement> requirements = new ArrayList<>();
        for (SimEmployeeData employee : employees) {
            EmployeeStarCfg starCfg = getEmployeeStarCfg(employee.getEmployeeId(), employee.getStar());
            if (starCfg != null && starCfg.getStarUpCost() > 0) {
                EmployeeProfileCfg profileCfg = GameDataManager.getEmployeeProfileCfg(employee.getEmployeeId());
                List<Integer> shard = profileCfg == null ? null : profileCfg.getDuplicatetoShard();
                if (shard != null && shard.size() > 1) {
                    requirements.add(new Requirement(employee.getEmployeeId(), "starUpIds", Map.of(shard.get(1), (long) starCfg.getStarUpCost())));
                }
            }

            int levelCap = starCfg == null ? 0 : starCfg.getLevelCap();
            if (levelCap > 0 && employee.getLevel() >= levelCap) {
                continue;
            }
            EmployeeLevelCfg nextLevelCfg = getEmployeeLevelCfg(employee.getEmployeeId(), employee.getLevel() + 1);
            if (nextLevelCfg != null) {
                Map<Integer, Long> cost = nextLevelCfg.getUpgradeCost();
                requirements.add(new Requirement(employee.getEmployeeId(), "levelUpIds", cost == null ? Map.of() : cost));
            }
        }
        return requirements;
    }

    private EmployeeLevelCfg getEmployeeLevelCfg(int employeeId, int level) {
        Map<Integer, Map<Integer, EmployeeLevelCfg>> levelCfgMap = configCache.getEmployeeLevelCfgMap();
        Map<Integer, EmployeeLevelCfg> employeeCfg = levelCfgMap == null ? null : levelCfgMap.get(employeeId);
        return employeeCfg == null ? null : employeeCfg.get(level);
    }

    private EmployeeStarCfg getEmployeeStarCfg(int employeeId, int star) {
        Map<Integer, Map<Integer, EmployeeStarCfg>> starCfgMap = configCache.getEmployeeStarCfgMap();
        Map<Integer, EmployeeStarCfg> employeeCfg = starCfgMap == null ? null : starCfgMap.get(employeeId);
        return employeeCfg == null ? null : employeeCfg.get(star);
    }

    private ActivePoolSnapshot currentActivePools(long now) {
        long scanSecond = now / 1000;
        if (activePoolScanSecond == scanSecond) {
            return activePoolSnapshot;
        }
        synchronized (this) {
            if (activePoolScanSecond == scanSecond) {
                return activePoolSnapshot;
            }
            List<Requirement> requirements = new ArrayList<>();
            addOpenPoolRequirements(requirements, SimConstant.PoolList.TYPE_GUEST);
            addOpenPoolRequirements(requirements, SimConstant.PoolList.TYPE_EMPLOYEE);
            List<Requirement> currentRequirements = List.copyOf(requirements);
            if (!currentRequirements.equals(activePoolSnapshot.requirements())) {
                Set<Integer> itemIds = new LinkedHashSet<>();
                currentRequirements.forEach(requirement -> itemIds.addAll(requirement.cost().keySet()));
                activePoolSnapshot = new ActivePoolSnapshot(activePoolSnapshot.version() + 1,
                        currentRequirements, Set.copyOf(itemIds));
            }
            activePoolScanSecond = scanSecond;
            return activePoolSnapshot;
        }
    }

    private void addOpenPoolRequirements(List<Requirement> requirements, int poolType) {
        String action = poolType == SimConstant.PoolList.TYPE_GUEST ? GUEST_POOL_IDS : EMPLOYEE_POOL_IDS;
        for (RecruitPoolInfo poolInfo : configCache.getOpenPoolIds(poolType)) {
            PoolListCfg poolCfg = configCache.getOpenPoolCfg(poolInfo.id, poolType);
            boolean validDrop = poolCfg != null && (poolType == SimConstant.PoolList.TYPE_GUEST
                    ? configCache.getPoolRand(poolCfg.getDropItem()) != null
                    : configCache.getEmployeePoolRand(poolCfg.getDropItem()) != null);
            if (validDrop && poolCfg.getDrawCost() != null && !poolCfg.getDrawCost().isEmpty()) {
                requirements.add(new Requirement(poolInfo.id, action,
                        Collections.unmodifiableMap(new HashMap<>(poolCfg.getDrawCost()))));
            }
        }
    }

    private String readScope(long playerId, SimPlayerContext ctx, int submodule) {
        if (submodule == NEW_EMPLOYEE) return "newEmployee";
        int casinoId = ctx != null && ctx.getCurrentCasino() != null ? ctx.getCurrentCasino().getCasinoId()
                : simPlayerGameDao.findCurrentCasinoId(playerId);
        return "newBond:" + casinoId;
    }

    /** 获得新内容后记录未读；独立容错，避免影响招募/解锁结果。 */
    public void recordNewContent(SimPlayerContext ctx, int submodule, int id) {
        try {
            redDotReadDao.addUnread(ctx.playerId(), readScope(ctx.playerId(), ctx, submodule), List.of(id));
            updateRedDots(ctx.playerId(), submodule);
        } catch (Exception e) {
            log.error("记录新内容红点失败 playerId={},submodule={},id={}", ctx.playerId(), submodule, id, e);
        }
    }

    public void recordNewBond(SimCasinoData casino, int bondId) {
        try {
            redDotReadDao.addUnread(casino.getPlayerId(), "newBond:" + casino.getCasinoId(), List.of(bondId));
            updateRedDots(casino.getPlayerId(), NEW_BOND);
        } catch (Exception e) {
            log.error("记录新羁绊红点失败 playerId={},bondId={}", casino.getPlayerId(), bondId, e);
        }
    }

    @Override
    public boolean markRead(long playerId, int submodule, List<Integer> ids) {
        if (submodule != NEW_EMPLOYEE && submodule != NEW_BOND) return false;
        redDotReadDao.read(playerId, readScope(playerId, contextRegistry.getContext(playerId), submodule), ids);
        if (submodule == NEW_BOND) {
            updateRedDots(playerId, SimConstant.Employee.RED_DOT_VISITOR_ENTRY);
        } else if (submodule == NEW_EMPLOYEE) {
            updateRedDots(playerId, SimConstant.Employee.RED_DOT_EMPLOYEE_GROWTH);
        }
        return true;
    }

    private record Requirement(int id, String action, Map<Integer, Long> cost) {}

    private record ActivePoolSnapshot(long version, List<Requirement> requirements, Set<Integer> itemIds) {
        private static ActivePoolSnapshot empty() {
            return new ActivePoolSnapshot(0, List.of(), Set.of());
        }
    }
}
