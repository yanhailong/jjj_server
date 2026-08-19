package com.jjg.game.sim.data;

import java.util.HashMap;
import java.util.Map;

/**
 * slots 技能对结果库类型和倍数区间的权重加成。
 */
public class SlotsSkillEffectData {
    private Map<Integer, Integer> libTypeWeightDelta;
    private Map<Integer, Map<Integer, Integer>> sectionWeightDelta;

    public Map<Integer, Integer> getLibTypeWeightDelta() {
        if (libTypeWeightDelta == null) {
            libTypeWeightDelta = new HashMap<>();
        }
        return libTypeWeightDelta;
    }

    public void setLibTypeWeightDelta(Map<Integer, Integer> libTypeWeightDelta) {
        this.libTypeWeightDelta = libTypeWeightDelta == null
                ? new HashMap<>() : new HashMap<>(libTypeWeightDelta);
    }

    public Map<Integer, Map<Integer, Integer>> getSectionWeightDelta() {
        if (sectionWeightDelta == null) {
            sectionWeightDelta = new HashMap<>();
        }
        return sectionWeightDelta;
    }

    public void setSectionWeightDelta(Map<Integer, Map<Integer, Integer>> sectionWeightDelta) {
        this.sectionWeightDelta = new HashMap<>();
        if (sectionWeightDelta == null) {
            return;
        }
        sectionWeightDelta.forEach((libType, delta) -> this.sectionWeightDelta.put(
                libType, delta == null ? new HashMap<>() : new HashMap<>(delta)));
    }

    /**
     * 按 ResearchSkills、SeasonGem 和 VisitorBonds 共用的字段语义累加一组权重。
     */
    public void addBonus(Map<Integer, Integer> specialMode,
                         Map<Integer, Map<Integer, Integer>> winRate,
                         Map<Integer, Map<Integer, Integer>> specialModeProbUp) {
        mergeDelta(getLibTypeWeightDelta(), specialMode);
        mergeSectionDelta(getSectionWeightDelta(), winRate);
        mergeSectionDelta(getSectionWeightDelta(), specialModeProbUp);
    }

    private static void mergeDelta(Map<Integer, Integer> target, Map<Integer, Integer> source) {
        if (source == null || source.isEmpty()) {
            return;
        }
        source.forEach((key, delta) -> {
            if (key != null && delta != null) {
                target.merge(key, delta, Integer::sum);
            }
        });
    }

    private static void mergeSectionDelta(Map<Integer, Map<Integer, Integer>> target,
                                          Map<Integer, Map<Integer, Integer>> source) {
        if (source == null || source.isEmpty()) {
            return;
        }
        source.forEach((libType, sectionDelta) -> {
            if (libType == null || sectionDelta == null || sectionDelta.isEmpty()) {
                return;
            }
            mergeDelta(target.computeIfAbsent(libType, ignored -> new HashMap<>()), sectionDelta);
        });
    }
}
