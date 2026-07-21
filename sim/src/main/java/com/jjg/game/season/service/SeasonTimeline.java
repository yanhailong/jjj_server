package com.jjg.game.season.service;

import com.jjg.game.season.config.SeasonDefinition;
import com.jjg.game.season.model.SeasonPhase;
import com.jjg.game.season.model.SeasonSnapshot;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 根据玩家注册时间和赛季配置计算个人赛季时间线。
 */
public class SeasonTimeline {
    public SeasonSnapshot resolve(long registeredAt, long now, List<SeasonDefinition> definitions) {
        TimelineConfig config = validate(definitions);
        ZoneId zone = ZoneId.systemDefault();
        LocalDate cursor = dateOf(registeredAt, zone);
        LocalDate currentDate = dateOf(Math.max(registeredAt, now), zone);
        long elapsed = ChronoUnit.DAYS.between(cursor, currentDate);

        SeasonDefinition novice = config.introductory().get(0);
        long noviceDuration = novice.durationDays();
        if (elapsed < noviceDuration) {
            return snapshot(novice, SeasonPhase.NOVICE, 0, cursor, currentDate, zone);
        }
        elapsed -= noviceDuration;
        cursor = cursor.plusDays(noviceDuration);

        SeasonDefinition advanced = config.introductory().get(1);
        long advancedDuration = advanced.durationDays();
        if (elapsed < advancedDuration) {
            return snapshot(advanced, SeasonPhase.ADVANCED, 0, cursor, currentDate, zone);
        }
        elapsed -= advancedDuration;
        cursor = cursor.plusDays(advancedDuration);

        long loopDuration = config.loop().stream().mapToLong(SeasonDefinition::durationDays).sum();
        long completedRounds = elapsed / loopDuration;
        long inRound = elapsed % loopDuration;
        int cycleIndex = Math.toIntExact(completedRounds * config.loop().size());
        LocalDate roundStart = cursor.plusDays(Math.multiplyExact(completedRounds, loopDuration));
        for (SeasonDefinition definition : config.loop()) {
            long duration = definition.durationDays();
            cycleIndex++;
            if (inRound < duration) {
                LocalDate start = roundStart;
                for (SeasonDefinition previous : config.loop()) {
                    if (previous == definition) {
                        break;
                    }
                    start = start.plusDays(previous.durationDays());
                }
                return snapshot(definition, SeasonPhase.LOOP, cycleIndex, start, currentDate, zone);
            }
            inRound -= duration;
        }
        throw new IllegalStateException("无法解析循环赛季");
    }

    private SeasonSnapshot snapshot(SeasonDefinition definition, SeasonPhase phase,
                                    int cycleIndex, LocalDate startDate,
                                    LocalDate currentDate, ZoneId zone) {
        long start = startDate.atStartOfDay(zone).toInstant().toEpochMilli();
        long end = startDate.plusDays(definition.durationDays())
                .atStartOfDay(zone).toInstant().toEpochMilli();
        int day = Math.toIntExact(ChronoUnit.DAYS.between(startDate, currentDate)) + 1;
        String key = definition.id() + ":" + cycleIndex;
        return new SeasonSnapshot(definition.id(), phase, cycleIndex, start, end, day, key);
    }

    static int currentDay(long start, long now) {
        ZoneId zone = ZoneId.systemDefault();
        LocalDate startDate = dateOf(start, zone);
        LocalDate currentDate = dateOf(Math.max(start, now), zone);
        return Math.toIntExact(ChronoUnit.DAYS.between(startDate, currentDate)) + 1;
    }

    private static LocalDate dateOf(long time, ZoneId zone) {
        return Instant.ofEpochMilli(time).atZone(zone).toLocalDate();
    }

    private TimelineConfig validate(List<SeasonDefinition> definitions) {
        if (definitions == null) {
            throw new IllegalArgumentException("赛季配置不能为空");
        }
        List<SeasonDefinition> introductory = new ArrayList<>();
        List<SeasonDefinition> loop = new ArrayList<>();
        for (SeasonDefinition definition : definitions) {
            if (definition == null || definition.durationDays() < 0) {
                throw new IllegalArgumentException("赛季持续时间不能为负");
            }
            // SeasonDuration=0 表示跳过该赛季:
            // 前置赛季保留占位, resolve 按 0 天穿过, 从而进入下一个前置/循环赛季且保持相位次序;
            // 循环赛季直接剔除, 使循环序号连续并避免循环总时长为 0。
            if (definition.loopSequence() > 0) {
                if (definition.durationDays() > 0) {
                    loop.add(definition);
                }
            } else {
                introductory.add(definition);
            }
        }
        if (introductory.size() < 2 || loop.isEmpty()) {
            throw new IllegalArgumentException("赛季配置必须包含两个前置赛季和至少一个有效循环赛季");
        }
        loop.sort(Comparator.comparingInt(SeasonDefinition::loopSequence));
        for (int i = 1; i < loop.size(); i++) {
            if (loop.get(i - 1).loopSequence() == loop.get(i).loopSequence()) {
                throw new IllegalArgumentException("循环赛季顺序不能重复");
            }
        }
        return new TimelineConfig(introductory, loop);
    }

    private record TimelineConfig(List<SeasonDefinition> introductory, List<SeasonDefinition> loop) {
    }
}
