package com.jjg.game.sim.service;

import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.common.utils.WeightRandom;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.pb.KVInfo;
import com.jjg.game.core.utils.ItemUtils;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BuildingAreaTableCfg;
import com.jjg.game.sampledata.bean.EmployeeLevelCfg;
import com.jjg.game.sampledata.bean.EmployeeProfileCfg;
import com.jjg.game.sampledata.bean.EmployeeStarCfg;
import com.jjg.game.sampledata.bean.PoolListCfg;
import com.jjg.game.sim.constant.BonusType;
import com.jjg.game.sim.constant.SimConstant;
import com.jjg.game.sim.dao.SimEmployeeDao;
import com.jjg.game.sim.data.BuildingData;
import com.jjg.game.sim.data.SimEmployeeData;
import com.jjg.game.sim.data.SimPlayerContext;
import com.jjg.game.sim.pb.res.ResAllEmployee;
import com.jjg.game.sim.pb.res.ResAssignSupervisor;
import com.jjg.game.sim.pb.res.ResRecruitEmployee;
import com.jjg.game.sim.pb.res.ResStarUpEmployee;
import com.jjg.game.sim.pb.res.ResUpgradeEmployee;
import com.jjg.game.sim.pb.struct.EmployDetailInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

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
    @Autowired
    private SimPackService simPackService;

    /**
     * 招募雇员 (卡池抽取):
     * 命中未拥有的雇员则解锁, 命中已拥有的雇员则转化为对应碎片
     *
     * @param ctx
     * @param count 招募次数 (1/10)
     */
    public void onRecruitEmployee(SimPlayerContext ctx, int count) {
        ResRecruitEmployee res = new ResRecruitEmployee(Code.SUCCESS);
        try {
            if (count != 1 && count != 10) {
                log.warn("招募雇员失败,次数参数错误 playerId={},count={}", ctx.playerId(), count);
                res.code = Code.PARAM_ERROR;
                ctx.send(res);
                return;
            }

            long now = System.currentTimeMillis();
            PoolListCfg tmpCfg = null;
            for (PoolListCfg cfg : GameDataManager.getPoolListCfgList()) {
                if (cfg.getType() != SimConstant.PoolList.TYPE_EMPLOYEE) {
                    continue;
                }

                if (!cfg.getOpen()) {
                    continue;
                }

                if (cfg.getTime_start() != null && !cfg.getTime_start().isEmpty() && cfg.getTime_end() != null && !cfg.getTime_end().isEmpty()) {
                    long startTime = TimeHelper.getTimeMillisBy(cfg.getTime_start());
                    long endTime = TimeHelper.getTimeMillisBy(cfg.getTime_end());
                    if (startTime >= endTime) {
                        continue;
                    }
                    if (now >= startTime && now <= endTime) {
                        tmpCfg = cfg;
                        break;
                    }
                } else {
                    tmpCfg = cfg;
                    break;
                }
            }

            if (tmpCfg == null) {
                log.warn("招募雇员失败,获取卡池配置失败 playerId={},count={}", ctx.playerId(), count);
                res.code = Code.PARAM_ERROR;
                ctx.send(res);
                return;
            }

            WeightRandom<List<Integer>> poolRand = configCache.getEmployeePoolRand(tmpCfg.getId());
            if (poolRand == null) {
                log.warn("招募雇员失败,获取卡池权重失败 playerId={},count={},poolId={}", ctx.playerId(), count, tmpCfg.getId());
                res.code = Code.PARAM_ERROR;
                ctx.send(res);
                return;
            }

            Map<Integer, Long> addItems = new HashMap<>();
            Map<Integer, Integer> addEmployee = new HashMap<>();
            for (int i = 0; i < count; i++) {
                List<Integer> next = poolRand.next();
                if (next == null || next.size() < 3) {
                    log.warn("招募雇员失败,卡池掉落配置异常 playerId={},count={},i={}", ctx.playerId(), count, i);
                    return;
                }

                EmployeeProfileCfg profileCfg = configCache.getEmployeeProfileCfgByItemId(next.get(1));
                if (profileCfg == null) {
                    log.warn("招募雇员失败,根据itemId获取EmployeeProfileCfg失败 playerId={},count={},itemId={},i={}", ctx.playerId(), count, next.get(1), i);
                    return;
                }

                int rewardCount = next.get(2);
                for (int j = 0; j < rewardCount; j++) {
                    SimEmployeeData data = ctx.getEmployee(profileCfg.getId());
                    if (data == null) {
                        data = new SimEmployeeData();
                        data.setPlayerId(ctx.playerId());
                        data.setEmployeeId(profileCfg.getId());
                        data.setLevel(INITIAL_LEVEL);
                        data.setStar(INITIAL_STAR);
                        ctx.putEmployee(data);

                        addEmployee.merge(profileCfg.getId(), 1, Integer::sum);
                    } else { //已拥有,转化成碎片
                        List<Integer> shard = profileCfg.getDuplicatetoShard();
                        addItems.merge(shard.get(1), shard.get(2).longValue(), Long::sum);
                    }
                }
            }

            if (!addItems.isEmpty()) {
                res.items = ItemUtils.buildItemInfo(addItems);
                simPackService.addItems(ctx, addItems, AddType.SIM_EMPLOYEE_RECRUIT, count + "", false);
            }

            if (!addEmployee.isEmpty()) {
                res.employees = new ArrayList<>();
                for (Map.Entry<Integer, Integer> en : addEmployee.entrySet()) {
                    KVInfo kvInfo = new KVInfo();
                    kvInfo.key = en.getKey();
                    kvInfo.value = en.getValue();
                    res.employees.add(kvInfo);
                }
            }

            log.info("招募雇员成功 playerId={},count={},newEmployee={},shard={}", ctx.playerId(), count, addEmployee, addItems);
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
//            if (data.getFragment() < curCfg.getStarUpCost()) {
//                log.warn("升星雇员失败, 碎片不足 playerId={},employeeId={},need={},have={}", ctx.playerId(), employeeId, curCfg.getStarUpCost(), data.getFragment());
//                res.code = Code.NOT_ENOUGH;
//                ctx.send(res);
//                return;
//            }
//            data.addFragment(-curCfg.getStarUpCost());
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
     * 获取所有雇员
     *
     * @param ctx
     */
    public void onAllEmployee(SimPlayerContext ctx) {
        ResAllEmployee res = new ResAllEmployee(Code.SUCCESS);
        try {
            if (ctx.getEmployeeMap() != null && !ctx.getEmployeeMap().isEmpty()) {
                res.employees = new ArrayList<>();
                for (Map.Entry<Integer, SimEmployeeData> en : ctx.getEmployeeMap().entrySet()) {
                    SimEmployeeData value = en.getValue();
                    EmployDetailInfo detailInfo = new EmployDetailInfo();
                    detailInfo.id = value.getEmployeeId();
                    detailInfo.level = value.getLevel();
                    detailInfo.star = value.getStar();
                    res.employees.add(detailInfo);
                }
            }
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
