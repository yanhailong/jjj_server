package com.jjg.game.sim.service;

import com.jjg.game.core.base.condition.numeric.PreparedCondition;
import com.jjg.game.core.service.PlayerStatService;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BuildingAreaTableCfg;
import com.jjg.game.sampledata.bean.EmployeeProfileCfg;
import com.jjg.game.sim.dao.SimCasinoDao;
import com.jjg.game.sim.data.BuildingData;
import com.jjg.game.sim.data.GuestData;
import com.jjg.game.sim.data.SimCasinoData;
import com.jjg.game.sim.data.SimCasinoUnlock;
import com.jjg.game.sim.data.SimEmployeeData;
import com.jjg.game.sim.data.SimPlayerContext;
import com.jjg.game.sim.data.SimSkillsData;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * sim 玩家统计读取。记录型从 Redis 读取，持有型按玩家当前数据实时计算。
 */
@Service
public class SimPlayerStatService {
    private final PlayerStatService playerStatService;
    private final SimCasinoDao simCasinoDao;
    private final SimSkillService simSkillService;

    public SimPlayerStatService(PlayerStatService playerStatService, SimCasinoDao simCasinoDao,
                                @Lazy SimSkillService simSkillService) {
        this.playerStatService = playerStatService;
        this.simCasinoDao = simCasinoDao;
        this.simSkillService = simSkillService;
    }

    public boolean supports(PreparedCondition condition) {
        return condition != null && PlayerStatService.supports(condition.spec().id());
    }

    public long progress(SimPlayerContext ctx, PreparedCondition condition) {
        int conditionId = condition.spec().id();
        if (PlayerStatService.recorded(conditionId)) {
            return playerStatService.progress(ctx.playerId(), condition);
        }
        return switch (conditionId) {
            case PlayerStatService.BUILDING_LEVEL -> buildingLevel(ctx,
                    condition.spec().intParameter(0));
            case PlayerStatService.BUILDING_COUNT -> buildingCount(ctx,
                    condition.spec().intParameter(1));
            case PlayerStatService.EMPLOYEE_COUNT -> employeeCount(ctx,
                    condition.spec().intParameter(0), condition.spec().intParameter(2));
            case PlayerStatService.GUEST_COUNT -> guestCount(ctx,
                    condition.spec().intParameter(1));
            case PlayerStatService.CASINO_UNLOCK -> unlockedCasinoCount(ctx);
            case PlayerStatService.SCENE_TOTAL_LEVEL -> sceneLevel(ctx,
                    condition.spec().intParameter(0));
            case PlayerStatService.SKILL_COMBAT_POWER -> combatPower(ctx,
                    condition.spec().intParameter(0));
            default -> 0;
        };
    }

    private long buildingCount(SimPlayerContext ctx, int minLevel) {
        long count = 0;
        for (SimCasinoData casino : allCasinos(ctx)) {
            if (casino.getBuildingData() == null) {
                continue;
            }
            for (BuildingData building : casino.getBuildingData().values()) {
                if (building != null && building.getLevel() >= minLevel) {
                    count++;
                }
            }
        }
        return count;
    }

    private long buildingLevel(SimPlayerContext ctx, int buildingId) {
        if (buildingId <= 0) {
            long maxLevel = 0;
            for (SimCasinoData casino : allCasinos(ctx)) {
                if (casino.getBuildingData() == null) {
                    continue;
                }
                for (BuildingData building : casino.getBuildingData().values()) {
                    if (building != null) {
                        maxLevel = Math.max(maxLevel, building.getLevel());
                    }
                }
            }
            return maxLevel;
        }
        BuildingAreaTableCfg cfg = GameDataManager.getBuildingAreaTableCfg(buildingId);
        if (cfg == null) {
            return 0;
        }
        SimCasinoData casino = casino(ctx, cfg.getRegionID());
        BuildingData building = casino == null ? null : casino.findBuilding(buildingId);
        return building == null ? 0 : building.getLevel();
    }

    private long employeeCount(SimPlayerContext ctx, int professionId, int minStar) {
        if (ctx.getEmployeeMap() == null || ctx.getEmployeeMap().isEmpty()) {
            return 0;
        }
        long count = 0;
        for (SimEmployeeData employee : ctx.getEmployeeMap().values()) {
            if (employee == null || employee.getStar() < minStar) {
                continue;
            }
            EmployeeProfileCfg cfg = GameDataManager.getEmployeeProfileCfg(employee.getEmployeeId());
            if (cfg != null && (professionId <= 0 || cfg.getProfessionID() == professionId)) {
                count++;
            }
        }
        return count;
    }

    private long guestCount(SimPlayerContext ctx, int minStar) {
        long count = 0;
        for (SimCasinoData casino : allCasinos(ctx)) {
            if (casino.getGuestMap() == null) {
                continue;
            }
            for (GuestData guest : casino.getGuestMap().values()) {
                if (guest != null && guest.getStar() >= minStar) {
                    count++;
                }
            }
        }
        return count;
    }

    private long unlockedCasinoCount(SimPlayerContext ctx) {
        SimCasinoUnlock unlock = ctx.getCasinoUnlock();
        return unlock == null || unlock.getResearchLevelMap() == null
                ? 0 : unlock.getResearchLevelMap().size();
    }

    private long sceneLevel(SimPlayerContext ctx, int casinoId) {
        if (casinoId <= 0) {
            return ctx.getSimBaseData() == null ? 0 : ctx.getSimBaseData().getAllLevel();
        }
        SimCasinoData casino = casino(ctx, casinoId);
        return casino == null ? 0 : casino.getCasinoLevel();
    }

    private SimCasinoData casino(SimPlayerContext ctx, int casinoId) {
        SimCasinoData current = ctx.getCurrentCasino();
        return current != null && current.getCasinoId() == casinoId
                ? current : simCasinoDao.findOne(ctx.playerId(), casinoId);
    }

    private long combatPower(SimPlayerContext ctx, int gameType) {
        if (gameType <= 0) {
            return simSkillService.computeCombatPower(ctx);
        }
        SimSkillsData skillsData = ctx.getSkillData(gameType);
        return skillsData == null ? 0 : simSkillService.oneGameCombatPower(skillsData);
    }

    /**
     * 当前场景可能尚未落库，以内存数据覆盖数据库中的同场景快照。
     */
    private List<SimCasinoData> allCasinos(SimPlayerContext ctx) {
        Map<Integer, SimCasinoData> casinos = new HashMap<>();
        for (SimCasinoData casino : simCasinoDao.findByPlayerId(ctx.playerId())) {
            if (casino != null) {
                casinos.put(casino.getCasinoId(), casino);
            }
        }
        SimCasinoData current = ctx.getCurrentCasino();
        if (current != null) {
            casinos.put(current.getCasinoId(), current);
        }
        return List.copyOf(casinos.values());
    }
}
