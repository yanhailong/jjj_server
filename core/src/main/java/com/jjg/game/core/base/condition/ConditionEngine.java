package com.jjg.game.core.base.condition;

/**
 * @author lm
 * @date 2026/1/14 10:36
 */

import com.jjg.game.core.data.Player;
import com.jjg.game.core.base.condition.numeric.ConditionEvent;
import com.jjg.game.core.base.condition.numeric.ConditionRuleRegistry;
import com.jjg.game.core.base.condition.numeric.ConditionSpec;
import com.jjg.game.core.base.condition.numeric.ConditionUpdate;
import com.jjg.game.core.base.condition.numeric.PreparedCondition;
import com.jjg.game.core.listener.ConfigExcelChangeListener;
import com.jjg.game.sampledata.bean.*;
import org.springframework.stereotype.Component;

/**
 * condition 的统一门面。
 * <p>
 * condition 表的数值配置统一由 {@link ConditionRuleRegistry} 编译和求值；字符串表达式相关方法仅作为
 * activity、drop、game-function 等既有功能的兼容适配层保留。新增条件不得在旧 handler 中重复定义语义，
 * 如需接入旧表达式，应由 handler 把现有事件或数据源转换为统一事实事件后委托注册表。
 */
@Component
public class ConditionEngine implements ConfigExcelChangeListener {

    private final ConditionTreeCache treeCache;
    private final ConditionRuleRegistry numericRegistry;

    public ConditionEngine(ConditionParser parser, ConditionRuleRegistry numericRegistry) {
        this.treeCache = new ConditionTreeCache(parser);
        this.numericRegistry = numericRegistry;
    }

    /**
     * 将 condition 表数值列表解析并校验为可在热路径复用的条件。
     */
    public PreparedCondition prepare(java.util.List<Long> config) {
        return prepare(ConditionSpec.from(config));
    }

    /**
     * 将后台文本配置解析并校验；下划线和星号都可作为参数分隔符。
     */
    public PreparedCondition prepare(String config) {
        return prepare(ConditionSpec.parse(config));
    }

    public PreparedCondition prepare(ConditionSpec spec) {
        return numericRegistry.prepare(spec);
    }

    public boolean supports(int conditionId) {
        return numericRegistry.supports(conditionId);
    }

    /**
     * 低频便捷入口。高频调用应在配置加载时保存 {@link PreparedCondition}，避免重复校验。
     */
    public ConditionUpdate evaluate(ConditionSpec spec, ConditionEvent event) {
        return prepare(spec).evaluate(event);
    }

    public boolean check(Player player, String prefix, String expr) {
        return check(player, prefix, null, expr);
    }

    public boolean check(Player player, String prefix, Object event, String expr) {
        ConditionNode node = treeCache.getOrParse(expr);
        MatchResultData r = node.match(new ConditionContext(player, event, prefix));
        return r.result() == MatchResult.MATCH;
    }

    public MatchResultData addProgressAndCheck(Player player, Object event, String expr) {
        return addProgressAndCheck(player, event, "", expr);
    }

    public MatchResultData addProgressAndCheck(Player player, Object event, String prefix, String expr) {
        ConditionNode node = treeCache.getOrParse(expr);
        return node.addProgress(new ConditionContext(player, event, prefix));
    }

    public MatchResultData checkAndGetCode(Player player, String prefix, String expr) {
        return checkAndGetCode(player, prefix, null, expr);
    }

    public MatchResultData checkAndGetCode(Player player, String prefix, Object event, String expr) {
        ConditionNode node = treeCache.getOrParse(expr);
        return node.match(new ConditionContext(player, event, prefix));
    }

    public void delete(Player player, String prefix, String expr) {
        ConditionNode node = treeCache.getOrParse(expr);
        node.delete(new ConditionContext(player, null, prefix));
    }

    public void reloadConfig() {
        treeCache.clear();
    }

    @Override
    public void initSampleCallbackCollector() {
        addChangeSampleFileObserveWithCallBack(ActivityConfigCfg.EXCEL_NAME, this::reloadConfig)
                .addChangeSampleFileObserveWithCallBack(DropConfigCfg.EXCEL_NAME, this::reloadConfig)
                .addChangeSampleFileObserveWithCallBack(GlobalConfigCfg.EXCEL_NAME, this::reloadConfig)
                .addChangeSampleFileObserveWithCallBack(GameFunctionCfg.EXCEL_NAME, this::reloadConfig)
                .addChangeSampleFileObserveWithCallBack(DailyRewardsCfg.EXCEL_NAME, this::reloadConfig)
                .addChangeSampleFileObserveWithCallBack(ConditionCfg.EXCEL_NAME, this::reloadConfig);

    }
}
