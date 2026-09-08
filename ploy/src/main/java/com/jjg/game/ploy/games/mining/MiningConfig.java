package com.jjg.game.ploy.games.mining;

import com.alibaba.fastjson.JSON;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Comparator;

/** miningConfig.json：内置默认值随ploy包发布，部署目录中的同名文件仍可覆盖和热更新。 */
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

    /** 避免部署目录漏放JSON时静默退回空任务；外部配置会在SampleDataManager启动时再次覆盖。 */
    @PostConstruct
    public void loadBundledDefaults() {
        try (InputStream input = MiningConfig.class.getResourceAsStream("/miningConfig.json")) {
            if (input == null) throw new IllegalStateException("Bundled miningConfig.json is missing");
            MiningConfig bundled = JSON.parseObject(new String(input.readAllBytes(), StandardCharsets.UTF_8), MiningConfig.class);
            if (bundled == null || bundled.dailyTasks == null || bundled.dailyTasks.isEmpty()) {
                throw new IllegalStateException("Bundled mining dailyTasks is empty");
            }
            enabled = bundled.enabled;
            permanentLimits = bundled.permanentLimits == null ? Map.of() : bundled.permanentLimits;
            dailyTasks = bundled.dailyTasks;
            seasons = bundled.seasons == null ? List.of() : bundled.seasons;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load bundled miningConfig.json", e);
        }
    }

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
