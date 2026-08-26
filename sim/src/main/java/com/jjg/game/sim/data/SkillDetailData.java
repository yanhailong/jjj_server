package com.jjg.game.sim.data;

public class SkillDetailData {
    //技能propId
    private int propId;
    //技能等级
    private int level;
    //建筑产出加成
    private int addOutPut;

    public int getPropId() {
        return propId;
    }

    public void setPropId(int propId) {
        this.propId = propId;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getAddOutPut() {
        return addOutPut;
    }

    public void setAddOutPut(int addOutPut) {
        this.addOutPut = addOutPut;
    }

    public void addOutPut(int addOutPut) {
        this.addOutPut += addOutPut;
    }
}
