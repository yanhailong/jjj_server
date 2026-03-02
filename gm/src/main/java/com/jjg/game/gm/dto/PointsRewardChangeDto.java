package com.jjg.game.gm.dto;

import java.util.List;

/**
 * @author 11
 * @date 2026/3/2
 */
public record PointsRewardChangeDto(
        List<PointsAddAto> pointsAddListAto,
        //0.增加积分  1.增加转盘次数
        int type
) {
}
