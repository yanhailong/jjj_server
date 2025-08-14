package com.jjg.game.room.datatrack;

/**
 * 数据打点字段常量名，打点非必要，高频重复使用的字段放置于此,用常量可以避免字段大小写不一致或者拼写错误的问题
 *
 * @author 2CL
 */
public interface DataTrackNameConstant {
    // 总押注
    String TOTAL_BET = "TotalBet";
    // 总赢分
    String TOTAL_WIN = "TotalWin";
    // 收入
    String INCOME = "Income";
    //区域数据
    String AREA_DATA = "AreaData";
}
