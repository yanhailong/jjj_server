package com.jjg.game.gm.dto;

import jakarta.validation.constraints.Positive;
import org.hibernate.validator.constraints.Range;

/**
 * @author lm
 * @date 2025/7/10 11:04
 */
public record GameStatusDto(
        @Positive(message = "游戏id不能为负")
        int number,
        @Range(min = 1, max = 2, message = "开放状态只能为1(开放)或2(不开放)")
        int open,
        @Range(min = 1, max = 2, message = "上下架状态只能为1(上架)或2(下架)")
        int status,
        //角标
        String right_top_icon) {
}
