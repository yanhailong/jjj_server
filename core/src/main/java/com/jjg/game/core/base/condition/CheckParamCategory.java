package com.jjg.game.core.base.condition;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 检查参数分类
 *
 * @author 2CL
 */
public class CheckParamCategory {

    // 有效流水参数
    public static class EffectiveFlowingParam extends CheckerParam {
        // 条件进度key
        private String conditionProgressKey;
        // 流水值
        private long flowingValue;
        // 游戏配置ID
        private long gameCfgId;
        // 需要更新进度
        private boolean needUpdateProgress;
        // 触发次数
        private int triggerTimes;

        public EffectiveFlowingParam(Set<String> checkName, Object targetParam) {
            super(checkName, targetParam);
        }

        public EffectiveFlowingParam(List<String> checkName, Object targetParam) {
            super(new HashSet<>(checkName), targetParam);
        }

        public String getConditionProgressKey() {
            return conditionProgressKey;
        }

        public void setConditionProgressKey(String conditionProgressKey) {
            this.conditionProgressKey = conditionProgressKey;
        }

        public long getFlowingValue() {
            return flowingValue;
        }

        public void setFlowingValue(long flowingValue) {
            this.flowingValue = flowingValue;
        }

        public long getGameCfgId() {
            return gameCfgId;
        }

        public void setGameCfgId(long gameCfgId) {
            this.gameCfgId = gameCfgId;
        }

        public boolean getNeedUpdateProgress() {
            return needUpdateProgress;
        }

        public void setNeedUpdateProgress(boolean needUpdateProgress) {
            this.needUpdateProgress = needUpdateProgress;
        }

        public int getTriggerTimes() {
            return triggerTimes;
        }

        public void setTriggerTimes(int triggerTimes) {
            this.triggerTimes = triggerTimes;
        }
    }
}
