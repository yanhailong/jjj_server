package com.jjg.game.gm.dto;

import java.util.List;

public record BackendRechargeDto(
        long playerId,
        String channelOrderId,
        String price,
        List<ItemDto> items,
        int rechargeType
) {
}
