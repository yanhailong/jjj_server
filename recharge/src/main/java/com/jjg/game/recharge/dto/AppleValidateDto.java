package com.jjg.game.recharge.dto;

/**
 * @author 11
 * @date 2025/10/23 11:16
 */
public class AppleValidateDto {
    //玩家id
    private long playerId;
    //玩家登录时的token
    private String playerToken;
    //apple充值结束后的jws
    private String jwsRepresentation;
    //apple交易id
    private String transactionId;
    //来源   1.商城   2.等级礼包
    public int rechargeType;
    //配置的订单id
    public String productId;

    public long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(long playerId) {
        this.playerId = playerId;
    }

    public String getPlayerToken() {
        return playerToken;
    }

    public void setPlayerToken(String playerToken) {
        this.playerToken = playerToken;
    }

    public String getJwsRepresentation() {
        return jwsRepresentation;
    }

    public void setJwsRepresentation(String jwsRepresentation) {
        this.jwsRepresentation = jwsRepresentation;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public int getRechargeType() {
        return rechargeType;
    }

    public void setRechargeType(int rechargeType) {
        this.rechargeType = rechargeType;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }
}
