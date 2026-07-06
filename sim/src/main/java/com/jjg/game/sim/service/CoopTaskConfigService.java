package com.jjg.game.sim.service;

import com.jjg.game.core.constant.TaskConstant;
import com.jjg.game.core.listener.ConfigExcelChangeListener;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.GlobalConfigCfg;
import com.jjg.game.sampledata.bean.TaskCfg;
import com.jjg.game.sim.constant.SimConstant;
import com.jjg.game.sim.data.CoopTaskRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 多人协作任务配置缓存 (task.xlsx taskType=4 池 + global.xlsx 全局项)。
 * <p>
 * 初始化与热更共用加载入口; 池与规则均为不可变快照, 玩家线程无锁读取。
 *
 * @author 11
 * @date 2026/7/6
 */
@Component
public class CoopTaskConfigService implements ConfigExcelChangeListener {
    private static final Logger log = LoggerFactory.getLogger(CoopTaskConfigService.class);

    //taskConditionId 至少包含 [conditionId, gameType, spinBudget, modeId, modeCount]
    private static final int CONDITION_MIN_SIZE = 5;

    //任务池 (taskType=4, 按 id 升序)
    private volatile List<Integer> poolTaskIds = Collections.emptyList();

    //global 全局项
    private volatile int dailyClaimLimit = 3;
    private volatile int dailyPoolCount = 10;
    private volatile int refreshCostItemId;
    private volatile long refreshCostCount;

    @Override
    public void initSampleCallbackCollector() {
        addInitSampleFileObserveWithCallBack(TaskCfg.EXCEL_NAME, this::loadPool);
        addInitSampleFileObserveWithCallBack(GlobalConfigCfg.EXCEL_NAME, this::loadGlobal);
    }

    @Override
    public void changeSampleCallbackCollector() {
        addChangeSampleFileObserveWithCallBack(TaskCfg.EXCEL_NAME, this::loadPool);
        addChangeSampleFileObserveWithCallBack(GlobalConfigCfg.EXCEL_NAME, this::loadGlobal);
    }

    private void loadPool() {
        List<TaskCfg> all = GameDataManager.getTaskCfgList();
        if (all == null || all.isEmpty()) {
            log.warn("加载多人任务池失败: task 配置为空");
            return;
        }
        List<Integer> ids = new ArrayList<>();
        for (TaskCfg cfg : all) {
            if (cfg.getTaskType() != TaskConstant.TaskType.COOP) {
                continue;
            }
            if (parseRule(cfg) == null) {
                log.warn("多人任务[{}]条件配置非法, 不入池 taskConditionId={}", cfg.getId(), cfg.getTaskConditionId());
                continue;
            }
            ids.add(cfg.getId());
        }
        ids.sort(Comparator.naturalOrder());
        this.poolTaskIds = Collections.unmodifiableList(ids);
        log.info("加载多人任务池: {} 条", ids.size());
    }

    private void loadGlobal() {
        this.dailyClaimLimit = positiveInt(SimConstant.Common.COOP_DAILY_CLAIM_LIMIT_ID, 3);
        this.dailyPoolCount = positiveInt(SimConstant.Common.COOP_DAILY_POOL_COUNT_ID, 10);
        GlobalConfigCfg costCfg = GameDataManager.getGlobalConfigCfg(SimConstant.Common.COOP_REFRESH_COST_ID);
        int itemId = 0;
        long count = 0;
        if (costCfg != null && costCfg.getValue() != null) {
            String[] parts = costCfg.getValue().trim().split("_");
            try {
                if (parts.length >= 2) {
                    itemId = Integer.parseInt(parts[0].trim());
                    count = Long.parseLong(parts[1].trim());
                }
            } catch (NumberFormatException e) {
                log.warn("多人任务刷新道具配置非法 value={}", costCfg.getValue());
            }
        }
        if (itemId <= 0 || count <= 0) {
            log.warn("多人任务刷新道具配置缺失, 付费刷新将不可用 id={}", SimConstant.Common.COOP_REFRESH_COST_ID);
            itemId = 0;
            count = 0;
        }
        this.refreshCostItemId = itemId;
        this.refreshCostCount = count;
    }

    private int positiveInt(int id, int defaultValue) {
        GlobalConfigCfg cfg = GameDataManager.getGlobalConfigCfg(id);
        if (cfg == null || cfg.getIntValue() <= 0) {
            log.warn("多人任务全局配置缺失或无效, 使用默认值 id={},defaultValue={}", id, defaultValue);
            return defaultValue;
        }
        return cfg.getIntValue();
    }

    /**
     * 解析任务规则; 配置非法返回 null。
     * <p>
     * taskConditionId = [conditionId, gameType(0=任意), 下注具体次数(团队血条), 模式id, 触发模式次数];
     * MinandMax = [总人数下限, 总人数上限]; Duration = 任务时限(分)。
     */
    public CoopTaskRule parseRule(TaskCfg cfg) {
        if (cfg == null || cfg.getTaskType() != TaskConstant.TaskType.COOP) {
            return null;
        }
        List<Long> cond = cfg.getTaskConditionId();
        if (cond == null || cond.size() < CONDITION_MIN_SIZE) {
            return null;
        }
        int spinBudget = cond.get(2).intValue();
        int modeCount = cond.get(4).intValue();
        if (spinBudget <= 0 || modeCount <= 0) {
            return null;
        }
        List<Integer> minMax = cfg.getMinandMax();
        int min = 1;
        int max = 1;
        if (minMax != null && minMax.size() >= 2) {
            min = Math.max(1, minMax.get(0));
            max = Math.max(min, minMax.get(1));
        }
        return new CoopTaskRule(cfg.getId(), cond.get(0).intValue(), cond.get(1).intValue(),
                spinBudget, cond.get(3).intValue(), modeCount, min, max,
                Math.max(0, cfg.getDuration()));
    }

    /**
     * 解析任务规则 (按任务id); 非多人任务或配置非法返回 null。
     */
    public CoopTaskRule ruleOf(int taskId) {
        return parseRule(GameDataManager.getTaskCfg(taskId));
    }

    public List<Integer> getPoolTaskIds() {
        return poolTaskIds;
    }

    public int getDailyClaimLimit() {
        return dailyClaimLimit;
    }

    public int getDailyPoolCount() {
        return dailyPoolCount;
    }

    public int getRefreshCostItemId() {
        return refreshCostItemId;
    }

    public long getRefreshCostCount() {
        return refreshCostCount;
    }
}
