package com.jjg.game.ploy.games.mining;

import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.MiningRankingCfg;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** MiningRanking.xlsx 的类型安全访问入口。 */
final class MiningRankingCatalog {
    private static final int ITEM_AWARD = 2;
    private static final List<DateTimeFormatter> DATE_TIMES = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"));
    private static final List<DateTimeFormatter> DATES = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
            DateTimeFormatter.BASIC_ISO_DATE);

    record RewardBand(int type, int from, int to, Map<Integer, Long> items) { }

    private MiningRankingCatalog() { }

    static List<RewardBand> rewards(long referenceTime) {
        List<MiningRankingCfg> configs = GameDataManager.getMiningRankingCfgList();
        if (configs == null || configs.isEmpty()) throw new IllegalStateException("MiningRanking is empty");
        int type = activeType(configs, referenceTime);
        List<RewardBand> result = configs.stream()
                .filter(cfg -> cfg.getType() == type)
                .map(MiningRankingCatalog::band)
                .sorted(Comparator.comparingInt(RewardBand::from))
                .toList();
        if (result.isEmpty()) throw new IllegalStateException("MiningRanking type is empty: " + type);
        int previousEnd = 0;
        for (RewardBand band : result) {
            if (band.from() <= previousEnd) throw new IllegalStateException("MiningRanking ranges overlap: " + type);
            previousEnd = band.to();
        }
        return result;
    }

    static Map<Integer, Long> reward(long referenceTime, int rank) {
        if (rank <= 0) return Map.of();
        return rewards(referenceTime).stream()
                .filter(band -> rank >= band.from() && rank <= band.to())
                .findFirst().map(RewardBand::items).orElse(Map.of());
    }

    private static RewardBand band(MiningRankingCfg cfg) {
        if (cfg.getAwardType() != ITEM_AWARD) {
            throw new IllegalStateException("Unsupported MiningRanking awardType: " + cfg.getAwardType());
        }
        List<Integer> ranking = cfg.getRanking();
        if (ranking == null || ranking.isEmpty() || ranking.size() > 2) {
            throw new IllegalStateException("Invalid MiningRanking ranking: " + cfg.getId());
        }
        int from = ranking.getFirst();
        int to = ranking.size() == 1 ? from : ranking.getLast();
        Map<Integer, Long> items = cfg.getGetItem();
        if (from <= 0 || to < from || items == null || items.isEmpty()
                || items.entrySet().stream().anyMatch(entry -> entry.getKey() <= 0 || entry.getValue() <= 0)) {
            throw new IllegalStateException("Invalid MiningRanking reward: " + cfg.getId());
        }
        return new RewardBand(cfg.getType(), from, to, Map.copyOf(items));
    }

    private static int activeType(List<MiningRankingCfg> configs, long referenceTime) {
        int fallback = configs.stream().mapToInt(MiningRankingCfg::getType).filter(type -> type > 0).min()
                .orElseThrow(() -> new IllegalStateException("MiningRanking type is invalid"));
        Map<Integer, Long> starts = new HashMap<>();
        for (MiningRankingCfg cfg : configs) {
            String time = cfg.getTime();
            if (time == null || time.isBlank()) continue;
            long start = parseTime(time.trim());
            Long previous = starts.putIfAbsent(cfg.getType(), start);
            if (previous != null && previous != start) {
                throw new IllegalStateException("MiningRanking type has conflicting start times: " + cfg.getType());
            }
        }
        int selected = fallback;
        long selectedStart = Long.MIN_VALUE;
        for (Map.Entry<Integer, Long> entry : starts.entrySet()) {
            if (entry.getValue() <= referenceTime && entry.getValue() > selectedStart) {
                selected = entry.getKey();
                selectedStart = entry.getValue();
            }
        }
        return selected;
    }

    private static long parseTime(String value) {
        if (value.chars().allMatch(Character::isDigit) && value.length() >= 11) {
            try { return Long.parseLong(value); }
            catch (NumberFormatException ignored) { }
        }
        ZoneId zone = ZoneId.systemDefault();
        for (DateTimeFormatter formatter : DATE_TIMES) {
            try { return LocalDateTime.parse(value, formatter).atZone(zone).toInstant().toEpochMilli(); }
            catch (DateTimeParseException ignored) { }
        }
        for (DateTimeFormatter formatter : DATES) {
            try { return LocalDate.parse(value, formatter).atStartOfDay(zone).toInstant().toEpochMilli(); }
            catch (DateTimeParseException ignored) { }
        }
        throw new IllegalStateException("Invalid MiningRanking time: " + value);
    }
}
