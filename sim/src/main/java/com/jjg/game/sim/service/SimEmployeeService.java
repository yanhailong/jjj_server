package com.jjg.game.sim.service;

import com.jjg.game.alliance.service.AllianceEventService;
import com.jjg.game.common.utils.WeightRandom;
import com.jjg.game.core.service.PlayerPackService;
import com.jjg.game.core.data.ItemOperationResult;
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
import com.jjg.game.sim.data.SimPlayerContext;
import com.jjg.game.sim.listener.SimTaskStateReporter;
import com.jjg.game.sim.pb.res.*;
import com.jjg.game.sim.pb.struct.EmployDetailInfo;
import com.jjg.game.sim.pb.struct.RecruitItemInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Consumer;

/**
 * 雇员服务: 招募(解锁)、升级、升星、任命主管
 *
 * @author 11
 * @date 2026/5/28
 */
@Service
public class SimEmployeeService implements SimTaskStateReporter {
    private static final Logger log = LoggerFactory.getLogger(SimEmployeeService.class);

    //雇员初始等级 / 星级
    private static final int INITIAL_LEVEL = 1;
    private static final int INITIAL_STAR = 1;

    @Autowired
    private SimConfigCacheService configCache;
    @Autowired
    private SimEmployeeDao simEmployeeDao;
    @Autowired
    private PlayerPackService playerPackService;
    @Autowired
    private AllianceEventService allianceEventService;
    @Autowired
    private SimMedalService medalService;
    @Autowired
    private SimGuideConfigService guideConfigService;
    //懒加载打破与 SimTaskService 的循环依赖 (对方持有本服务作状态补报口)
    @Autowired
    @Lazy
    private SimTaskService simTaskService;
    @Autowired
    private SimEmployeeRedDotService employeeRedDotService;

