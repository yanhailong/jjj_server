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
    int ROOM_TICK_TIME = 200;
    // 房间公共配置加载路径
    String ROOM_SAMPLE_PATH = CoreConst.Common.SAMPLE_ROOT_PATH + "common";
    // 玩家数据回存检查间隔时间 10-30分钟检查一次
    int PLAYER_SAVE_CHECK_INTERVAL_MIN = 10 * 60 * 1000;
    // 玩家数据回存检查间隔时间 10-30分钟检查一次
    int PLAYER_SAVE_CHECK_INTERVAL_MAX = 30 * 60 * 1000;
    // 玩家回存数据添加timer的下限时间, 用于分散时间存储数据
    int PLAYER_SAVE_DB_TIME_MIN = 500;
    // 玩家回存数据添加timer的上限时间
    int PLAYER_SAVE_DB_TIME_MAX = 5 * 1000;
}
