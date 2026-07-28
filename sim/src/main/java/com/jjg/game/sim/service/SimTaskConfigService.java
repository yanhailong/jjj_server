package com.jjg.game.sim.service;

import com.jjg.game.core.base.condition.numeric.ConditionRuleRegistry;
import com.jjg.game.core.base.condition.numeric.ConditionSpec;
import com.jjg.game.core.base.condition.numeric.PreparedCondition;
import com.jjg.game.core.base.condition.numeric.StateConditionEvent;
import com.jjg.game.core.constant.TaskConstant;
import com.jjg.game.core.listener.ConfigExcelChangeListener;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.TaskCfg;
import com.jjg.game.sampledata.bean.ConditionCfg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * sim 任务(主线/成就)配置链。
 * <p>
 * 配置表 {@code task.xlsx} 缺"后置任务ID"字段(表头该列字段名为空), 故链由"链内任务 id 升序"推导:
 * <ul>
 *   <li>主线(taskType=2): 全部按 id 升序串成单链;</li>
 *   <li>成就(taskType=3): 按 group 分链, 组内按 id 升序成阶梯。</li>
 * </ul>
 *
 * @author 11
 * @date 2026/6/25
 */
@Component
public class SimTaskConfigService implements ConfigExcelChangeListener {
    private static final Logger log = LoggerFactory.getLogger(SimTaskConfigService.class);

    /**
     * 主线链: 按 id 升序的任务 id 列表
     */
    private volatile List<Integer> mainChain = Collections.emptyList();

    /**
     * 成就链: group -> 按 id 升序的任务 id 列表
     */
    private volatile Map<Integer, List<Integer>> achievementGroups = Collections.emptyMap();

    /**
     * 任务 id -> 链内下一节点 id (0 表示末节点)。主线与成就合并索引, 便于 O(1) 取 next。
     */
    private volatile Map<Integer, Integer> nextIndex = Collections.emptyMap();

    /** 任务 id -> 已校验条件及兼容旧数据的 Redis featureId。 */
    private volatile Map<Integer, TaskConditionDef> conditions = Collections.emptyMap();

    private final ConditionRuleRegistry conditionRules;

    public SimTaskConfigService() {
        this(ConditionRuleRegistry.standard());
    }

    @Autowired
    public SimTaskConfigService(ConditionRuleRegistry conditionRules) {
        this.conditionRules = conditionRules;
    }

    @Override
    public void initSampleCallbackCollector() {
        addInitSampleFileObserveWithCallBack(TaskCfg.EXCEL_NAME, this::loadChains)
                .addInitSampleFileObserveWithCallBack(ConditionCfg.EXCEL_NAME, this::loadChains);
    }

    @Override
    public void changeSampleCallbackCollector() {
        addChangeSampleFileObserveWithCallBack(TaskCfg.EXCEL_NAME, this::loadChains)
                .addChangeSampleFileObserveWithCallBack(ConditionCfg.EXCEL_NAME, this::loadChains);
    }