    /**
     * 招募雇员 (卡池抽取):
     * 命中未拥有的雇员则解锁, 命中已拥有的雇员则转化为对应碎片
     *
     * @param ctx
     * @param poolId 卡池id
     * @param count  招募次数 (1/10)
     */
    public void onRecruitEmployee(SimPlayerContext ctx, int poolId, int count) {
        ResRecruitEmployee res = new ResRecruitEmployee(Code.SUCCESS);
        try {
            if (count != 1 && count != 10) {
                log.warn("招募雇员失败,次数参数错误 playerId={},count={}", ctx.playerId(), count);
                res.code = Code.PARAM_ERROR;
                ctx.send(res);
                return;
            }

            PoolListCfg poolCfg = configCache.getOpenPoolCfg(poolId, SimConstant.PoolList.TYPE_EMPLOYEE);
            if (poolCfg == null) {
                log.warn("招募雇员失败,卡池不存在、未开启、不在开放时间或类型错误 playerId={},poolId={},count={}", ctx.playerId(), poolId, count);
                res.code = Code.PARAM_ERROR;
                ctx.send(res);
                return;
            }

            WeightRandom<List<Integer>> poolRand = configCache.getEmployeePoolRand(poolCfg.getDropItem());
            if (poolRand == null) {
                log.warn("招募雇员失败,获取卡池权重失败 playerId={},count={},poolId={}", ctx.playerId(), count, poolCfg.getId());
                res.code = Code.PARAM_ERROR;
                ctx.send(res);
                return;
            }

            if (poolCfg.getDrawCost() != null && !poolCfg.getDrawCost().isEmpty()) {
                Map<Integer, Long> costMap = poolCfg.getDrawCost();
                if (count > 1) {
                    costMap = new HashMap<>();
                    for (Map.Entry<Integer, Long> en : poolCfg.getDrawCost().entrySet()) {
                        costMap.put(en.getKey(), en.getValue() * count);
                    }
                }
                boolean remove = playerPackService.removeItems(ctx.getPlayer(), costMap, AddType.SIM_EMPLOYEE_RECRUIT, null).success();
                if (!remove) {
                    log.warn("招募雇员失败,扣除道具失败 playerId={},count={},poolId={}", ctx.playerId(), count, poolCfg.getId());
                    res.code = Code.PARAM_ERROR;
                    ctx.send(res);
                    return;
                }
            }

            EmployeePoolCfg employeePoolCfg = GameDataManager.getEmployeePoolCfg(poolCfg.getDropItem());
            List<Integer> newbieGuideDraw = employeePoolCfg == null ? null : employeePoolCfg.getNewbieGuideDraw();
            int guideItemId = guideConfigService.newbieFixedDrawItemId(ctx.getSimBaseData(), newbieGuideDraw);

            Map<Integer, Long> addAllItems = new HashMap<>();
            List<RecruitItemInfo> recruitItems = new ArrayList<>();

            Map<Integer, Integer> addEmployee = new HashMap<>();
            Map<Integer, Long> recruitedProfessions = new HashMap<>();
            for (int i = 0; i < count; i++) {
                int drawItemId;
                int rewardCount;
                if (guideItemId > 0) {
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
                CommonResult<ItemOperationResult> simItemOperationResultCommonResult = playerPackService.addItems(ctx.playerId(), addAllItems, AddType.SIM_GUEST_RECRUIT, count + "", false);
                if (!simItemOperationResultCommonResult.success()) {
                    log.warn("招募雇员后添加碎片道具失败 playerId={},count={},code={}", ctx.playerId(), count, simItemOperationResultCommonResult.code);
                    res.code = simItemOperationResultCommonResult.code;
                    ctx.send(res);
                    return;
                }
            }

            res.shardInfos = recruitItems;
            //联盟任务: 卡池抽奖次数 (param=卡池ID, 供 0=任意/指定卡池 过滤; 10 连计为 10 次)
            allianceEventService.onCardPoolDraw(ctx.playerId(), poolCfg.getId(), count);
            allianceEventService.onEmployeePoolDraw(ctx.playerId(), count);
            recruitedProfessions.forEach((professionId, recruited) ->
                    allianceEventService.onEmployeeRecruit(ctx.playerId(), professionId, recruited));
            employeeRedDotService.updateRedDots(ctx.playerId(),
                    SimConstant.Employee.RED_DOT_RECRUIT_POOL,
                    SimConstant.Employee.RED_DOT_EMPLOYEE_GROWTH);
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
            EmployeeLevelCfg currentCfg = getLevelCfg(employeeId, data.getLevel());
            if (currentCfg == null) {
                log.warn("升级雇员失败, 获取等级配置失败 playerId={},employeeId={},level={}", ctx.playerId(), employeeId, data.getLevel());
                res.code = Code.LEVEL_MAX;
                ctx.send(res);
                return;
            }

            if (currentCfg.getUpgradeCost() != null && !currentCfg.getUpgradeCost().isEmpty()) {
                boolean removeItems = playerPackService.removeItems(ctx.getPlayer(), currentCfg.getUpgradeCost(), AddType.SIM_EMPLOYEE_LEVEL_UP, null).success();
                if (!removeItems) {
                    log.warn("升级雇员失败, 扣除道具失败 playerId={},employeeId={},level={},cost={}", ctx.playerId(), employeeId, data.getLevel(), currentCfg.getUpgradeCost());
                    res.code = Code.NOT_ENOUGH_ITEM;
                    ctx.send(res);
                    return;
                }
            }

            data.setLevel(data.getLevel() + 1);
            res.level = data.getLevel();
            employeeRedDotService.updateRedDots(ctx.playerId(),
                    SimConstant.Employee.RED_DOT_EMPLOYEE_GROWTH);
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

            boolean remove = playerPackService.removeItem(ctx.getPlayer(), employeeProfileCfg.getDuplicatetoShard().get(1), curCfg.getStarUpCost(), AddType.SIM_EMPLOYEE_STAR_UP).success();
            if (!remove) {
                log.warn("升星雇员失败, 扣除碎片道具失败 playerId={},employeeId={},need={}", ctx.playerId(), employeeId, curCfg.getStarUpCost());
                res.code = Code.NOT_ENOUGH;
                ctx.send(res);
                return;
            }
            data.setStar(data.getStar() + 1);
            res.star = data.getStar();
            employeeRedDotService.updateRedDots(ctx.playerId(),
                    SimConstant.Employee.RED_DOT_EMPLOYEE_GROWTH);
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
     * 直接用 ctx 投递: 登录期补报时 ctx 尚未入 registry, 走 playerId 查找会被丢弃。
     */
    @Override
    public void reportTaskState(SimPlayerContext ctx, Consumer<ActionConditionEvent> sink) {
        reportEmployeeCounts(ctx, sink);
    }

    private void reportEmployeeCounts(SimPlayerContext ctx) {
        reportEmployeeCounts(ctx, e -> simTaskService.onConditionEvent(ctx, e));
    }

    private void reportEmployeeCounts(SimPlayerContext ctx, Consumer<ActionConditionEvent> sink) {
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
                SimConditionEventFactory.emitOwnershipCounts(sink,
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

            //主管加成: 按该职业可任职建筑的产出类型过滤后返回
            ManageBonus manageBonus = managerBonusFiltered(ctx, cfg.getProfessionID());
            res.manageEmployeeBonus = toKVList(manageBonus.modifier());
            res.manageEmployeeFixBonus = toKVList(manageBonus.buff());

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
                    //主管加成: 原样返回雇员技能配置, 不做过滤 (11不拆, 客户端自行分类汇总)
                    ManageBonus bonus = employeeSkillBonus(value.getEmployeeId());
                    detailInfo.manageEmployeeBonus = toKVList(bonus.modifier());
                    detailInfo.manageEmployeeFixBonus = toKVList(bonus.buff());
                    //普通加成: 该雇员等级加成, 同样原样返回不拆11
                    detailInfo.employeeBonus = toKVList(employeeNormalBonus(value));
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
     * 单个雇员的普通加成 (等级加成; 单位千分比), 不含勋章, key 保留 MANAGE_ARRT(11) 不拆。
     * 与 {@link #computeTypeBonusFixed} 的雇员聚合口径同源, 供雇员列表逐雇员返回。
     */
    private Map<BuildingOutputType, Integer> employeeNormalBonus(SimEmployeeData emp) {
        EmployeeLevelCfg levelCfg = getLevelCfg(emp.getEmployeeId(), emp.getLevel());
        if (levelCfg == null) {
            return Collections.emptyMap();
        }
        Map<BuildingOutputType, Integer> map = new HashMap<>();
        sumBouns(map, levelCfg.getAttributeValue());
        return map;
    }

    /**
     * 仅仅获取主管的加成: 当前任职于该职业建筑的主管雇员技能加成 (Modifier 百分比 + Buff 固定值)。
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
        return employeeSkillBonus(managerId);
    }

    /**
     * 雇员技能配置的原始加成: EmployeeProfile.SkillIdList 对应 EmployeeSkillConfig 的
     * Modifier(百分比) + Buff(固定值), 按 {@link BuildingOutputType} 归类, 不做任何过滤。
     */
    private ManageBonus employeeSkillBonus(int employeeId) {
        EmployeeProfileCfg profileCfg = GameDataManager.getEmployeeProfileCfg(employeeId);
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
     * 当前任职于该职业建筑的主管加成, 已按该职业可任职建筑的产出类型过滤。
     * 供任命主管返回与建筑信息返回共用, 保证两者同名字段取值一致。
     */
    public ManageBonus managerBonusFiltered(SimPlayerContext ctx, int employeeProfile) {
        return filterByProfessionOutput(manageEmployeeBonus(ctx, employeeProfile), employeeProfile);
    }

    /**
     * 主管加成按建筑产出类型过滤: 某职业只能担任 EmployeeProfile 匹配的建筑主管,
     * 仅保留这些建筑产出类型 (typeValue 的 {@link BuildingOutputType#bonusGroup()}) 对应的加成。
     */
    private ManageBonus filterByProfessionOutput(ManageBonus bonus, int professionId) {
        if (bonus.isEmpty()) {
            return bonus;
        }
        Set<BuildingOutputType> allowed = EnumSet.noneOf(BuildingOutputType.class);
        for (BuildingAreaTableCfg areaCfg : GameDataManager.getBuildingAreaTableCfgList()) {
            List<Integer> typeValues = areaCfg.getTypeValue();
            if (areaCfg.getEmployeeProfile() != professionId || typeValues == null) {
                continue;
            }
            for (Integer typeValue : typeValues) {
                BuildingOutputType type = BuildingOutputType.fromCode(typeValue);
                if (type != null) {
                    allowed.add(type.bonusGroup());
                }
            }
        }
        return new ManageBonus(retainAllowed(bonus.modifier(), allowed), retainAllowed(bonus.buff(), allowed));
    }

    private Map<BuildingOutputType, Integer> retainAllowed(Map<BuildingOutputType, Integer> src, Set<BuildingOutputType> allowed) {
        if (src.isEmpty() || allowed.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<BuildingOutputType, Integer> result = new HashMap<>(src.size());
        for (Map.Entry<BuildingOutputType, Integer> en : src.entrySet()) {
            BuildingOutputType key = en.getKey();
            if (allowed.contains(key)) {
                if (key == BuildingOutputType.MANAGE_ARRT) {
                    result.put(BuildingOutputType.SERVICE, en.getValue());
                    result.put(BuildingOutputType.AWARENESS, en.getValue());
                    result.put(BuildingOutputType.EXPOSURE, en.getValue());
                } else {
                    result.put(key, en.getValue());
                }
            }
        }
        return result;
    }

    /**
     * 加成 Map 转 KVInfo 列表 (key=产出类型 code, value=加成值); 空则返回 null。
     */
    public List<KVInfo> toKVList(Map<BuildingOutputType, Integer> bonus) {
        if (bonus.isEmpty()) {
            return null;
        }
        List<KVInfo> list = new ArrayList<>(bonus.size());
        for (Map.Entry<BuildingOutputType, Integer> en : bonus.entrySet()) {
            list.add(new KVInfo(en.getKey().getCode(), en.getValue()));
        }
        return list;
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
     * @param poolId 卡池id
     */
    public void onPool(SimPlayerContext ctx, int poolId) {
        ResEmployeePool res = new ResEmployeePool(Code.SUCCESS);
        try {
            PoolListCfg poolCfg = configCache.getOpenPoolCfg(poolId, SimConstant.PoolList.TYPE_EMPLOYEE);
            if (poolCfg == null) {
                log.warn("获取雇员卡池失败,卡池不存在、未开启、未到开放时间或类型错误 playerId={},poolId={}", ctx.playerId(), poolId);
                res.code = Code.PARAM_ERROR;
                ctx.send(res);
                return;
            }

            EmployeePoolCfg employeePoolCfg = GameDataManager.getEmployeePoolCfg(poolCfg.getDropItem());
            if (employeePoolCfg == null || employeePoolCfg.getDetailedDropItem() == null) {
                log.warn("获取雇员卡池失败,掉落配置不存在 playerId={},poolId={},dropItem={}", ctx.playerId(), poolId, poolCfg.getDropItem());
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
