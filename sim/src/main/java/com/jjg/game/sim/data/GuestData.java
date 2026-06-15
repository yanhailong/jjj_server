package com.jjg.game.sim.data;

import com.jjg.game.sampledata.bean.VisitorLevelCfg;

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
    //碎片数量 (升星消耗)
    private int fragment;

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

    public int getFragment() {
        return fragment;
    }

    public void setFragment(int fragment) {
        this.fragment = fragment;
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
}
