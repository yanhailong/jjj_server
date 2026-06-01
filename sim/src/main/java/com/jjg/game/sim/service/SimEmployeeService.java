package com.jjg.game.sim.service;

import com.jjg.game.core.constant.Code;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.EmployeeLevelCfg;
import com.jjg.game.sampledata.bean.EmployeeProfileCfg;
import com.jjg.game.sampledata.bean.EmployeeStarCfg;
import com.jjg.game.sim.constant.BonusType;
import com.jjg.game.sim.dao.SimEmployeeDao;
import com.jjg.game.sim.data.BuildingData;
import com.jjg.game.sim.data.SimCasinoData;
import com.jjg.game.sim.data.SimEmployeeData;
import com.jjg.game.sim.data.SimPlayerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 雇员服务: 招募(解锁)、升级、升星、任命主管
 *
 * @author 11
 * @date 2026/5/28
 */
@Service
public class SimEmployeeService {
    private static final Logger log = LoggerFactory.getLogger(SimEmployeeService.class);

    //雇员初始等级 / 星级
    private static final int INITIAL_LEVEL = 1;
    private static final int INITIAL_STAR = 1;

    @Autowired
    private SimConfigCacheService configCache;
    @Autowired
    private SimEmployeeDao simEmployeeDao;

    /**
     * 加载雇员数据
     *
     * @param ctx
     */
    public void loadEmployeeData(SimPlayerContext ctx) {
        List<SimEmployeeData> list = simEmployeeDao.findByPlayerId(ctx.playerId());
        if (list != null && !list.isEmpty()) {
            for (SimEmployeeData e : list) {
                ctx.getEmployeeMap().put(e.getEmployeeId(), e);
            }
        }
    }

    /**
     * 招募(解锁) 雇员
     * - 首次解锁: 写入一条 EmployeeData (level=1, star=1)
     * - 已解锁: 给玩家追加一份碎片
     */
    public int recruitEmployee(SimPlayerContext ctx, int employeeId) {
        EmployeeProfileCfg profile = GameDataManager.getEmployeeProfileCfg(employeeId);
        if (profile == null) {
            log.warn("招募雇员失败, 配置不存在 playerId={},employeeId={}", ctx.playerId(), employeeId);
            return Code.NOT_FOUND;
        }
        SimEmployeeData data = ctx.getEmployee(employeeId);
        if (data == null) {
            data = new SimEmployeeData();
            data.setPlayerId(ctx.playerId());
            data.setEmployeeId(employeeId);
            data.setLevel(INITIAL_LEVEL);
            data.setStar(INITIAL_STAR);
            ctx.getEmployeeMap().put(employeeId, data);
            log.info("招募雇员成功 playerId={},employeeId={}", ctx.playerId(), employeeId);
            return Code.SUCCESS;
        }
        //已解锁: 自动转化为碎片
        data.addFragment(1);
        ctx.getEmployeeMap().put(employeeId, data);
        log.info("雇员已解锁, 转化为碎片 playerId={},employeeId={},fragment={}", ctx.playerId(), employeeId, data.getFragment());
        return Code.SUCCESS;
    }

    /**
     * 升级雇员: 等级 +1
     */
    public int upgradeEmployee(SimPlayerContext ctx, int employeeId) {
        SimEmployeeData data = ctx.getEmployee(employeeId);
        if (data == null) {
            log.warn("升级雇员失败, 未解锁 playerId={},employeeId={}", ctx.playerId(), employeeId);
            return Code.NOT_FOUND;
        }
        //检查星级带来的等级上限
        EmployeeStarCfg starCfg = getStarCfg(employeeId, data.getStar());
        int levelCap = starCfg == null ? 0 : starCfg.getLevelCap();
        if (levelCap > 0 && data.getLevel() >= levelCap) {
            log.warn("升级雇员失败, 已到星级等级上限 playerId={},employeeId={},level={},cap={}", ctx.playerId(), employeeId, data.getLevel(), levelCap);
            return Code.PARAM_ERROR;
        }
        EmployeeLevelCfg nextCfg = getLevelCfg(employeeId, data.getLevel() + 1);
        if (nextCfg == null) {
            log.warn("升级雇员失败, 已达配置上限 playerId={},employeeId={},level={}", ctx.playerId(), employeeId, data.getLevel());
            return Code.PARAM_ERROR;
        }
        if (!checkAndConsumeUpgradeCost(ctx, nextCfg.getUpgradeCost())) {
            return Code.NOT_ENOUGH;
        }
        data.setLevel(data.getLevel() + 1);
        log.info("升级雇员成功 playerId={},employeeId={},newLevel={}", ctx.playerId(), employeeId, data.getLevel());
        return Code.SUCCESS;
    }

