package com.jjg.game.sim.data;

import java.util.HashSet;
import java.util.Set;

/**
 * 建筑数据
 *
 * @author 11
 * @date 2026/5/22
 */
public class BuildingData {
    //建筑id
    private int id;
    //当前在里面逛的游客 (接待区)
    private Set<Integer> guestId;
    //排队区的游客
    private Set<Integer> waitGuestId;
    //预占位置的游客 (规划目的地时占位, 到达时释放)
    private Set<Integer> reserveGuestId;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Set<Integer> getGuestId() {
        return guestId;
    }

    public void setGuestId(Set<Integer> guestId) {
        this.guestId = guestId;
    }

    public Set<Integer> getWaitGuestId() {
        return waitGuestId;
    }

    public void setWaitGuestId(Set<Integer> waitGuestId) {
        this.waitGuestId = waitGuestId;
    }

    public Set<Integer> getReserveGuestId() {
        return reserveGuestId;
    }

    public void setReserveGuestId(Set<Integer> reserveGuestId) {
        this.reserveGuestId = reserveGuestId;
    }

    /**
     * 当前总占用数 = 接待 + 排队 + 预占
     */
    public int occupancy() {
        int n = 0;
        if (guestId != null) {
            n += guestId.size();
        }
        if (waitGuestId != null) {
            n += waitGuestId.size();
        }
        if (reserveGuestId != null) {
            n += reserveGuestId.size();
        }
        return n;
    }

    /**
     * 预占位
     *
     * @param gid
     */
    public void addReserve(int gid) {
        if (reserveGuestId == null) {
            reserveGuestId = new HashSet<>();
        }
        reserveGuestId.add(gid);
    }

    public void removeReserve(int gid) {
        if (reserveGuestId != null) {
            reserveGuestId.remove(gid);
        }
    }

    public void addSeating(int gid) {
        if (guestId == null) {
            guestId = new HashSet<>();
        }
        guestId.add(gid);
    }

    public void removeSeating(int gid) {
        if (guestId != null) {
            guestId.remove(gid);
        }
    }

    public void addWait(int gid) {
        if (waitGuestId == null) {
            waitGuestId = new HashSet<>();
        }
        waitGuestId.add(gid);
    }

    public void removeWait(int gid) {
        if (waitGuestId != null) {
            waitGuestId.remove(gid);
        }
    }

    public int seatingSize() {
        return guestId == null ? 0 : guestId.size();
    }

    public int waitSize() {
        return waitGuestId == null ? 0 : waitGuestId.size();
    }

    // ---------------------------------------------------------------------
    // 状态机操作: 把多步组合内聚到聚合根, Service 只调一次即可
    // ---------------------------------------------------------------------

    /**
     * 游客是否在本建筑 (接待 / 排队 / 预占任一集合)
     */
    public boolean contains(int gid) {
        if (guestId != null && guestId.contains(gid)) return true;
        if (waitGuestId != null && waitGuestId.contains(gid)) return true;
        if (reserveGuestId != null && reserveGuestId.contains(gid)) return true;
        return false;
    }

    /**
     * 游客到达建筑:
     * - 先从 reserve 释放
     * - 再按 seating / wait 容量决定落位
     * - 若 seating 与 wait 都已满, 兜底加 seating 并返回 false (调用方据此告警)
     *
     * @param gid        游客 id
     * @param seatingCap 接待上限
     * @param queueCap   排队上限
     * @return true=正常落位; false=都已满, 兜底入座
     */
    public boolean arriveSeating(int gid, int seatingCap, int queueCap) {
        removeReserve(gid);
        if (seatingSize() < seatingCap) {
            addSeating(gid);
            return true;
        }
        if (waitSize() < queueCap) {
            addWait(gid);
            return true;
        }
        //兜底
        addSeating(gid);
        return false;
    }

    /**
     * 游客离开建筑: 同时从 seating / wait 中移除
     */
    public void leaveBuilding(int gid) {
        removeSeating(gid);
        removeWait(gid);
    }

    /**
     * 清除该游客在本建筑所有集合 (接待 / 排队 / 预占) 的痕迹
     */
    public void clearTraces(int gid) {
        removeSeating(gid);
        removeWait(gid);
        removeReserve(gid);
    }
}
