package com.jjg.game.ploy.games.mining;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Comparator;

/** 正式赛季配置表尚未提供时，挖矿使用常驻 practice 赛季。 */
@Component
public class MiningConfig {
    public boolean enabled = true;
    public Map<Integer, Integer> permanentLimits = Map.of();
    public List<DailyTask> dailyTasks = List.of();
    public List<Season> seasons = List.of();

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean value) { enabled = value; }
    public Map<Integer, Integer> getPermanentLimits() { return permanentLimits; }
    public void setPermanentLimits(Map<Integer, Integer> value) { permanentLimits = value; }
    public List<DailyTask> getDailyTasks() { return dailyTasks; }
    public void setDailyTasks(List<DailyTask> value) { dailyTasks = value; }
    public List<Season> getSeasons() { return seasons; }
    public void setSeasons(List<Season> value) { seasons = value; }

    public static class DailyTask {
        public int id;
        /** 1挖开格子 2当日到达深度 3使用道具 4收集矿石 5观看广告 */
        public int kind;
        public int itemId;
        public long target;
        public Map<Integer, Long> rewards = Map.of();
        public int nameLanguageId;
        public int descLanguageId;
    }

    public static class Season {
        public String id;
        /** Unix毫秒，左闭右开。赛季不可重叠。 */
        public long startTime;
        public long endTime;
    }

    public Season currentSeason(long now) {
        if (seasons.isEmpty()) {
            Season practice = new Season();
            practice.id = "practice";
            return practice;
        }
        Season found = null;
        HashSet<String> ids = new HashSet<>();
        long previousEnd = Long.MIN_VALUE;
        for (Season season : seasons.stream().sorted(Comparator.comparingLong(s -> s.startTime)).toList()) {
            if (season.id == null || !season.id.matches("[a-zA-Z0-9_-]{1,64}")
                    || "practice".equals(season.id) || !ids.add(season.id)
                    || season.startTime >= season.endTime || season.startTime < previousEnd) {
                throw new IllegalStateException("Invalid mining season");
            }
            previousEnd = season.endTime;
            if (season.startTime <= now && now < season.endTime) {
                if (found != null) throw new IllegalStateException("Overlapping mining seasons");
                found = season;
            }
        }
        return found;
    }
}
