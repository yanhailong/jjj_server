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
    private volatile Map<Integer, String> groupPathNames = Collections.emptyMap();
    private volatile Map<Integer, Integer> guideGroups = Collections.emptyMap();
    private volatile Map<Integer, List<Integer>> groupGuideIds = Collections.emptyMap();
    private volatile Map<Integer, Set<Integer>> groupSkipGuideIds = Collections.emptyMap();

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
            groupPathNames = Collections.emptyMap();
            guideGroups = Collections.emptyMap();
            groupGuideIds = Collections.emptyMap();
            groupSkipGuideIds = Collections.emptyMap();
            log.warn("加载新手引导触发索引失败: Guide 配置为空");
            return;
        }
        Map<TriggerKey, Set<Integer>> groups = new HashMap<>();
        Map<Integer, Integer> conditions = new HashMap<>();
        Map<Integer, String> pathNames = new HashMap<>();
        Map<Integer, Integer> guides = new HashMap<>();
        Map<Integer, List<Integer>> guidesByGroup = new HashMap<>();
        Map<Integer, Set<Integer>> skipGuidesByGroup = new HashMap<>();
        for (GuideCfg cfg : all) {
            if (cfg == null) {
                continue;
            }
            // 关闭的引导组不建立任何触发、步骤或跳过索引，后续不会参与条件判断。
            if (!cfg.getIsOpen()) {
                continue;
            }
            if (cfg.getGuideGroupId() <= 0 || !validCondition(cfg.getCondition())) {
                continue;
            }
            int param = conditionNeedsParam(cfg.getCondition()) ? cfg.getParam() : 0;
            if (conditionNeedsParam(cfg.getCondition()) && param <= 0) {
                continue;
            }
            Integer oldCondition = conditions.putIfAbsent(cfg.getGuideGroupId(), cfg.getCondition());
            if (oldCondition != null && oldCondition != cfg.getCondition()) {
                log.warn("同一引导组配置了不同触发类型 groupId={},old={},new={},guideId={}",
                        cfg.getGuideGroupId(), oldCondition, cfg.getCondition(), cfg.getId());
                continue;
            }
            String pathName = normalizePathName(cfg.getPathName());
            String oldPathName = pathNames.putIfAbsent(cfg.getGuideGroupId(), pathName);
            if (oldPathName != null && !oldPathName.equals(pathName)) {
                log.warn("同一引导组配置了不同触发场景 groupId={},old={},new={},guideId={}",
                        cfg.getGuideGroupId(), oldPathName, pathName, cfg.getId());
                continue;
            }
            groups.computeIfAbsent(new TriggerKey(cfg.getCondition(), param), ignored -> new LinkedHashSet<>())
                    .add(cfg.getGuideGroupId());

            List<Integer> configuredGuideIds = cfg.getGuideIdList();
            List<Integer> normalizedGuideIds = new ArrayList<>();
            if (configuredGuideIds != null) {
                for (Integer guideId : configuredGuideIds) {
                    if (guideId != null && guideId > 0 && !normalizedGuideIds.contains(guideId)) {
                        normalizedGuideIds.add(guideId);
                    }
                }
            }
            // 兼容尚未配置 GuideIdList 的单步骤引导组，以配置行 ID 作为步骤 ID。
            if (normalizedGuideIds.isEmpty() && cfg.getId() > 0) {
                normalizedGuideIds.add(cfg.getId());
            }
            for (int guideId : normalizedGuideIds) {
                Integer oldGroupId = guides.putIfAbsent(guideId, cfg.getGuideGroupId());
                if (oldGroupId != null && oldGroupId != cfg.getGuideGroupId()) {
                    log.warn("同一引导步骤配置到了不同引导组 guideId={},oldGroupId={},newGroupId={}",
                            guideId, oldGroupId, cfg.getGuideGroupId());
                }
            }
            guidesByGroup.put(cfg.getGuideGroupId(),
                    Collections.unmodifiableList(new ArrayList<>(normalizedGuideIds)));
            Set<Integer> normalizedSkipGuideIds = new LinkedHashSet<>();
            List<Integer> configuredSkipGuideIds = cfg.getSkipGuideId();
            if (configuredSkipGuideIds != null) {
                for (Integer guideId : configuredSkipGuideIds) {
                    if (guideId == null || guideId <= 0) continue;
                    if (!normalizedGuideIds.contains(guideId)) {
                        log.warn("跳过引导ID不属于当前引导组，已忽略 groupId={},guideId={}",
                                cfg.getGuideGroupId(), guideId);
                        continue;
                    }
                    normalizedSkipGuideIds.add(guideId);
                }
            }
            skipGuidesByGroup.put(cfg.getGuideGroupId(),
                    Collections.unmodifiableSet(normalizedSkipGuideIds));
        }
        Map<TriggerKey, List<Integer>> immutable = new HashMap<>();
        groups.forEach((key, value) -> immutable.put(key,
                Collections.unmodifiableList(new ArrayList<>(value))));
        triggerGroups = Collections.unmodifiableMap(immutable);
        groupConditions = Collections.unmodifiableMap(conditions);
        groupPathNames = Collections.unmodifiableMap(pathNames);
        guideGroups = Collections.unmodifiableMap(guides);
        groupGuideIds = Collections.unmodifiableMap(guidesByGroup);
        groupSkipGuideIds = Collections.unmodifiableMap(skipGuidesByGroup);
        int skipGuideCount = groupSkipGuideIds.values().stream().mapToInt(Set::size).sum();
        long pathRestrictedGroupCount = groupPathNames.values().stream().filter(value -> !value.isEmpty()).count();
        log.info("加载新手引导触发索引完成 triggerCount={},groupCount={},guideCount={},skipGuideCount={},pathRestrictedGroupCount={}",
                triggerGroups.size(), groupConditions.size(), guideGroups.size(), skipGuideCount,
                pathRestrictedGroupCount);
    }

    public List<Integer> groupsFor(int condition, int param) {
        int normalized = condition == SimConstant.GuideCondition.NEW_PLAYER ? 0 : param;
        return triggerGroups.getOrDefault(new TriggerKey(condition, normalized), Collections.emptyList());
    }

    /** 获取指定条件下不高于当前值的全部配置参数，供“达到等级”补扫使用。 */
    public List<Integer> paramsAtOrBelow(int condition, int currentValue) {
        if (currentValue <= 0) return Collections.emptyList();
        List<Integer> params = triggerGroups.keySet().stream()
                .filter(key -> key.condition() == condition && key.param() > 0 && key.param() <= currentValue)
                .map(TriggerKey::param)
                .distinct()
                .sorted()
                .toList();
        return params.isEmpty() ? Collections.emptyList() : params;
    }

    public int conditionOfGroup(int guideGroupId) {
        return groupConditions.getOrDefault(guideGroupId, 0);
    }

    /** 获取引导组要求的触发场景；空字符串表示不限制场景。 */
    public String pathNameOfGroup(int guideGroupId) {
        return groupPathNames.getOrDefault(guideGroupId, "");
    }

    public boolean containsGroup(int guideGroupId) {
        return groupConditions.containsKey(guideGroupId);
    }

    /** 是否存在指定引导步骤。 */
    public boolean containsGuide(int guideId) {
        return guideId > 0 && guideGroups.containsKey(guideId);
    }

    /** 根据步骤ID获取所属引导组。 */
    public int groupOfGuide(int guideId) {
        return guideGroups.getOrDefault(guideId, 0);

    }

    /** 获取指定引导组包含的全部步骤ID。 */
    public List<Integer> guideIdsOfGroup(int guideGroupId) {
        return groupGuideIds.getOrDefault(guideGroupId, Collections.emptyList());

    }

    /** 获取指定引导组中允许在重新进入大厅时自动完成的步骤ID。 */
    public Set<Integer> skipGuideIdsOfGroup(int guideGroupId) {
        return groupSkipGuideIds.getOrDefault(guideGroupId, Collections.emptySet());
    }

    /** 获取配置中的全部引导步骤ID。 */
    public List<Integer> allGuideIds() {
        List<Integer> result = new ArrayList<>(guideGroups.keySet());

        result.sort(Integer::compareTo);
        return Collections.unmodifiableList(result);
    }

    private boolean validCondition(int condition) {
        return condition == SimConstant.GuideCondition.NEW_PLAYER
                || condition == SimConstant.GuideCondition.SCENE_TOTAL_LEVEL
                || condition == SimConstant.GuideCondition.PLAYER_LEVEL
                || condition == SimConstant.GuideCondition.ITEM_NOT_ENOUGH
                || condition == SimConstant.GuideCondition.ITEM_GAINED
                || condition == SimConstant.GuideCondition.FUNCTION_UNLOCKED
                || condition == SimConstant.GuideCondition.GUIDE_GROUP_FINISHED;
    }

    private boolean conditionNeedsParam(int condition) {
        return condition != SimConstant.GuideCondition.NEW_PLAYER;
    }

    private String normalizePathName(String pathName) {
        if (pathName == null) {
            return "";
        }
        String normalized = pathName.trim();
        // PathName 列定义为 string，但策划填写数字时 Excel 解析器会得到 "1.0"。
        if (normalized.endsWith(".0")) {
            String integerPart = normalized.substring(0, normalized.length() - 2);
            if (!integerPart.isEmpty() && integerPart.chars().allMatch(Character::isDigit)) {
                return integerPart;
            }
        }
        return normalized;
    }

    private record TriggerKey(int condition, int param) {
    }
}
