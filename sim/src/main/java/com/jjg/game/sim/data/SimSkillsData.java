package com.jjg.game.sim.data;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.*;

/**
 * @author 11
 * @date 2026/5/22
 */
@Document
public class SimSkillsData extends AbstractData {
    //playerId:gameType 联合主键
    @Id
    private String id;
    //玩家id (登录全量加载按此查询)
    @Indexed
    private long playerId;
    //游戏类型
    private int gameType;
    //技能 propId -> detail
    private Map<Integer, SkillDetailData> skillsMap;

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

    public Map<Integer, SkillDetailData> getSkillsMap() {
        return skillsMap;
    }

    public void setSkillsMap(Map<Integer, SkillDetailData> skillsMap) {
        this.skillsMap = skillsMap;
    }

    /**
     * 获取该技能等级
     *
     * @param propId
     * @return null表示未解锁
     */
    public SkillDetailData findSkilLevelByPropId(int propId) {
        if (this.skillsMap == null) {
            return null;
        }
        return this.skillsMap.get(propId);
    }

    public void changeSkillLevel(int propId, int skillLevel) {
        if (this.skillsMap == null) {
            this.skillsMap = new HashMap<>();
        }

        SkillDetailData skillDetailData = this.skillsMap.get(propId);
        if (skillDetailData == null) {
            skillDetailData = new SkillDetailData();
            skillDetailData.setPropId(propId);
        }
        skillDetailData.setLevel(skillLevel);
        this.skillsMap.put(propId, skillDetailData);
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

    public int allLevel() {
        int alllevel = 0;
        for (Map.Entry<Integer, SkillDetailData> en : this.skillsMap.entrySet()) {
            alllevel += en.getValue().getLevel();
        }
        return alllevel;
    }
}
