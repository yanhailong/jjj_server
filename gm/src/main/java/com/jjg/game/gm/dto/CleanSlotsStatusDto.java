package com.jjg.game.gm.dto;

/**
 * @author 11
 * @date 2026/1/28
 */
public record CleanSlotsStatusDto(
        long playerId,
        int gameType,
        int roomCfgId
) {
}
