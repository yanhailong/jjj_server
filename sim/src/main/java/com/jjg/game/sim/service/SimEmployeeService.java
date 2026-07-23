package com.jjg.game.sim.service;

import com.jjg.game.alliance.service.AllianceEventService;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.common.utils.WeightRandom;
import com.jjg.game.core.base.condition.numeric.ActionConditionEvent;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.pb.KVInfo;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.*;
import com.jjg.game.sim.constant.BuildingOutputType;
import com.jjg.game.sim.constant.SimConstant;
import com.jjg.game.sim.dao.SimEmployeeDao;
import com.jjg.game.sim.data.SimEmployeeData;
import com.jjg.game.sim.data.SimItemOperationResult;
import com.jjg.game.sim.data.SimPlayerContext;
import com.jjg.game.sim.pb.res.*;
import com.jjg.game.sim.pb.struct.EmployDetailInfo;
import com.jjg.game.sim.pb.struct.RecruitItemInfo;
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
    @Autowired
    private AllianceEventService allianceEventService;
    @Autowired
    private SimMedalService medalService;

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

            WeightRandom<List<Integer>> poolRand = configCache.getEmployeePoolRand(tmpCfg.getDropItem());
            if (poolRand == null) {
                log.warn("招募雇员失败,获取卡池权重失败 playerId={},count={},poolId={}", ctx.playerId(), count, tmpCfg.getId());
                res.code = Code.PARAM_ERROR;
                ctx.send(res);
                return;
            }

            if (tmpCfg.getDrawCost() != null && !tmpCfg.getDrawCost().isEmpty()) {
                Map<Integer, Long> costMap = tmpCfg.getDrawCost();
                if (count > 1) {
                    costMap = new HashMap<>();
                    for (Map.Entry<Integer, Long> en : tmpCfg.getDrawCost().entrySet()) {
                        costMap.put(en.getKey(), en.getValue() * count);
                    }
                }
                boolean remove = simPackService.removeItems(ctx, costMap, AddType.SIM_EMPLOYEE_RECRUIT, null);
                if (!remove) {
                    log.warn("招募雇员失败,扣除道具失败 playerId={},count={},poolId={}", ctx.playerId(), count, tmpCfg.getId());
                    res.code = Code.PARAM_ERROR;
                    ctx.send(res);
                    return;
                }
            }

            //新手引导: 不走随机, 固定召唤 NewbieGuideDraw 配置的雇员
            boolean guide = ctx.getSimBaseData().isGuide();
            int guideItemId = 0;
            if (!guide) {
                EmployeePoolCfg employeePoolCfg = GameDataManager.getEmployeePoolCfg(tmpCfg.getDropItem());
                if (employeePoolCfg == null || employeePoolCfg.getNewbieGuideDraw() <= 0) {
                    log.warn("招募雇员失败,新手引导卡池配置异常 playerId={},poolId={}", ctx.playerId(), tmpCfg.getDropItem());
                    res.code = Code.PARAM_ERROR;
                    ctx.send(res);
                    return;
                }
                guideItemId = employeePoolCfg.getNewbieGuideDraw();
            }

            Map<Integer, Long> addAllItems = new HashMap<>();
            List<RecruitItemInfo> recruitItems = new ArrayList<>();

            Map<Integer, Integer> addEmployee = new HashMap<>();
            Map<Integer, Long> recruitedProfessions = new HashMap<>();
            for (int i = 0; i < count; i++) {
                int drawItemId;
                int rewardCount;
                if (!guide) {
                    drawItemId = guideItemId;
                    rewardCount = 1;
                } else {
                    List<Integer> next = poolRand.next();
                    if (next == null || next.size() < 3) {
                        log.warn("招募雇员失败,卡池掉落配置异常 playerId={},count={},i={}", ctx.playerId(), count, i);
                        return;
                    }
                    drawItemId = next.get(1);
                    rewardCount = next.get(2);
                }

                EmployeeProfileCfg profileCfg = configCache.getEmployeeProfileCfgByItemId(drawItemId);
                if (profileCfg == null) {
                    log.warn("招募雇员失败,根据itemId获取EmployeeProfileCfg失败 playerId={},count={},itemId={},i={}", ctx.playerId(), count, drawItemId, i);
                    return;
                }

                for (int j = 0; j < rewardCount; j++) {
                    recruitedProfessions.merge(profileCfg.getProfessionID(), 1L, Long::sum);
                    RecruitItemInfo re = new RecruitItemInfo();
                    re.itemId = profileCfg.getDuplicatetoShard().get(0);
                    re.count = profileCfg.getDuplicatetoShard().get(2);

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
                        List<Integer> tmpList = profileCfg.getDuplicatetoShard();
                        if (tmpList != null && !tmpList.isEmpty()) {
                            int itemId = tmpList.get(1);
                            long itemCount = tmpList.get(2).longValue();

                            addAllItems.merge(itemId, itemCount, Long::sum);
                            re.breakDown = true;
                        }
                    }
                    recruitItems.add(re);
                }
            }

            if (!addAllItems.isEmpty()) {
                CommonResult<SimItemOperationResult> simItemOperationResultCommonResult = simPackService.addItems(ctx, addAllItems, AddType.SIM_GUEST_RECRUIT, count + "", false);
                if (!simItemOperationResultCommonResult.success()) {
                    log.warn("招募雇员后添加碎片道具失败 playerId={},count={},code={}", ctx.playerId(), count, simItemOperationResultCommonResult.code);
                    res.code = simItemOperationResultCommonResult.code;
                    ctx.send(res);
                    return;
                }
            }

            res.shardInfos = recruitItems;
            //联盟任务: 卡池抽奖次数 (param=卡池ID, 供 0=任意/指定卡池 过滤; 10 连计为 10 次)
            allianceEventService.onCardPoolDraw(ctx.playerId(), tmpCfg.getId(), count);
            recruitedProfessions.forEach((professionId, recruited) ->
                    allianceEventService.onEmployeeRecruit(ctx.playerId(), professionId, recruited));
            //主线任务: 招募改变各职业各星级持有量 -> 上报 12212 "拥有 N 个 X 星雇员"
            reportEmployeeCounts(ctx);
            log.info("招募雇员成功 playerId={},count={},newEmployee={},addAllItems={}", ctx.playerId(), count, addEmployee, addAllItems);
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

            if (nextCfg.getUpgradeCost() != null && !nextCfg.getUpgradeCost().isEmpty()) {
                boolean removeItems = simPackService.removeItems(ctx, nextCfg.getUpgradeCost(), AddType.SIM_EMPLOYEE_LEVEL_UP, null);
                if (!removeItems) {
                    log.warn("升级雇员失败, 扣除道具失败 playerId={},employeeId={},level={},cost={}", ctx.playerId(), employeeId, data.getLevel(), nextCfg.getUpgradeCost());
                    res.code = Code.PARAM_ERROR;
                    ctx.send(res);
                    return;
                }
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

            EmployeeProfileCfg employeeProfileCfg = GameDataManager.getEmployeeProfileCfg(employeeId);
            if (employeeProfileCfg == null || employeeProfileCfg.getDuplicatetoShard() == null) {
                log.warn("升星雇员失败, 获取雇员配置失败 playerId={},employeeId={}", ctx.playerId(), employeeId);
                res.code = Code.PARAM_ERROR;
                ctx.send(res);
                return;
            }

            boolean remove = simPackService.removeItem(ctx, employeeProfileCfg.getDuplicatetoShard().get(1), curCfg.getStarUpCost(), AddType.SIM_EMPLOYEE_STAR_UP);
            if (!remove) {
                log.warn("升星雇员失败, 扣除碎片道具失败 playerId={},employeeId={},need={}", ctx.playerId(), employeeId, curCfg.getStarUpCost());
                res.code = Code.NOT_ENOUGH;
                ctx.send(res);
                return;
            }
            data.setStar(data.getStar() + 1);
            res.star = data.getStar();
            //主线任务: 升星改变各星级持有量 -> 上报 12212 "拥有 N 个 X 星雇员"
            reportEmployeeCounts(ctx);
            log.info("升星雇员成功 playerId={},employeeId={},newStar={}", ctx.playerId(), employeeId, data.getStar());
        } catch (Exception e) {
            log.error("", e);
            res.code = Code.EXCEPTION;
        }
        ctx.send(res);
    }

    /**
     * 上报各职业各星级雇员持有量, 推进 12212 "拥有 N 个 X 星雇员" (X 星按 ≥ 阈值判定)。
     * 雇员为玩家级全量驻留内存, 按职业分别上报, 供 12212 的职业过滤维度匹配。
     */
    private void reportEmployeeCounts(SimPlayerContext ctx) {
        Map<Integer, SimEmployeeData> employees = ctx.getEmployeeMap();
        if (employees == null || employees.isEmpty()) {
            return;
        }
        // 职业 -> (星级 -> 恰好该星级的数量)
        Map<Integer, SortedMap<Integer, Long>> byProfession = new HashMap<>();
        for (SimEmployeeData employee : employees.values()) {
            EmployeeProfileCfg cfg = GameDataManager.getEmployeeProfileCfg(employee.getEmployeeId());
            if (cfg == null) {
                continue;
            }
            byProfession.computeIfAbsent(cfg.getProfessionID(), k -> new TreeMap<>())
                    .merge(employee.getStar(), 1L, Long::sum);
        }
        byProfession.forEach((profession, starCounts) ->
                allianceEventService.onOwnershipCounts(ctx.playerId(),
                        ActionConditionEvent.Type.EMPLOYEE_COUNT, profession, starCounts));
    }

    /**
     * 任命主管
     */
    public void onAssignSupervisor(SimPlayerContext ctx, int employeeId) {
        ResAssignSupervisor res = new ResAssignSupervisor(Code.SUCCESS);
        res.employeeId = employeeId;
        try {
            //获取雇员配置
            EmployeeProfileCfg cfg = GameDataManager.getEmployeeProfileCfg(employeeId);
            if (cfg == null) {
                log.warn("任命主管失败, 未找到雇员配置信息 playerId={},employeeId={}", ctx.playerId(), employeeId);
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
            ctx.getCurrentCasino().addManagerEmploy(cfg.getProfessionID(), employeeId);

            //主管百分比加成
            ManageBonus manageBonus = manageEmployeeBonus(ctx, cfg.getProfessionID());
            if (!manageBonus.modifier().isEmpty()) {
                res.manageEmployeeBonus = new ArrayList<>(manageBonus.modifier().size());
                for (Map.Entry<BuildingOutputType, Integer> en : manageBonus.modifier().entrySet()) {
                    res.manageEmployeeBonus.add(new KVInfo(en.getKey().getCode(), en.getValue()));
                }
            }
            //主管固定加成
            if (!manageBonus.buff().isEmpty()) {
                res.manageEmployeeFixBonus = new ArrayList<>(manageBonus.buff().size());
                for (Map.Entry<BuildingOutputType, Integer> en : manageBonus.buff().entrySet()) {
                    res.manageEmployeeFixBonus.add(new KVInfo(en.getKey().getCode(), en.getValue()));
                }
            }

            log.info("任命主管 playerId={},casinoId={},professionId={},employeeId={}", ctx.playerId(), ctx.getCurrentCasino().getCasinoId(), cfg.getProfessionID(), employeeId);
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
                    detailInfo.manager = ctx.getCurrentCasino().employIsManager(value.getEmployeeId());
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
     * 汇总玩家加成固定值之和 (雇员等级加成 + 勋章品质加成) 到 bonusesMap; 单位千分比。
     *
     * @param ctx        玩家上下文
     * @param bonusesMap 加成汇总输出
     */
    public void computeTypeBonusFixed(SimPlayerContext ctx, Map<BuildingOutputType, Integer> bonusesMap) {
        //所有已解锁同职业雇员的等级加成
        for (SimEmployeeData emp : ctx.getEmployeeMap().values()) {
            EmployeeLevelCfg levelCfg = getLevelCfg(emp.getEmployeeId(), emp.getLevel());
            if (levelCfg == null) {
                continue;
            }
            sumBouns(bonusesMap, levelCfg.getAttributeValue());
        }
        //勋章品质加成
        medalService.mergeMedalBonus(ctx, bonusesMap);
    }

    /**
     * 仅仅获取主管的加成: 主管雇员的 EmployeeProfile.SkillIdList 对应的 EmployeeSkillConfig
     * (Modifier 百分比 + Buff 固定值), 按 BuildingOutputType 归类。
     *
     * @param ctx             玩家上下文
     * @param employeeProfile 建筑关联的职业ID
     * @return 主管加成 (百分比 + 固定值)
     */
    public ManageBonus manageEmployeeBonus(SimPlayerContext ctx, int employeeProfile) {
        if (employeeProfile < 1) {
            return ManageBonus.empty();
        }

        int managerId = ctx.getCurrentCasino().manageEmploy(employeeProfile);
        if (managerId < 1) {
            return ManageBonus.empty();
        }
        //主管未解锁则无加成
        if (ctx.getEmployee(managerId) == null) {
            return ManageBonus.empty();
        }
        EmployeeProfileCfg profileCfg = GameDataManager.getEmployeeProfileCfg(managerId);
        if (profileCfg == null || profileCfg.getSkillIdList() == null || profileCfg.getSkillIdList().isEmpty()) {
            return ManageBonus.empty();
        }

        Map<BuildingOutputType, Integer> modifier = new HashMap<>();
        Map<BuildingOutputType, Integer> buff = new HashMap<>();
        for (Integer skillId : profileCfg.getSkillIdList()) {
            EmployeeSkillConfigCfg skillCfg = GameDataManager.getEmployeeSkillConfigCfg(skillId);
            if (skillCfg == null) {
                continue;
            }
            sumBouns(modifier, skillCfg.getModifier());
            sumBouns(buff, skillCfg.getBuff());
        }
        return new ManageBonus(modifier, buff);
    }

    /**
     * 主管加成: 百分比(千分比) + 固定值, 均按 {@link BuildingOutputType} 归类。
     */
    public record ManageBonus(Map<BuildingOutputType, Integer> modifier, Map<BuildingOutputType, Integer> buff) {
        private static final ManageBonus EMPTY = new ManageBonus(Collections.emptyMap(), Collections.emptyMap());

        public static ManageBonus empty() {
            return EMPTY;
        }

        public boolean isEmpty() {
            return modifier.isEmpty() && buff.isEmpty();
        }
    }

    /**
     * 将所有的加成总结
     *
     * @param bonusesMap
     * @param attrMap
     */
    private void sumBouns(Map<BuildingOutputType, Integer> bonusesMap, Map<Integer, Integer> attrMap) {
        if (attrMap == null || attrMap.isEmpty()) {
            return;
        }

        for (Map.Entry<Integer, Integer> en : attrMap.entrySet()) {
            BuildingOutputType buildingOutputType = BuildingOutputType.fromCode(en.getKey());
            if (buildingOutputType == null) {
                continue;
            }
            bonusesMap.merge(buildingOutputType, en.getValue(), Integer::sum);
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
     * 获取卡池
     *
     * @param ctx
     */
    public void onPool(SimPlayerContext ctx) {
        ResEmployeePool res = new ResEmployeePool(Code.SUCCESS);
        try {
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
                log.warn("获取雇员卡池失败1,playerId={}", ctx.playerId());
                res.code = Code.PARAM_ERROR;
                ctx.send(res);
                return;
            }

            EmployeePoolCfg employeePoolCfg = GameDataManager.getEmployeePoolCfg(tmpCfg.getDropItem());
            if (employeePoolCfg == null || employeePoolCfg.getDetailedDropItem() == null) {
                log.warn("获取雇员卡池失败2,playerId={}", ctx.playerId());
                res.code = Code.PARAM_ERROR;
                ctx.send(res);
                return;
            }

            res.employeeIds = new ArrayList<>();
            for (List<Integer> list : employeePoolCfg.getDetailedDropItem()) {
                res.employeeIds.add(list.get(1));
            }
        } catch (Exception e) {
            log.error("", e);
            res.code = Code.EXCEPTION;
        }
        ctx.send(res);
    }
}
