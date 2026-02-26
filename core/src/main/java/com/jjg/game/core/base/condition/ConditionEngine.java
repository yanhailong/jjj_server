package com.jjg.game.core.base.condition;

/**
 * @author lm
 * @date 2026/1/14 10:36
 */

import com.jjg.game.core.data.Player;
import com.jjg.game.core.listener.ConfigExcelChangeListener;
import com.jjg.game.sampledata.bean.*;
import org.springframework.stereotype.Component;

@Component
public class ConditionEngine implements ConfigExcelChangeListener {

    private final ConditionTreeCache treeCache;

    public ConditionEngine(ConditionParser parser) {
        this.treeCache = new ConditionTreeCache(parser);
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
                .addChangeSampleFileObserveWithCallBack(DailyRewardsCfg.EXCEL_NAME, this::reloadConfig);

    }
}
