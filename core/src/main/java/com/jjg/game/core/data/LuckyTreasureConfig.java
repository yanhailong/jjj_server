package com.jjg.game.core.data;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

/**
 * 夺宝奇兵配置类
 */
@Document
@CompoundIndex(name = "id_idx", def = "{'id': 1}")
public class LuckyTreasureConfig {

    /**
     * 配置唯一id
     */
    @Id
    private int id;

    /**
     * 商品价值
     */
    private int bestValue;

    /**
     * 单份需要道具数(ID_NUM)
     */
    private List<Integer> consumption;

    /**
     * 图片资源
     */
    private String des;

    /**
     * 商品ID
     */
    private int itemId;

    /**
     * 商品数量
     */
    private int itemNum;

    /**
     * 总份数
     */
    private int total;

    /**
     * 领奖时间(分)
     */
    private int collectTime;

    /**
     * 商品名称
     */
    private String name;

    /**
     * 每局抢购时间(分钟)
     */
    private int time;

    /**
     * 类型
     */
    private int type;

    /**
     * 是否重复 后台修改的开关,如果false则上一期结束后不会再开下一期
     */
    private boolean repeated;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getBestValue() {
        return bestValue;
    }

    public void setBestValue(int bestValue) {
        this.bestValue = bestValue;
    }

    public List<Integer> getConsumption() {
        return consumption;
    }

    public void setConsumption(List<Integer> consumption) {
        this.consumption = consumption;
    }

    public String getDes() {
        return des;
    }

    public void setDes(String des) {
        this.des = des;
    }

    public int getItemId() {
        return itemId;
    }

    public void setItemId(int itemId) {
        this.itemId = itemId;
    }

    public int getItemNum() {
        return itemNum;
    }

    public void setItemNum(int itemNum) {
        this.itemNum = itemNum;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getCollectTime() {
        return collectTime;
    }

    public void setCollectTime(int collectTime) {
        this.collectTime = collectTime;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public boolean isRepeated() {
        return repeated;
    }

    public void setRepeated(boolean repeated) {
        this.repeated = repeated;
    }
}