    /**
     * 加载主线/成就链 (初始化与热更共用)
     */
    public void loadChains() {
        List<TaskCfg> all = GameDataManager.getTaskCfgList();
        if (all == null || all.isEmpty()) {
            log.warn("加载 sim 任务链失败: task 配置为空");
            return;
        }
        List<TaskCfg> mains = new ArrayList<>();
        Map<Integer, List<TaskCfg>> groups = new HashMap<>();
        Map<Integer, TaskConditionDef> tmpConditions = new HashMap<>();
        for (TaskCfg cfg : all) {
            if (cfg.getTaskType() != TaskConstant.TaskType.MAIN_LINE
                    && cfg.getTaskType() != TaskConstant.TaskType.ACHIEVEMENT) {
                continue;
            }
            PreparedCondition prepared;
            try {
                prepared = conditionRules.prepare(ConditionSpec.from(cfg.getTaskConditionId()));
            } catch (IllegalArgumentException e) {
                log.warn("sim 任务条件配置非法, 不加入任务链 taskId={},condition={},error={}",
                        cfg.getId(), cfg.getTaskConditionId(), e.getMessage());
                continue;
            }
            if (prepared.eventType() == StateConditionEvent.class
                    && !SimTaskStateEventFactory.supports(prepared)) {
                log.warn("sim 任务状态条件缺少可靠数据源, 不加入任务链 taskId={},conditionId={}",
                        cfg.getId(), prepared.spec().id());
                continue;
            }
            ConditionCfg conditionCfg = GameDataManager.getConditionCfg(prepared.spec().id());
            String legacyType = conditionCfg == null ? null : conditionCfg.getTriggerEventType();
            String counterType = legacyType == null || legacyType.isBlank()
                    ? "condition" + prepared.spec().id() : legacyType;
            tmpConditions.put(cfg.getId(), new TaskConditionDef(prepared, counterType));
            if (cfg.getTaskType() == TaskConstant.TaskType.MAIN_LINE) {
                mains.add(cfg);
            } else if (cfg.getTaskType() == TaskConstant.TaskType.ACHIEVEMENT) {
                groups.computeIfAbsent(cfg.getGroup(), k -> new ArrayList<>()).add(cfg);
            }
        }

        Map<Integer, Integer> tmpNext = new HashMap<>();

        mains.sort(Comparator.comparingInt(TaskCfg::getId));
        List<Integer> tmpMain = new ArrayList<>(mains.size());
        buildChain(mains, tmpMain, tmpNext);

        Map<Integer, List<Integer>> tmpGroups = new HashMap<>();
        for (Map.Entry<Integer, List<TaskCfg>> en : groups.entrySet()) {
            List<TaskCfg> list = en.getValue();
            list.sort(Comparator.comparingInt(TaskCfg::getId));
            List<Integer> ids = new ArrayList<>(list.size());
            buildChain(list, ids, tmpNext);
            tmpGroups.put(en.getKey(), Collections.unmodifiableList(ids));
        }

        this.mainChain = Collections.unmodifiableList(tmpMain);
        this.achievementGroups = Collections.unmodifiableMap(tmpGroups);
        this.nextIndex = Collections.unmodifiableMap(tmpNext);
        this.conditions = Collections.unmodifiableMap(tmpConditions);
        //列出主线节点 id: 条件校验失败的节点会被上面的 continue 排除, 对比配置表即可发现缺了谁
        log.info("加载 sim 任务链: 主线 {} 条, 成就组 {} 个, 主线节点={}",
                tmpMain.size(), tmpGroups.size(), tmpMain);
    }

    /**
     * 把已按 id 升序的配置列表串成链: 写出 id 顺序列表, 同时登记每个节点的 next。
     */
    private void buildChain(List<TaskCfg> sorted, List<Integer> outIds, Map<Integer, Integer> outNext) {
        for (int i = 0; i < sorted.size(); i++) {
            int id = sorted.get(i).getId();
            outIds.add(id);
            outNext.put(id, i + 1 < sorted.size() ? sorted.get(i + 1).getId() : 0);
        }
    }

    // ---------------------------------------------------------------------
    // 主线
    // ---------------------------------------------------------------------

    /**
     * 主线首节点 id; 无主线返回 0
     */
    public int firstMain() {
        List<Integer> chain = this.mainChain;
        return chain.isEmpty() ? 0 : chain.getFirst();
    }

    // ---------------------------------------------------------------------
    // 成就
    // ---------------------------------------------------------------------

    /**
     * 所有成就组 id
     */
    public Set<Integer> achievementGroupIds() {
        return this.achievementGroups.keySet();
    }

    /**
     * 成就组首节点 id; 组不存在返回 0
     */
    public int firstOf(int group) {
        List<Integer> chain = this.achievementGroups.get(group);
        return chain == null || chain.isEmpty() ? 0 : chain.getFirst();
    }

    // ---------------------------------------------------------------------
    // 通用
    // ---------------------------------------------------------------------

    /**
     * 链内下一节点 id; 末节点或未知 id 返回 0
     */
    public int next(int taskId) {
        return this.nextIndex.getOrDefault(taskId, 0);
    }

    public TaskConditionDef conditionOf(int taskId) {
        return conditions.get(taskId);
    }

    /** 配置加载后不可变，可被玩家热路径无锁复用。 */
    public record TaskConditionDef(PreparedCondition condition, String counterType) {
    }
}
