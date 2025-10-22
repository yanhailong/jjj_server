package com.jjg.game.gm.dto;

/**
 * 修改玩家积分
 *
 * @param playerId 玩家id
 * @param points   积分
 * @param flag     true 修改 false减少
 */
public record ChangePointsDto(
        long playerId,
        int points,
        boolean flag
) {
}