    /**
     * 升星雇员: 星级 +1, 消耗 fragment
     */
    public int starUpEmployee(SimPlayerContext ctx, int employeeId) {
        SimEmployeeData data = ctx.getEmployee(employeeId);
        if (data == null) {
            log.warn("升星雇员失败, 未解锁 playerId={},employeeId={}", ctx.playerId(), employeeId);
            return Code.NOT_FOUND;
        }
        EmployeeStarCfg curCfg = getStarCfg(employeeId, data.getStar());
        if (curCfg == null || curCfg.getStarUpCost() <= 0) {
            log.warn("升星雇员失败, 已达星级上限 playerId={},employeeId={},star={}", ctx.playerId(), employeeId, data.getStar());
            return Code.PARAM_ERROR;
        }
        if (data.getFragment() < curCfg.getStarUpCost()) {
            log.warn("升星雇员失败, 碎片不足 playerId={},employeeId={},need={},have={}", ctx.playerId(), employeeId, curCfg.getStarUpCost(), data.getFragment());
            return Code.NOT_ENOUGH;
        }
        data.addFragment(-curCfg.getStarUpCost());
        data.setStar(data.getStar() + 1);
        log.info("升星雇员成功 playerId={},employeeId={},newStar={}", ctx.playerId(), employeeId, data.getStar());
        return Code.SUCCESS;
    }

    /**
     * 任命/更换主管: 按建筑分类记录到当前赌场, 同分类内所有建筑共享
     */
    public int assignSupervisor(SimPlayerContext ctx, int buildingId, int employeeId) {
        SimCasinoData casino = ctx.getCurrentCasino();
        if (casino == null) {
            return Code.NOT_FOUND;
        }
        SimEmployeeData data = ctx.getEmployee(employeeId);
        if (data == null) {
            log.warn("任命主管失败, 雇员未解锁 playerId={},employeeId={}", ctx.playerId(), employeeId);
            return Code.NOT_FOUND;
        }
        BuildingData buildingData = ctx.getCurrentCasino().getBuildingData().get(buildingId);
        if (buildingData == null) {
            log.warn("任命主管失败, 该建筑未解锁 playerId={},buildingId={}", ctx.playerId(), buildingId);
            return Code.NOT_FOUND;
        }
        buildingData.setManagerEmployId(employeeId);
        log.info("任命主管 playerId={},casinoId={},type={},employeeId={}", ctx.playerId(), casino.getCasinoId(), buildingId, employeeId);
        return Code.SUCCESS;
    }

    /**
     * 计算所有已解锁雇员加成固定值之和:
     *
     * @param ctx 玩家上下文
     * @return 加成之和
     */
    public void computeTypeBonusFixed(SimPlayerContext ctx, Map<BonusType, Integer> bonusesMap) {
        //所有已解锁同职业雇员的等级加成
        for (SimEmployeeData emp : ctx.getEmployeeMap().values()) {
            EmployeeLevelCfg levelCfg = getLevelCfg(emp.getEmployeeId(), emp.getLevel());
            if (levelCfg == null) {
                continue;
            }
            sumBouns(bonusesMap, levelCfg.getAttributeValue());
        }
    }

    /**
     * 主管加成
     *
     * @param ctx
     * @param bonusesMap
     * @param supervisorEmployId 该建筑当前主管雇员id (0 表示未任命)
     */
    public Map<BonusType, Integer> computeSupervisorBonusFixed(SimPlayerContext ctx, Map<BonusType, Integer> bonusesMap, int supervisorEmployId) {
        if (supervisorEmployId < 1 || bonusesMap == null || bonusesMap.isEmpty()) {
            return bonusesMap;
        }

        Map<BonusType, Integer> tmpMap = new HashMap<>(bonusesMap);
        SimEmployeeData supervisor = ctx.getEmployee(supervisorEmployId);
        if (supervisor != null) {
            EmployeeStarCfg starCfg = getStarCfg(supervisorEmployId, supervisor.getStar());
            if (starCfg != null) {
                sumBouns(tmpMap, starCfg.getSupervisorBonus());
            }
        }
        return tmpMap;
    }

    /**
     * 将所有的加成总结
     *
     * @param bonusesMap
     * @param attrMap
     */
    private void sumBouns(Map<BonusType, Integer> bonusesMap, Map<Integer, Integer> attrMap) {
        if (attrMap == null || attrMap.isEmpty()) {
            return;
        }

        for (Map.Entry<Integer, Integer> en : attrMap.entrySet()) {
            BonusType bonusType = BonusType.fromCode(en.getKey());
            if (bonusType == null) {
                continue;
            }
            bonusesMap.merge(bonusType, en.getValue(), Integer::sum);
        }
    }

    // ---------------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------------

    private EmployeeLevelCfg getLevelCfg(int employeeId, int level) {
        Map<Integer, Map<Integer, EmployeeLevelCfg>> levelMap = configCache.getEmployeeLevelCfgMap();
        if (levelMap == null) {
            return null;
        }
        Map<Integer, EmployeeLevelCfg> m = levelMap.get(employeeId);
        return m == null ? null : m.get(level);
    }

    private EmployeeStarCfg getStarCfg(int employeeId, int star) {
        Map<Integer, Map<Integer, EmployeeStarCfg>> starMap = configCache.getEmployeeStarCfgMap();
        if (starMap == null) {
            return null;
        }
        Map<Integer, EmployeeStarCfg> m = starMap.get(employeeId);
        return m == null ? null : m.get(star);
    }

    /**
     * 雇员升级资源检查 & 扣除 (占位)
     */
    private boolean checkAndConsumeUpgradeCost(SimPlayerContext ctx, Map<Integer, Integer> cost) {
        if (cost == null || cost.isEmpty()) {
            return true;
        }
        log.debug("[stub] 扣除雇员升级资源 playerId={},cost={}", ctx.playerId(), cost);
        return true;
    }
}
