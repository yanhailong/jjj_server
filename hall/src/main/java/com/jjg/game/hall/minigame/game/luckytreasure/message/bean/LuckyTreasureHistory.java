package com.jjg.game.hall.minigame.game.luckytreasure.message.bean;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * 夺宝奇兵开奖历史详情
 */
@ProtobufMessage
@ProtoDesc("夺宝奇兵开奖历史详情")
public class LuckyTreasureHistory {

    /**
     * 期号
     */
    @ProtoDesc("期号")
    private long issueNumber;

    /**
     * 配置id
     */
    @ProtoDesc("配置id")
    private int configId;

    /**
     * 商品类型
     */
    @ProtoDesc("商品类型")
    private int type;

    /**
     * 商品道具id
     */
    @ProtoDesc("商品道具id")
    private int itemId;

    /**
     * 商品道具数量
     */
    @ProtoDesc("商品道具数量")
    private int itemNum;

    /**
     * 商品icon
     */
    @ProtoDesc("商品icon")
    private String icon;

    /**
     * 商品名字
     */
    @ProtoDesc("商品名字")
    private String name;

    public long getIssueNumber() {
        return issueNumber;
    }

    public void setIssueNumber(long issueNumber) {
        this.issueNumber = issueNumber;
    }

    public int getConfigId() {
        return configId;
    }

    public void setConfigId(int configId) {
        this.configId = configId;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
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

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
