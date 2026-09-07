package com.jjg.game.ploy.games.mining;

import java.util.*;

/** JSON存入PlayerPack，随背包迁移到Mongo；不保存无限历史地图。 */
public class MiningState {
    public String seasonId;
    public long version;
    public long seed;
    public int width;
    public int visibleRows;
    public int topRow = 1;
    public int depth;
    public long depthReachedAt;
    public int generatedRows;
    public int lastToolRow;
    public int connectivityVersion;
    public List<Cell> cells = new ArrayList<>();
    public Stats total = new Stats();
    public Stats daily = new Stats();
    public int day;
    public Map<Integer, Integer> dailyPurchases = new HashMap<>();
    public Map<Integer, Integer> permanentPurchases = new HashMap<>();
    public Set<Integer> claimedAchievements = new HashSet<>();
    public Set<Integer> claimedDailyTasks = new HashSet<>();
    public Map<Long, Secret> secrets = new HashMap<>();
    public Map<String, Long> paidOrders = new HashMap<>();
    public Map<String, Quote> paymentQuotes = new HashMap<>();
    /** 特殊道具的跨系统发奖日志。崩溃后的不确定结果禁止自动重发。 */
    public Delivery delivery;

    public static class Cell {
        public int row;
        public int column;
        public int type;
        public int hp;
        public long secretId;
        /** 已打开且能够沿打开格连到地表；用于滚屏后保留连通来源。 */
        public boolean reachable;

        public Cell() { }
        public Cell(int row, int column, int type, int hp) {
            this.row = row;
            this.column = column;
            this.type = type;
            this.hp = hp;
        }
    }

    public static class Stats {
        public long grids;
        public int depth;
        public long exchanges;
        public long ads;
        public Map<Integer, Long> tools = new HashMap<>();
        public Map<Integer, Long> resources = new HashMap<>();
        public Map<Integer, Long> exchangedItems = new HashMap<>();
    }

    public static class Secret {
        public long id;
        public int remaining;
        public boolean abandoned;
    }

    public static class Delivery {
        public String id;
        public Map<Integer, Long> rewards;
        public Map<Integer, Long> costs;
        public int goodId;
        public String previousState;
    }

    public static class Quote {
        public String id;
        public int goodId;
        public int day;
        public String price;
        public Map<Integer, Long> goods;
    }

    public void refreshDay(int today) {
        if (day == today) return;
        day = today;
        daily = new Stats();
        dailyPurchases.clear();
        claimedDailyTasks.clear();
    }
}
