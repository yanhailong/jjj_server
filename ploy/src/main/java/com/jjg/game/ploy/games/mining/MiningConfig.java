package com.jjg.game.ploy.games.mining;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Comparator;

/** miningConfig.json：仅保留不属于配置表的运行与赛季参数。玩法数据由 Mining*.xlsx 提供。 */
@Component
public class MiningConfig {
    public boolean enabled = true;
    public int adTicketSeconds = 600;
    public Map<Integer, Integer> permanentLimits = Map.of();
    public List<DailyTask> dailyTasks = List.of();
    public List<Season> seasons = List.of();

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean value) { enabled = value; }
    public int getAdTicketSeconds() { return adTicketSeconds; }
    public void setAdTicketSeconds(int value) { adTicketSeconds = value; }
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
        public List<RankReward> rewards = List.of();
    }

    public static class RankReward {
        public int from;
        public int to;
        public Map<Integer, Long> items = Map.of();
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
            int previousRank = 0;
            for (RankReward reward : season.rewards.stream().sorted(Comparator.comparingInt(r -> r.from)).toList()) {
                if (reward.from <= previousRank || reward.to < reward.from || reward.to > 300 || reward.items == null
                        || reward.items.entrySet().stream().anyMatch(e -> e.getKey() <= 0 || e.getValue() <= 0))
                    throw new IllegalStateException("Invalid mining rank reward");
                previousRank = reward.to;
            }
            if (season.startTime <= now && now < season.endTime) {
                if (found != null) throw new IllegalStateException("Overlapping mining seasons");
                found = season;
            }
        }
        return found;
    }
}
