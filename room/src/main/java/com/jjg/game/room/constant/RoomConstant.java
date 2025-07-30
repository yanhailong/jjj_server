package com.jjg.game.room.constant;

import com.jjg.game.common.constant.CoreConst;

/**
 * @author 11
 * @date 2025/6/25 10:01
 */
public interface RoomConstant {
    // 最小房间id
    int ROOM_ID_MIN = 100000;
    // 最大房间id
    int ROOM_ID_MAX = 999999;
    // 房间每次tick时间
    int ROOM_TICK_TIME = 100;

    String ROOM_SAMPLE_PATH = CoreConst.Common.SAMPLE_ROOT_PATH + "common";
}
