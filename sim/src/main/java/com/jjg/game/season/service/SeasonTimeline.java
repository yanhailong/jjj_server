package com.jjg.game.season.service;

import com.jjg.game.season.config.SeasonDefinition;
import com.jjg.game.season.model.SeasonPhase;
import com.jjg.game.season.model.SeasonSnapshot;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 根据玩家注册时间和赛季配置计算个人赛季时间线。
 */
public class SeasonTimeline {
    private static final long DAY_MILLIS = 24L * 60 * 60 * 1000;

    public SeasonSnapshot resolve(long registeredAt, long now, List<SeasonDefinition> definitions) {
        TimelineConfig config = validate(definitions);
        long elapsed = Math.max(0, now - registeredAt);
        long cursor = registeredAt;

        SeasonDefinition novice = config.introductory().get(0);
        long noviceDuration = durationMillis(novice);
        if (elapsed < noviceDuration) {
            return snapshot(novice, SeasonPhase.NOVICE, 0, cursor, now);
        }
        elapsed -= noviceDuration;
        cursor += noviceDuration;

        SeasonDefinition advanced = config.introductory().get(1);
        long advancedDuration = durationMillis(advanced);
        if (elapsed < advancedDuration) {
            return snapshot(advanced, SeasonPhase.ADVANCED, 0, cursor, now);
        }
        elapsed -= advancedDuration;
        cursor += advancedDuration;

        long loopDuration = config.loop().stream().mapToLong(this::durationMillis).sum();
        long completedRounds = elapsed / loopDuration;
        long inRound = elapsed % loopDuration;
        int cycleIndex = Math.toIntExact(completedRounds * config.loop().size());
        for (SeasonDefinition definition : config.loop()) {
            long duration = durationMillis(definition);
            cycleIndex++;
            if (inRound < duration) {
                long start = cursor + completedRounds * loopDuration;
                for (SeasonDefinition previous : config.loop()) {
                    if (previous == definition) {
                        break;
                    }
                    start += durationMillis(previous);
                }
                return snapshot(definition, SeasonPhase.LOOP, cycleIndex, start, now);
            }
            inRound -= duration;
        }
        throw new IllegalStateException("无法解析循环赛季");
    }

    private SeasonSnapshot snapshot(SeasonDefinition definition, SeasonPhase phase,
                                    int cycleIndex, long start, long now) {
        long end = start + durationMillis(definition);
        int day = (int) ((Math.max(start, now) - start) / DAY_MILLIS) + 1;
        String key = definition.id() + ":" + cycleIndex;
        return new SeasonSnapshot(definition.id(), phase, cycleIndex, start, end, day, key);
    }

    private TimelineConfig validate(List<SeasonDefinition> definitions) {
        if (definitions == null) {
            throw new IllegalArgumentException("赛季配置不能为空");
        }
        List<SeasonDefinition> introductory = new ArrayList<>();
        List<SeasonDefinition> loop = new ArrayList<>();
        for (SeasonDefinition definition : definitions) {
            if (definition == null || definition.durationDays() <= 0) {
                throw new IllegalArgumentException("赛季持续时间必须大于 0");
            }
            if (definition.loopSequence() > 0) {
                loop.add(definition);
            } else {
                introductory.add(definition);
            }
        }
        if (introductory.size() < 2 || loop.isEmpty()) {
            throw new IllegalArgumentException("赛季配置必须包含两个前置赛季和至少一个循环赛季");
        }
        loop.sort(Comparator.comparingInt(SeasonDefinition::loopSequence));
        for (int i = 1; i < loop.size(); i++) {
            if (loop.get(i - 1).loopSequence() == loop.get(i).loopSequence()) {
                throw new IllegalArgumentException("循环赛季顺序不能重复");
            }
        }
        return new TimelineConfig(introductory, loop);
    }

    private long durationMillis(SeasonDefinition definition) {
        return Math.multiplyExact(definition.durationDays(), DAY_MILLIS);
    }

    private record TimelineConfig(List<SeasonDefinition> introductory, List<SeasonDefinition> loop) {
    }
}
