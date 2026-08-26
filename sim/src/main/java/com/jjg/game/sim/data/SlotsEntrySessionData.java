package com.jjg.game.sim.data;

import com.jjg.game.season.data.SeasonSlotsSessionData;

import java.util.HashMap;
import java.util.Map;

/**
 * sim 在 slots 进场时生成的技能快照，各功能数据保持独立。
 */
public class SlotsEntrySessionData {
    /** 普通入口为玩家本人、拜访入口为房主的研发技能 propId -> level。 */
    private Map<Integer, Integer> researchSkills;
    /** 技能归属玩家指定场景中匹配游戏的游客羁绊效果。 */
    private SlotsSkillEffectData visitorBondsEffect;
    /** 玩家自己的好友同玩全局技能效果。 */
    private TogetherPlaySkillEffectData togetherPlaySkillEffect;
    /** 赛季入口的赛季运行态及宝石效果。 */
    private SeasonSlotsSessionData seasonData;

    public Map<Integer, Integer> getResearchSkills() {
        return researchSkills;
    }

    public void setResearchSkills(Map<Integer, Integer> researchSkills) {
        this.researchSkills = researchSkills == null ? null : new HashMap<>(researchSkills);
    }

    public SlotsSkillEffectData getVisitorBondsEffect() {
        if (visitorBondsEffect == null) {
            visitorBondsEffect = new SlotsSkillEffectData();
        }
        return visitorBondsEffect;
    }

    public void setVisitorBondsEffect(SlotsSkillEffectData visitorBondsEffect) {
        this.visitorBondsEffect = visitorBondsEffect == null
                ? new SlotsSkillEffectData() : visitorBondsEffect;
    }

    public TogetherPlaySkillEffectData getTogetherPlaySkillEffect() {
        if (togetherPlaySkillEffect == null) {
            togetherPlaySkillEffect = new TogetherPlaySkillEffectData();
        }
        return togetherPlaySkillEffect;
    }

    public void setTogetherPlaySkillEffect(TogetherPlaySkillEffectData togetherPlaySkillEffect) {
        this.togetherPlaySkillEffect = togetherPlaySkillEffect == null
                ? new TogetherPlaySkillEffectData() : togetherPlaySkillEffect;
    }

    public SeasonSlotsSessionData getSeasonData() {
        return seasonData;
    }

    public void setSeasonData(SeasonSlotsSessionData seasonData) {
        this.seasonData = seasonData;
    }
}
