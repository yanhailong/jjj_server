package com.jjg.game.sim.service;

import com.jjg.game.core.constant.TaskConstant;
import com.jjg.game.core.listener.ConfigExcelChangeListener;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.TaskCfg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    @Override
    public void initSampleCallbackCollector() {
        addInitSampleFileObserveWithCallBack(TaskCfg.EXCEL_NAME, this::loadChains);
    }

    @Override
    public void changeSampleCallbackCollector() {
        addChangeSampleFileObserveWithCallBack(TaskCfg.EXCEL_NAME, this::loadChains);
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
        for (TaskCfg cfg : all) {
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
        log.info("加载 sim 任务链: 主线 {} 条, 成就组 {} 个", tmpMain.size(), tmpGroups.size());
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
}
