package com.jjg.game.sim.data;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 玩家多人协作任务数据 (每日任务池 + 已领取任务)。
 * <p>
 * 每日 0 点数据按 dayKey 懒重置 (玩家访问时对比重置, 无全服定时扫库):
 * 重抽今日列表/清领取次数/清免费刷新; 已领取未完结的任务跨天保留直至领奖或失败。
 *
 * @author 11
 * @date 2026/7/6
 */
@Document
public class SimCoopTaskData extends AbstractData {
    @Id
    private long playerId;

    //数据所属日期 (yyyyMMdd), 与当日不符则懒重置
    private int dayKey;

    //今日任务列表 (从任务池抽取的任务配置id, 有序)
    private List<Integer> poolTaskIds = new ArrayList<>();

    //今日免费刷新是否已用
    private boolean freeRefreshUsed;

    //今日已领取任务次数
    private int claimedCount;

    //已领取任务 taskId -> entry (领奖/失败清理后移除)
    private Map<Integer, SimCoopTaskEntry> tasks = new HashMap<>();

    public long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(long playerId) {
        this.playerId = playerId;
    }

    public int getDayKey() {
        return dayKey;
    }

    public void setDayKey(int dayKey) {
        this.dayKey = dayKey;
    }

    public List<Integer> getPoolTaskIds() {
        if (poolTaskIds == null) {
            poolTaskIds = new ArrayList<>();
        }
        return poolTaskIds;
    }

    public void setPoolTaskIds(List<Integer> poolTaskIds) {
        this.poolTaskIds = poolTaskIds == null ? new ArrayList<>() : poolTaskIds;
    }

    public boolean isFreeRefreshUsed() {
        return freeRefreshUsed;
    }

    public void setFreeRefreshUsed(boolean freeRefreshUsed) {
        this.freeRefreshUsed = freeRefreshUsed;
    }

    public int getClaimedCount() {
        return claimedCount;
    }

    public void setClaimedCount(int claimedCount) {
        this.claimedCount = claimedCount;
    }

    public Map<Integer, SimCoopTaskEntry> getTasks() {
        if (tasks == null) {
            tasks = new HashMap<>();
        }
        return tasks;
    }

    public void setTasks(Map<Integer, SimCoopTaskEntry> tasks) {
        this.tasks = tasks == null ? new HashMap<>() : tasks;
    }
}
