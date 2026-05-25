package com.jjg.game.sim.data;

import com.jjg.game.sampledata.bean.VisitorLevelCfg;

import java.util.List;
import java.util.Map;

/**
 * 游客对象
 *
 * @author 11
 * @date 2026/5/20
 */
public class GuestData {
    //游客 id (对应 VisitorQuestCfg.id)
    private int id;
    //当前星级
    private int star;
    //当前等级
    private int level;
    //当前经验
    private int exp;
    //是否在赌场
    private boolean online;
    //本次行程的目的地序列 (顺序固定)
    private List<Destination> destinations;
    //当前所在建筑id (0 = 建筑外, 在路上或离场)
    private int currentBuildingId;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getStar() {
        return star;
    }

    public void setStar(int star) {
        this.star = star;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getExp() {
        return exp;
    }

    public void setExp(int exp) {
        this.exp = exp;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public List<Destination> getDestinations() {
        return destinations;
    }

    public void setDestinations(List<Destination> destinations) {
        this.destinations = destinations;
    }

    public int getCurrentBuildingId() {
        return currentBuildingId;
    }

    public void setCurrentBuildingId(int currentBuildingId) {
        this.currentBuildingId = currentBuildingId;
    }

    /**
     * 查找第一个匹配且未完成的目的地条目
     */
    public Destination findPendingDestination(int buildingId, int deviceId) {
        if (this.destinations == null) {
            return null;
        }
        for (Destination dest : this.destinations) {
            if (dest.isDone()) {
                continue;
            }
            if (dest.getBuildingId() == buildingId && dest.getDeviceId() == deviceId) {
                return dest;
            }
        }
        return null;
    }

    /**
     * 是否所有目的地都已交互完毕
     */
    public boolean isAllDestinationsDone() {
        if (this.destinations == null || this.destinations.isEmpty()) {
            return true;
        }
        for (Destination dest : this.destinations) {
            if (!dest.isDone()) {
                return false;
            }
        }
        return true;
    }

    /**
     * 经验 +1, 达标自动升级
     */
    public void addExp(Map<Integer, Map<Integer, VisitorLevelCfg>> levelMap) {
        this.exp += 1;
        tryLevelUp(levelMap);
    }

    /**
     * 根据 VisitorLevel 表尝试升级 (循环, 满足条件就连升)
     */
    private void tryLevelUp(Map<Integer, Map<Integer, VisitorLevelCfg>> levelMap) {
        if (levelMap == null || levelMap.isEmpty()) {
            return;
        }

        Map<Integer, VisitorLevelCfg> guestLevelMap = levelMap.get(this.id);
        if (guestLevelMap == null || guestLevelMap.isEmpty()) {
            return;
        }

        while (true) {
            int nextLevel = this.level + 1;
            VisitorLevelCfg nextCfg = guestLevelMap.get(nextLevel);
            if (nextCfg == null) {
                return;
            }
            if (this.exp < nextCfg.getLevelUpExp()) {
                return;
            }
            this.level += 1;
        }
    }

    /**
     * 下线
     */
    public void offLine() {
        this.online = false;
        this.currentBuildingId = 0;
        this.destinations = null;
    }
}
