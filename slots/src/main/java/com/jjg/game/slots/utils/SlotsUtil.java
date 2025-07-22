package com.jjg.game.slots.utils;

/**
 * @author 11
 * @date 2025/7/22 17:41
 */
public class SlotsUtil {
    public static int wareIdToRoomCfgId(int wareId,int gameType) {
        return gameType * 10 + wareId;
    }
}
