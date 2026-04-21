package com.jjg.game.core.data;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * 轮播数据
 */
@ProtobufMessage
@ProtoDesc("轮播数据")
public class Carousel {
    /**
     * 类型 1轮播 2活动
     */
    @ProtoDesc("类型 1轮播 2活动")
    private int activityImageType;

    /**
     * 资源名字
     */
    @ProtoDesc("资源名字")
    private String sourceName;

    /**
     * 跳转类型 1 游戏 2 网址 3 无 4 活动
     */
    @ProtoDesc("跳转类型 1 游戏 2 网址 3 无 4 活动")
    private int jumpType;

    /**
     * 跳转值
     */
    @ProtoDesc("跳转值")
    private String jumpValue;

    /**
     * 排序值
     */
    @ProtoDesc("排序值")
    private int sort;

    /**
     * 动图 1是 2不是
     */
    @ProtoDesc("动图 1是 2不是")
    private int showType;

    /**
     * id唯一
     */
    @ProtoDesc("id唯一")
    private long id;

    public Carousel() {
    }

    public Carousel(long id) {
        this.id = id;
    }

    public Carousel(int activityImageType, String sourceName, int jumpType, String jumpValue, int sort, int showType, long id) {
        this.activityImageType = activityImageType;
        this.sourceName = sourceName;
        this.jumpType = jumpType;
        this.jumpValue = jumpValue;
        this.sort = sort;
        this.showType = showType;
        this.id = id;
    }

    public int getActivityImageType() {
        return activityImageType;
    }

    public void setActivityImageType(int activityImageType) {
        this.activityImageType = activityImageType;
    }

    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    public int getJumpType() {
        return jumpType;
    }

    public void setJumpType(int jumpType) {
        this.jumpType = jumpType;
    }

    public String getJumpValue() {
        return jumpValue;
    }

    public void setJumpValue(String jumpValue) {
        this.jumpValue = jumpValue;
    }

    public int getSort() {
        return sort;
    }

    public void setSort(int sort) {
        this.sort = sort;
    }

    public int getShowType() {
        return showType;
    }

    public void setShowType(int showType) {
        this.showType = showType;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "Carousel{" +
                "activityImageType=" + activityImageType +
                ", sourceName='" + sourceName + '\'' +
                ", jumpType=" + jumpType +
                ", jumpValue='" + jumpValue + '\'' +
                ", sort=" + sort +
                ", showType=" + showType +
                ", id=" + id +
                '}';
    }
}
