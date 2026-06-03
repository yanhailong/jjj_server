package com.jjg.game.sim.service;

import com.jjg.game.core.constant.Code;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BuildingAreaTableCfg;
import com.jjg.game.sampledata.bean.EmployeeLevelCfg;
import com.jjg.game.sampledata.bean.EmployeeProfileCfg;
import com.jjg.game.sampledata.bean.EmployeeStarCfg;
import com.jjg.game.sim.constant.BonusType;
import com.jjg.game.sim.dao.SimEmployeeDao;
import com.jjg.game.sim.data.BuildingData;
import com.jjg.game.sim.data.SimEmployeeData;
import com.jjg.game.sim.data.SimPlayerContext;
import com.jjg.game.sim.pb.res.ResAssignSupervisor;
import com.jjg.game.sim.pb.res.ResRecruitEmployee;
import com.jjg.game.sim.pb.res.ResStarUpEmployee;
import com.jjg.game.sim.pb.res.ResUpgradeEmployee;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
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
     * 招募雇员
     */
    public void onRecruitEmployee(SimPlayerContext ctx, int employeeId) {
        ResRecruitEmployee res = new ResRecruitEmployee(Code.SUCCESS);
        res.employeeId = employeeId;
        try {

            EmployeeProfileCfg profile = GameDataManager.getEmployeeProfileCfg(employeeId);
            if (profile == null) {
                log.warn("招募雇员失败, 配置不存在 playerId={},employeeId={}", ctx.playerId(), employeeId);
                res.code = Code.NOT_FOUND;
                ctx.send(res);
                return;
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
            } else {
                //已解锁: 自动转化为碎片
                data.addFragment(1);
                ctx.getEmployeeMap().put(employeeId, data);
                log.info("雇员已解锁, 转化为碎片 playerId={},employeeId={},fragment={}", ctx.playerId(), employeeId, data.getFragment());
            }
        } catch (Exception e) {
            log.error("", e);
            res.code = Code.EXCEPTION;
        }
        ctx.send(res);
    }


    /**
     * 升级雇员
     */
    public void onUpgradeEmployee(SimPlayerContext ctx, int employeeId) {
        ResUpgradeEmployee res = new ResUpgradeEmployee(Code.SUCCESS);
        res.employeeId = employeeId;
        try {
            SimEmployeeData data = ctx.getEmployee(employeeId);
            if (data == null) {
                log.warn("升级雇员失败, 未解锁 playerId={},employeeId={}", ctx.playerId(), employeeId);
                res.code = Code.NOT_FOUND;
                ctx.send(res);
                return;
            }
            //检查星级带来的等级上限
            EmployeeStarCfg starCfg = getStarCfg(employeeId, data.getStar());
            int levelCap = starCfg == null ? 0 : starCfg.getLevelCap();
            if (levelCap > 0 && data.getLevel() >= levelCap) {
                log.warn("升级雇员失败, 已到星级等级上限 playerId={},employeeId={},level={},cap={}", ctx.playerId(), employeeId, data.getLevel(), levelCap);
                res.code = Code.PARAM_ERROR;
                ctx.send(res);
                return;
            }
            EmployeeLevelCfg nextCfg = getLevelCfg(employeeId, data.getLevel() + 1);
            if (nextCfg == null) {
                log.warn("升级雇员失败, 已达配置上限 playerId={},employeeId={},level={}", ctx.playerId(), employeeId, data.getLevel());
                res.code = Code.PARAM_ERROR;
                ctx.send(res);
                return;
            }
            if (!checkAndConsumeUpgradeCost(ctx, nextCfg.getUpgradeCost())) {
                res.code = Code.NOT_ENOUGH;
                ctx.send(res);
                return;
            }
            data.setLevel(data.getLevel() + 1);
            res.level = data.getLevel();
            log.info("升级雇员成功 playerId={},employeeId={},newLevel={}", ctx.playerId(), employeeId, data.getLevel());
        } catch (Exception e) {
            log.error("", e);
            res.code = Code.EXCEPTION;
        }
        ctx.send(res);
    }

    /**
     * 升星雇员
     */
    public void onStarUpEmployee(SimPlayerContext ctx, int employeeId) {
        ResStarUpEmployee res = new ResStarUpEmployee(Code.SUCCESS);
        res.employeeId = employeeId;
        try {
            SimEmployeeData data = ctx.getEmployee(employeeId);
            if (data == null) {
                log.warn("升星雇员失败, 未解锁 playerId={},employeeId={}", ctx.playerId(), employeeId);
                res.code = Code.NOT_FOUND;
                ctx.send(res);
                return;
            }
            EmployeeStarCfg curCfg = getStarCfg(employeeId, data.getStar());
            if (curCfg == null || curCfg.getStarUpCost() <= 0) {
                log.warn("升星雇员失败, 已达星级上限 playerId={},employeeId={},star={}", ctx.playerId(), employeeId, data.getStar());
                res.code = Code.PARAM_ERROR;
                ctx.send(res);
                return;
            }
            if (data.getFragment() < curCfg.getStarUpCost()) {
                log.warn("升星雇员失败, 碎片不足 playerId={},employeeId={},need={},have={}", ctx.playerId(), employeeId, curCfg.getStarUpCost(), data.getFragment());
                res.code = Code.NOT_ENOUGH;
                ctx.send(res);
                return;
            }
            data.addFragment(-curCfg.getStarUpCost());
            data.setStar(data.getStar() + 1);
            res.star = data.getStar();
            log.info("升星雇员成功 playerId={},employeeId={},newStar={}", ctx.playerId(), employeeId, data.getStar());
        } catch (Exception e) {
            log.error("", e);
            res.code = Code.EXCEPTION;
        }
        ctx.send(res);
    }

    /**
     * 任命主管
     */
    public void onAssignSupervisor(SimPlayerContext ctx, int buildingId, int employeeId) {
        ResAssignSupervisor res = new ResAssignSupervisor(Code.SUCCESS);
        res.buildingType = buildingId;
        res.employeeId = employeeId;
        try {
            //获取建筑配置
            BuildingAreaTableCfg cfg = GameDataManager.getBuildingAreaTableCfg(buildingId);
            if (cfg == null) {
                log.warn("任命主管失败, 未找到建筑配置信息 playerId={},buildingId={}", ctx.playerId(), buildingId);
                res.code = Code.NOT_FOUND;
                ctx.send(res);
                return;
            }
            SimEmployeeData data = ctx.getEmployee(employeeId);
            if (data == null) {
                log.warn("任命主管失败, 雇员未解锁 playerId={},employeeId={}", ctx.playerId(), employeeId);
                res.code = Code.NOT_FOUND;
                ctx.send(res);
                return;
            }
            BuildingData buildingData = ctx.getCurrentCasino().getBuildingData().get(buildingId);
            if (buildingData == null) {
                log.warn("任命主管失败, 该建筑未解锁 playerId={},buildingId={}", ctx.playerId(), buildingId);
                res.code = Code.NOT_FOUND;
                ctx.send(res);
                return;
            }

            boolean contains = ctx.getCurrentCasino().containsManageEmploy(cfg.getType());
            if (contains) {
                log.warn("任命主管失败, 该类建筑已有主管 playerId={},buildingId={},type={}", ctx.playerId(), buildingId, cfg.getType());
                res.code = Code.FORBID;
                ctx.send(res);
                return;
            }

            ctx.getCurrentCasino().addManagerEmploy(cfg.getType(), employeeId);
            log.info("任命主管 playerId={},casinoId={},type={},employeeId={}", ctx.playerId(), ctx.getCurrentCasino().getCasinoId(), buildingId, employeeId);
        } catch (Exception e) {
            log.error("", e);
            res.code = Code.EXCEPTION;
        }
        ctx.send(res);
    }

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
     * 仅仅获取主管的加成
     *
     * @param ctx
     * @param supervisorEmployId
     * @return
     */
    public Map<BonusType, Integer> manageEmployeeBonus(SimPlayerContext ctx, int supervisorEmployId) {
        if (supervisorEmployId < 1) {
            return Collections.emptyMap();
        }
        SimEmployeeData supervisor = ctx.getEmployee(supervisorEmployId);
        if (supervisor == null) {
            return Collections.emptyMap();
        }
        EmployeeStarCfg starCfg = getStarCfg(supervisorEmployId, supervisor.getStar());
        if (starCfg == null) {
            return Collections.emptyMap();
        }
        Map<BonusType, Integer> map = new HashMap<>();
        sumBouns(map, starCfg.getSupervisorBonus());
        return map;
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
