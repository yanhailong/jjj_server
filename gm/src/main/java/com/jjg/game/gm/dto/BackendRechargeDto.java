package com.jjg.game.gm.dto;

public record BackendRechargeDto(
        long playerId,
        String channelOrderId,
        String productId,
        long price,
        int rechargeType
) {
}
