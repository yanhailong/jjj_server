package com.jjg.game.core.data;

import com.jjg.game.core.pb.RechargeType;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;

/**
 * 订单
 * @author 11
 * @date 2025/9/18 13:37
 */
@Document
public class Order {
    //订单id
    @Id
    private String id;
    //苹果下单透传的是uuid
    @Indexed
    private String uuid;
    //玩家id
    private long playerId;
    //玩家渠道
    private int playerChannel;
    //支付渠道
    private int payChannel;
    //商品id
    private String productId;
    //价格
    private BigDecimal price;
    //订单状态
    private OrderStatus orderStatus;
    //充值类型
    private RechargeType rechargeType;
    //创建时间
    private int createTime;
    //更新时间
    private int updateTime;
    //渠道订单id
    private String channelOrderId;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(long playerId) {
        this.playerId = playerId;
    }

    public int getPlayerChannel() {
        return playerChannel;
    }

    public void setPlayerChannel(int playerChannel) {
        this.playerChannel = playerChannel;
    }

    public int getPayChannel() {
        return payChannel;
    }

    public void setPayChannel(int payChannel) {
        this.payChannel = payChannel;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public OrderStatus getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(OrderStatus orderStatus) {
        this.orderStatus = orderStatus;
    }

    public RechargeType getRechargeType() {
        return rechargeType;
    }

    public void setRechargeType(RechargeType rechargeType) {
        this.rechargeType = rechargeType;
    }

    public int getCreateTime() {
        return createTime;
    }

    public void setCreateTime(int createTime) {
        this.createTime = createTime;
    }

    public int getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(int updateTime) {
        this.updateTime = updateTime;
    }

    public String getChannelOrderId() {
        return channelOrderId;
    }

    public void setChannelOrderId(String channelOrderId) {
        this.channelOrderId = channelOrderId;
    }
}
