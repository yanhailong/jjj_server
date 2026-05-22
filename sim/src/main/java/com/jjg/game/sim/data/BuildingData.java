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
}
