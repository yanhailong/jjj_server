package com.jjg.game.core.data;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 商城商品
 * @author 11
 * @date 2025/9/17 19:53
 */
@Document
public class ShopProduct {
    //商品id
    @Id
    private int id;
    //类型
    private int type;
    //条件
    private Map<Integer,Integer> conditionsMap;
    //是否开启
    private boolean open;
    //开启时间
    private int startTime;
    //关闭时间
    private int endTime;
    //奖励道具
    private Map<Integer,Long> rewardItems;
    //价值类型
    private int valueType;
    //价值
    private long value;
    //购买类型  -1.充值  ,其他值则为道具id
    private int payType;
    //价格
    private BigDecimal money;
    //标签1
    private int label1;
    //标签2
    private int label2;
    //图片
    private String pic;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public Map<Integer, Integer> getConditionsMap() {
        return conditionsMap;
    }

    public void setConditionsMap(Map<Integer, Integer> conditionsMap) {
        this.conditionsMap = conditionsMap;
    }

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean open) {
        this.open = open;
    }

    public int getStartTime() {
        return startTime;
    }

    public void setStartTime(int startTime) {
        this.startTime = startTime;
    }

    public int getEndTime() {
        return endTime;
    }

    public void setEndTime(int endTime) {
        this.endTime = endTime;
    }

    public Map<Integer, Long> getRewardItems() {
        return rewardItems;
    }

    public void setRewardItems(Map<Integer, Long> rewardItems) {
        this.rewardItems = rewardItems;
    }

    public int getValueType() {
        return valueType;
    }

    public void setValueType(int valueType) {
        this.valueType = valueType;
    }

    public long getValue() {
        return value;
    }

    public void setValue(long value) {
        this.value = value;
    }

    public int getPayType() {
        return payType;
    }

    public void setPayType(int payType) {
        this.payType = payType;
    }

    public BigDecimal getMoney() {
        return money;
    }

    public void setMoney(BigDecimal money) {
        this.money = money;
    }

    public int getLabel1() {
        return label1;
    }

    public void setLabel1(int label1) {
        this.label1 = label1;
    }

    public int getLabel2() {
        return label2;
    }

    public void setLabel2(int label2) {
        this.label2 = label2;
    }

    public String getPic() {
        return pic;
    }

    public void setPic(String pic) {
        this.pic = pic;
    }
}
