package com.jjg.game.core.pb.reddot;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * 红点详情
 */
@ProtobufMessage
@ProtoDesc("红点详情")
public class RedDotDetails {

    /**
     * 类型
     */
    @ProtoDesc("类型 0-普通红点 1-数量红点")
    private RedDotType redDotType;

    /**
     * 模块
     */
    @ProtoDesc("模块 1-邮件 2-背包 3-活动")
    private RedDotModule redDotModule;

    /**
     * 子模块
     */
    @ProtoDesc("子模块 1-获得新道具 2-签到")
    private int redDotSubmodule;

    /**
     * 数量
     */
    @ProtoDesc("数量")
    private long count;

    /**
     * 额外参数
     */
    @ProtoDesc("额外参数")
    private String extra;

    /**
     * 红点类型
     */
    @ProtobufMessage
    @ProtoDesc("红点类型")
    public enum RedDotType {
        /**
         * 普通红点
         */
        @ProtoDesc("普通红点")
        COMMON,
        /**
         * 数量红点
         */
        @ProtoDesc("数量红点")
        COUNT,
    }

    /**
     * 红点模块
     */
    @ProtobufMessage
    @ProtoDesc("红点模块")
    public enum RedDotModule {
        /**
         * 邮件
         */
        @ProtoDesc("邮件")
        MAIL,
        /**
         * 背包
         */
        @ProtoDesc("背包")
        PACK,
        /**
         * 活动
         */
        @ProtoDesc("活动")
        ACTIVITY,
    }


    public String getExtra() {
        return extra;
    }

    public void setExtra(String extra) {
        this.extra = extra;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public int getRedDotSubmodule() {
        return redDotSubmodule;
    }

    public void setRedDotSubmodule(int redDotSubmodule) {
        this.redDotSubmodule = redDotSubmodule;
    }

    public RedDotModule getRedDotModule() {
        return redDotModule;
    }

    public void setRedDotModule(RedDotModule redDotModule) {
        this.redDotModule = redDotModule;
    }

    public RedDotType getRedDotType() {
        return redDotType;
    }

    public void setRedDotType(RedDotType redDotType) {
        this.redDotType = redDotType;
    }
}
