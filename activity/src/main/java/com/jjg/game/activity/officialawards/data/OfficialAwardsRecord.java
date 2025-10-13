package com.jjg.game.activity.officialawards.data;

/**
 * @author lm
 * @date 2025/9/22 17:02
 */
public class OfficialAwardsRecord {
    //名称
    private String name;
    //创建时间
    private long createTime;
    //奖金数量
    private long getNum;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public long getGetNum() {
        return getNum;
    }

    public void setGetNum(long getNum) {
        this.getNum = getNum;
    }

    @Override
    public String toString() {
        return "OfficialAwardsRecord{" +
                "name='" + name + '\'' +
                ", createTime=" + createTime +
                ", getNum=" + getNum +
                '}';
    }
}
