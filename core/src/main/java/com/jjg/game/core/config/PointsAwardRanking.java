package com.jjg.game.core.config;

import java.util.List;

/**
 * 排行榜配置
 */
@ExcelConfig(name = "PointsAwardRanking")
public class PointsAwardRanking extends AbstractExcelConfig {

    /**
     * 排行类型
     * <p>
     * 1、上午排行
     * 2、下午排行
     * 3、月总行榜
     */
    private int type;

    /**
     * 排名名次
     */
    private List<Integer> ranking;

    /**
     * 奖励类型
     * <p>
     * 1. 其它
     * 2. 道具
     *
     */
    private int awardType;

    /**
     * 排名奖励
     * <p>
     * 奖励类型1时：客户端显示用，发放奖励时不用管；
     * 奖励类型2时：读取道具表中的ID及数量，发放奖励时也读取该配置数据；
     */
    private List<String> getItem;

    /**
     * 奖励价值
     */
    private long price;

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public List<Integer> getRanking() {
        return ranking;
    }

    public void setRanking(List<Integer> ranking) {
        this.ranking = ranking;
    }

    public int getAwardType() {
        return awardType;
    }

    public void setAwardType(int awardType) {
        this.awardType = awardType;
    }

    public List<String> getGetItem() {
        return getItem;
    }

    public void setGetItem(List<String> getItem) {
        this.getItem = getItem;
    }

    public long getPrice() {
        return price;
    }

    public void setPrice(long price) {
        this.price = price;
    }

}
