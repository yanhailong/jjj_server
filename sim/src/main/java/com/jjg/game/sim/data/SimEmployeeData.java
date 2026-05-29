package com.jjg.game.sim.data;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * 雇员数据
 *
 * @author 11
 * @date 2026/5/28
 */
@Document
public class SimEmployeeData extends AbstractData {
    //联合主键 playerId:employeeId
    @Id
    private String id;
    @Indexed
    private long playerId;
    //雇员id (对应 EmployeeProfileCfg.id)
    private int employeeId;
    //当前等级 (>=1 表示已招募)
    private int level;
    //当前星级
    private int star;
    //碎片数量 (升星消耗)
    private int fragment;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(long playerId) {
        this.playerId = playerId;
    }

    public int getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(int employeeId) {
        this.employeeId = employeeId;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getStar() {
        return star;
    }

    public void setStar(int star) {
        this.star = star;
    }

    public int getFragment() {
        return fragment;
    }

    public void setFragment(int fragment) {
        this.fragment = fragment;
    }

    public void addFragment(int delta) {
        this.fragment += delta;
        if (this.fragment < 0) {
            this.fragment = 0;
        }
    }

    /**
     * 根据 playerId 和 employeeId 构建联合主键
     */
    public void buildKey() {
        this.id = buildKey(this.playerId, this.employeeId);
    }

    public static String buildKey(long playerId, int employeeId) {
        return playerId + ":" + employeeId;
    }
}
