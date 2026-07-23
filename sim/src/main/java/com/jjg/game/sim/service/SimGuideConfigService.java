package com.jjg.game.sim.service;

import com.jjg.game.core.listener.ConfigExcelChangeListener;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.GuideCfg;
import com.jjg.game.sim.constant.SimConstant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Guide.xlsx 的服务端触发索引；步骤链与表现字段由客户端使用。 */
@Component
public class SimGuideConfigService implements ConfigExcelChangeListener {
    private static final Logger log = LoggerFactory.getLogger(SimGuideConfigService.class);

    private volatile Map<TriggerKey, List<Integer>> triggerGroups = Collections.emptyMap();
    private volatile Map<Integer, Integer> groupConditions = Collections.emptyMap();

    @Override
    public void initSampleCallbackCollector() {
        addInitSampleFileObserveWithCallBack(GuideCfg.EXCEL_NAME, this::load);
    }

    @Override
    public void changeSampleCallbackCollector() {
        addChangeSampleFileObserveWithCallBack(GuideCfg.EXCEL_NAME, this::load);
    }

    public void load() {
        List<GuideCfg> all = GameDataManager.getGuideCfgList();
        if (all == null || all.isEmpty()) {
            triggerGroups = Collections.emptyMap();
            groupConditions = Collections.emptyMap();
            log.warn("加载新手引导触发索引失败: Guide 配置为空");
            return;
        }
        Map<TriggerKey, Set<Integer>> groups = new HashMap<>();
        Map<Integer, Integer> conditions = new HashMap<>();
        for (GuideCfg cfg : all) {
            if (cfg == null || cfg.getGuideGroupId() <= 0 || !validCondition(cfg.getCondition())) {
                continue;
            }
            int param = cfg.getCondition() == SimConstant.GuideCondition.NEW_PLAYER ? 0 : cfg.getParam1();
            // 条件 2~5 只有配置了 param1 的入口行参与服务端触发，其余步骤行忽略。
            if (cfg.getCondition() != SimConstant.GuideCondition.NEW_PLAYER && param <= 0) {
                continue;
            }
            Integer oldCondition = conditions.putIfAbsent(cfg.getGuideGroupId(), cfg.getCondition());
            if (oldCondition != null && oldCondition != cfg.getCondition()) {
                log.warn("同一引导组配置了不同触发类型 groupId={},old={},new={},guideId={}",
                        cfg.getGuideGroupId(), oldCondition, cfg.getCondition(), cfg.getId());
                continue;
            }
            groups.computeIfAbsent(new TriggerKey(cfg.getCondition(), param), ignored -> new LinkedHashSet<>())
                    .add(cfg.getGuideGroupId());
        }
        Map<TriggerKey, List<Integer>> immutable = new HashMap<>();
        groups.forEach((key, value) -> immutable.put(key,
                Collections.unmodifiableList(new ArrayList<>(value))));
        triggerGroups = Collections.unmodifiableMap(immutable);
        groupConditions = Collections.unmodifiableMap(conditions);
        log.info("加载新手引导触发索引完成 triggerCount={},groupCount={}",
                triggerGroups.size(), groupConditions.size());
    }

    public List<Integer> groupsFor(int condition, int param) {
        int normalized = condition == SimConstant.GuideCondition.NEW_PLAYER ? 0 : param;
        return triggerGroups.getOrDefault(new TriggerKey(condition, normalized), Collections.emptyList());
    }

    public int conditionOfGroup(int guideGroupId) {
        return groupConditions.getOrDefault(guideGroupId, 0);
    }

    public boolean containsGroup(int guideGroupId) {
        return groupConditions.containsKey(guideGroupId);
    }

    /** 是否存在指定引导步骤。 */
    public boolean containsGuide(int guideId) {
        return guideId > 0 && GameDataManager.getGuideCfg(guideId) != null;
    }

    /** 根据步骤ID获取所属引导组。 */
    public int groupOfGuide(int guideId) {
        GuideCfg cfg = GameDataManager.getGuideCfg(guideId);
        return cfg == null ? 0 : cfg.getGuideGroupId();
    }

    /** 获取指定引导组包含的全部步骤ID。 */
    public List<Integer> guideIdsOfGroup(int guideGroupId) {
        List<GuideCfg> all = GameDataManager.getGuideCfgList();
        if (all == null || all.isEmpty()) return Collections.emptyList();
        List<Integer> result = new ArrayList<>();
        for (GuideCfg cfg : all) {
            if (cfg != null && cfg.getGuideGroupId() == guideGroupId) result.add(cfg.getId());
        }
        result.sort(Integer::compareTo);
        return Collections.unmodifiableList(result);
    }

    /** 获取配置中的全部引导步骤ID。 */
    public List<Integer> allGuideIds() {
        List<GuideCfg> all = GameDataManager.getGuideCfgList();
        if (all == null || all.isEmpty()) return Collections.emptyList();
        List<Integer> result = new ArrayList<>(all.size());
        for (GuideCfg cfg : all) if (cfg != null && cfg.getId() > 0) result.add(cfg.getId());
        result.sort(Integer::compareTo);
        return Collections.unmodifiableList(result);
    }

    private boolean validCondition(int condition) {
        return condition >= SimConstant.GuideCondition.NEW_PLAYER
                && condition <= SimConstant.GuideCondition.ITEM_GAINED;
    }

    private record TriggerKey(int condition, int param) {
    }
}
