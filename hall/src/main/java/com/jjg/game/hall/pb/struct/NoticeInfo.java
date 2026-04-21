package com.jjg.game.hall.pb.struct;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author 11
 * @date 2025/11/10 11:17
 */
@ProtobufMessage
@ProtoDesc("公告信息")
public class NoticeInfo {
    private long id;
    @ProtoDesc("公告名称")
    private String name;
    @ProtoDesc("标题")
    private String title;
    @ProtoDesc("内容")
    private String content;
    @ProtoDesc("类型  1.普通公告   2.场景公告  3.外部地址")
    private int type;
    @ProtoDesc("排序")
    private int sort;
    @ProtoDesc("角标资源")
    private String cornerMark;
    @ProtoDesc("背景图")
    private String backdrop;
    @ProtoDesc("按钮图")
    private String button;
    @ProtoDesc("开始时间")
    private int startTime;
    @ProtoDesc("结束时间")
    private int endTime;
    @ProtoDesc("跳转场景")
    private int scence;
    @ProtoDesc("跳转地址")
    private String jumpUrl;
    @ProtoDesc("是否阅读")
    private boolean read;
    @ProtoDesc("大类型  0.公告  1.活动")
    private int bigType;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getSort() {
        return sort;
    }

    public void setSort(int sort) {
        this.sort = sort;
    }

    public String getCornerMark() {
        return cornerMark;
    }

    public void setCornerMark(String cornerMark) {
        this.cornerMark = cornerMark;
    }

    public String getBackdrop() {
        return backdrop;
    }

    public void setBackdrop(String backdrop) {
        this.backdrop = backdrop;
    }

    public String getButton() {
        return button;
    }

    public void setButton(String button) {
        this.button = button;
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

    public int getScence() {
        return scence;
    }

    public void setScence(int scence) {
        this.scence = scence;
    }

    public String getJumpUrl() {
        return jumpUrl;
    }

    public void setJumpUrl(String jumpUrl) {
        this.jumpUrl = jumpUrl;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public int getBigType() {
        return bigType;
    }

    public void setBigType(int bigType) {
        this.bigType = bigType;
    }
}
