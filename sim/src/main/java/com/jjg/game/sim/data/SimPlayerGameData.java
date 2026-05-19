package com.jjg.game.sim.data;

import com.jjg.game.core.data.PlayerController;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Map;
import java.util.Set;

/**
 * @author 11
 * @date 2026/5/15
 */
@Document
public class SimPlayerGameData {
    @Id
    private long id;
    @Transient
    private transient PlayerController playerController;
    //研究院等级
    private int researchId;
    //体力值
    private int stamina;
    //能量
    private int power;
    //研究点 研究点类型：1.普通 2.珍惜  -> 数量
    private Map<Integer, Integer> researchPointMap;
    //技能 gameType -> skills
    private Map<Integer, Set<Integer>> skillsMap;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public PlayerController getPlayerController() {
        return playerController;
    }

    public void setPlayerController(PlayerController playerController) {
        this.playerController = playerController;
    }

    public int getResearchId() {
        return researchId;
    }

    public void setResearchId(int researchId) {
        this.researchId = researchId;
    }

    public int getStamina() {
        return stamina;
    }

    public void setStamina(int stamina) {
        this.stamina = stamina;
    }

    public int getPower() {
        return power;
    }

    public void setPower(int power) {
        this.power = power;
    }

    public Map<Integer, Integer> getResearchPointMap() {
        return researchPointMap;
    }

    public void setResearchPointMap(Map<Integer, Integer> researchPointMap) {
        this.researchPointMap = researchPointMap;
    }

    public Map<Integer, Set<Integer>> getSkillsMap() {
        return skillsMap;
    }

    public void setSkillsMap(Map<Integer, Set<Integer>> skillsMap) {
        this.skillsMap = skillsMap;
    }

    /**
     * 查询研究点
     *
     * @param skillId
     * @return
     */
    public int findResearchPoint(int skillId) {
        if (this.researchPointMap == null || this.researchPointMap.isEmpty()) {
            return 0;
        }
        return this.researchPointMap.get(skillId);
    }

    /**
     * 扣除研究点
     *
     * @param skillId
     * @return
     */
    public boolean deductResearchPoint(int skillId, int points) {
        if (this.researchPointMap == null || this.researchPointMap.isEmpty()) {
            return false;
        }

        Integer beforePoints = this.researchPointMap.get(skillId);
        if (beforePoints == null || beforePoints < points) {
            return false;
        }

        int afterPoints = beforePoints - points;
        if (afterPoints < 1) {
            this.researchPointMap.remove(skillId);
        } else {
            this.researchPointMap.put(skillId, afterPoints);
        }

        return true;
    }
}
