package com.jjg.game.sim.data;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author 11
 * @date 2026/5/22
 */
@Document
public class SimSkillsData {
    //playerId:gameType 联合主键
    @Id
    private String id;
    //玩家id
    private long playerId;
    //游戏类型
    private int gameType;
    //技能 propId -> level
    private Map<Integer, Integer> skillsMap;
    //通过技能解锁的下注额
    private List<Integer> stakeList;

    public long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(long playerId) {
        this.playerId = playerId;
    }

    public int getGameType() {
        return gameType;
    }

    public void setGameType(int gameType) {
        this.gameType = gameType;
    }

    public Map<Integer, Integer> getSkillsMap() {
        return skillsMap;
    }

    public void setSkillsMap(Map<Integer, Integer> skillsMap) {
        this.skillsMap = skillsMap;
    }

    public List<Integer> getStakeList() {
        return stakeList;
    }

    public void setStakeList(List<Integer> stakeList) {
        this.stakeList = stakeList;
    }

    public void addNewSkill(int propId) {
        if (this.skillsMap == null) {
            this.skillsMap = new HashMap<>();
        }

        if (!this.skillsMap.containsKey(propId)) {
            this.skillsMap.put(propId, 0);
        }
    }

    /**
     * 获取该技能等级
     *
     * @param propId
     * @return  null表示未解锁
     */
    public Integer findSkilLevelByPropId(int propId) {
        if (this.skillsMap == null) {
            return null;
        }
        return this.skillsMap.get(propId);
    }

    public void changeSkillLevel(int propId, int skillLevel) {
        if (this.skillsMap == null) {
            this.skillsMap = new HashMap<>();
        }
        this.skillsMap.put(propId, skillLevel);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    /**
     * 根据 playerId 和 gameType 构建联合主键
     */
    public void buildKey() {
        this.id = buildKey(this.playerId, this.gameType);
    }

    public static String buildKey(long playerId, int gameType) {
        return playerId + ":" + gameType;
    }
}
