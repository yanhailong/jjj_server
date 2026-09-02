package com.jjg.game.sim.data;

import java.util.*;

/**
 * 玩家各场景通过建筑解锁的游戏
 *
 * @author 11
 * @date 2026/6/4
 */
public class SimCasinoUnlock {
    //场景id -> 已解锁游戏id集合
    private Map<Integer, Set<Integer>> unlockedGameMap;

    public Map<Integer, Set<Integer>> getUnlockedGameMap() {
        return unlockedGameMap;
    }

    public void setUnlockedGameMap(Map<Integer, Set<Integer>> unlockedGameMap) {
        this.unlockedGameMap = unlockedGameMap;
    }

    public boolean unlockCasino(int casinoId) {
        if (this.unlockedGameMap == null) {
            this.unlockedGameMap = new HashMap<>();
        }
        if (this.unlockedGameMap.containsKey(casinoId)) {
            return false;
        }
        this.unlockedGameMap.put(casinoId, new HashSet<>());
        return true;
    }

    public boolean unlockGame(int casinoId, int gameId) {
        if (gameId <= 0) {
            return false;
        }
        unlockCasino(casinoId);
        return this.unlockedGameMap.get(casinoId).add(gameId);
    }

    public Set<Integer> findUnlockedCasinoIds() {
        if (this.unlockedGameMap == null || this.unlockedGameMap.isEmpty()) {
            return Collections.emptySet();
        }
        return this.unlockedGameMap.keySet();
    }

    public Set<Integer> findUnlockedGameIds() {
        if (this.unlockedGameMap == null || this.unlockedGameMap.isEmpty()) {
            return Collections.emptySet();
        }
        Set<Integer> result = new HashSet<>();
        this.unlockedGameMap.values().forEach(result::addAll);
        return result;
    }

    public boolean gameUnlocked(int gameId) {
        if (this.unlockedGameMap == null) {
            return false;
        }
        for (Set<Integer> gameIds : this.unlockedGameMap.values()) {
            if (gameIds.contains(gameId)) {
                return true;
            }
        }
        return false;
    }

    public boolean gameUnlocked(int casinoId, int gameId) {
        if (this.unlockedGameMap == null) {
            return false;
        }
        Set<Integer> gameIds = this.unlockedGameMap.get(casinoId);
        return gameIds != null && gameIds.contains(gameId);
    }

    public boolean hasUnlockCasino(int casinoId) {
        if (this.unlockedGameMap == null) {
            this.unlockedGameMap = new HashMap<>();
        }
        return this.unlockedGameMap.containsKey(casinoId);
    }
}
