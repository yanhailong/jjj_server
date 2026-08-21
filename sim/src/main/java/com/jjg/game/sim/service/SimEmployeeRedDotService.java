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
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 雇员入口及其卡池、游客、雇员页签的红点判定。 */
@Service
public class SimEmployeeRedDotService implements IRedDotService, ItemAddListener, ItemConsumeListener,
        SimPlayerTickListener {
    private static final Logger log = LoggerFactory.getLogger(SimEmployeeRedDotService.class);
    private static final List<Integer> RED_DOT_SUBMODULES = List.of(
            SimConstant.Employee.RED_DOT_RECRUIT_POOL,
            SimConstant.Employee.RED_DOT_GUEST_STAR_UP,
            SimConstant.Employee.RED_DOT_EMPLOYEE_GROWTH);
    private static final Set<AddType> MANAGED_ADD_TYPES = EnumSet.of(
            AddType.SIM_GUEST_RECRUIT,
            AddType.SIM_EMPLOYEE_RECRUIT,
            AddType.SIM_GUEST_STAR_UP,
            AddType.SIM_EMPLOYEE_STAR_UP,
            AddType.SIM_EMPLOYEE_LEVEL_UP);

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
        List<RedDotDetails> details = new ArrayList<>(submodules.size());
        for (int currentSubmodule : submodules) {
            Collection<Map<Integer, Long>> requirements = switch (currentSubmodule) {
                case SimConstant.Employee.RED_DOT_RECRUIT_POOL -> poolSnapshot.requirements();
                case SimConstant.Employee.RED_DOT_GUEST_STAR_UP -> guestStarRequirements(playerId, ctx);
                case SimConstant.Employee.RED_DOT_EMPLOYEE_GROWTH -> employeeGrowthRequirements(playerId, ctx);
                default -> null;
            };
            if (requirements == null) {
                continue;
            }
            int count = playerPackService.checkHasAnyItems(player, requirements) ? 1 : 0;
            details.add(redDotManager.buildRedDotDetails(getModule(), currentSubmodule, count));
        }
        if (ctx != null && includesPool) {
            ctx.setEmployeePoolRedDotVersion(poolSnapshot.version());
        }
        return details;
    }

    @Override
    public void onTick(SimPlayerContext ctx, long now) {
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
        if (ctx == null || items == null || items.isEmpty()
                || (addType != null && MANAGED_ADD_TYPES.contains(addType))) {
            return;
        }
        Set<Integer> changedItemIds = items.keySet();
        List<Integer> affected = new ArrayList<>(RED_DOT_SUBMODULES.size());
        if (!Collections.disjoint(currentActivePools(System.currentTimeMillis()).itemIds(), changedItemIds)) {
            affected.add(SimConstant.Employee.RED_DOT_RECRUIT_POOL);
        }
        if (requirementsUseItems(guestStarRequirements(playerId, ctx), changedItemIds)) {
            affected.add(SimConstant.Employee.RED_DOT_GUEST_STAR_UP);
        }
        if (requirementsUseItems(employeeGrowthRequirements(playerId, ctx), changedItemIds)) {
            affected.add(SimConstant.Employee.RED_DOT_EMPLOYEE_GROWTH);
        }
        if (!affected.isEmpty()) {
            updateRedDots(playerId, affected);
        }
    }

    public void updateRedDots(long playerId, int... submodules) {
        List<Integer> updates = new ArrayList<>(submodules.length);
        for (int submodule : submodules) {
            updates.add(submodule);
        }
        updateRedDots(playerId, updates);
    }

    private void updateRedDots(long playerId, List<Integer> submodules) {
        try {
            redDotManager.updateRedDotByInitialize(getModule(), submodules, playerId);
        } catch (Exception e) {
            log.error("刷新雇员红点异常 playerId={},submodules={}", playerId, submodules, e);
        }
    }

    private Collection<Map<Integer, Long>> guestStarRequirements(long playerId, SimPlayerContext ctx) {
        SimCasinoData casino = ctx == null ? null : ctx.getCurrentCasino();
        if (casino == null) {
            int casinoId = simPlayerGameDao.findCurrentCasinoId(playerId);
            casino = simCasinoDao.findGuestGrowthRedDotData(playerId, casinoId);
        }
        if (casino == null || casino.getGuestMap() == null || casino.getGuestMap().isEmpty()) {
            return List.of();
        }

        List<Map<Integer, Long>> requirements = new ArrayList<>();
        for (GuestData guest : casino.getGuestMap().values()) {
            VisitorStarCfg currentCfg = configCache.getVisitorStarCfgByGuest(guest.getId(), guest.getStar());
            VisitorStarCfg nextCfg = configCache.getVisitorStarCfgByGuest(guest.getId(), guest.getStar() + 1);
            VisitorQuestCfg guestCfg = GameDataManager.getVisitorQuestCfg(guest.getId());
            List<Integer> shard = guestCfg == null ? null : guestCfg.getDuplicatetoShard();
            if (currentCfg != null && nextCfg != null && shard != null && shard.size() > 1) {
                requirements.add(Map.of(shard.get(1), (long) currentCfg.getAscend()));
            }
        }
        return requirements;
    }

    private Collection<Map<Integer, Long>> employeeGrowthRequirements(long playerId, SimPlayerContext ctx) {
        Collection<SimEmployeeData> employees = ctx == null
                ? simEmployeeDao.findGrowthRedDotData(playerId)
                : ctx.getEmployeeMap().values();
        if (employees == null || employees.isEmpty()) {
            return List.of();
        }

        List<Map<Integer, Long>> requirements = new ArrayList<>();
        for (SimEmployeeData employee : employees) {
            EmployeeStarCfg starCfg = getEmployeeStarCfg(employee.getEmployeeId(), employee.getStar());
            if (starCfg != null && starCfg.getStarUpCost() > 0) {
                EmployeeProfileCfg profileCfg = GameDataManager.getEmployeeProfileCfg(employee.getEmployeeId());
                List<Integer> shard = profileCfg == null ? null : profileCfg.getDuplicatetoShard();
                if (shard != null && shard.size() > 1) {
                    requirements.add(Map.of(shard.get(1), (long) starCfg.getStarUpCost()));
                }
            }

            int levelCap = starCfg == null ? 0 : starCfg.getLevelCap();
            if (levelCap > 0 && employee.getLevel() >= levelCap) {
                continue;
            }
            EmployeeLevelCfg nextLevelCfg = getEmployeeLevelCfg(employee.getEmployeeId(), employee.getLevel() + 1);
            if (nextLevelCfg != null) {
                Map<Integer, Long> cost = nextLevelCfg.getUpgradeCost();
                requirements.add(cost == null ? Map.of() : cost);
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

    private boolean requirementsUseItems(Collection<Map<Integer, Long>> requirements, Set<Integer> itemIds) {
        for (Map<Integer, Long> requirement : requirements) {
            if (requirement != null && !Collections.disjoint(requirement.keySet(), itemIds)) {
                return true;
            }
        }
        return false;
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
            LinkedHashSet<Map<Integer, Long>> requirements = new LinkedHashSet<>();
            addOpenPoolRequirements(requirements, SimConstant.PoolList.TYPE_GUEST);
            addOpenPoolRequirements(requirements, SimConstant.PoolList.TYPE_EMPLOYEE);
            List<Map<Integer, Long>> currentRequirements = List.copyOf(requirements);
            if (!currentRequirements.equals(activePoolSnapshot.requirements())) {
                Set<Integer> itemIds = new LinkedHashSet<>();
                currentRequirements.forEach(requirement -> itemIds.addAll(requirement.keySet()));
                activePoolSnapshot = new ActivePoolSnapshot(activePoolSnapshot.version() + 1,
                        currentRequirements, Set.copyOf(itemIds));
            }
            activePoolScanSecond = scanSecond;
            return activePoolSnapshot;
        }
    }

    private void addOpenPoolRequirements(Set<Map<Integer, Long>> requirements, int poolType) {
        for (RecruitPoolInfo poolInfo : configCache.getOpenPoolIds(poolType)) {
            PoolListCfg poolCfg = configCache.getOpenPoolCfg(poolInfo.id, poolType);
            boolean validDrop = poolCfg != null && (poolType == SimConstant.PoolList.TYPE_GUEST
                    ? configCache.getPoolRand(poolCfg.getDropItem()) != null
                    : configCache.getEmployeePoolRand(poolCfg.getDropItem()) != null);
            if (validDrop && poolCfg.getDrawCost() != null && !poolCfg.getDrawCost().isEmpty()) {
                requirements.add(Collections.unmodifiableMap(new HashMap<>(poolCfg.getDrawCost())));
            }
        }
    }

    private record ActivePoolSnapshot(long version, List<Map<Integer, Long>> requirements, Set<Integer> itemIds) {
        private static ActivePoolSnapshot empty() {
            return new ActivePoolSnapshot(0, List.of(), Set.of());
        }
    }
}
