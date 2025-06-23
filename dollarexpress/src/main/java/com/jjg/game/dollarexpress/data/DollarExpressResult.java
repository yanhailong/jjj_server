package com.jjg.game.dollarexpress.data;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

/**
 * @author 11
 * @date 2025/6/23 10:43
 */
@Document
public class DollarExpressResult {
    @Id
    private int id;
    //所有图标
    private List<Integer> iconList;
    //中奖信息
    private List<LineInfo> awardList;
    //中奖倍率
    private int awardTimes;
    //wild数量
    private int wildCount;
    //特殊结果类型
    private int specialId;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public List<Integer> getIconList() {
        return iconList;
    }

    public void setIconList(List<Integer> iconList) {
        this.iconList = iconList;
    }

    public List<LineInfo> getAwardList() {
        return awardList;
    }

    public void setAwardList(List<LineInfo> awardList) {
        this.awardList = awardList;
    }

    public int getAwardTimes() {
        return awardTimes;
    }

    public void setAwardTimes(int awardTimes) {
        this.awardTimes = awardTimes;
    }

    public int getWildCount() {
        return wildCount;
    }

    public void setWildCount(int wildCount) {
        this.wildCount = wildCount;
    }

    public int getSpecialId() {
        return specialId;
    }

    public void setSpecialId(int specialId) {
        this.specialId = specialId;
    }
}
