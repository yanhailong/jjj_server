package com.jjg.game.gm.dto;

import java.util.List;

/**
 * 删除轮播数据
 */
public record CarouselDeleteDto(
        List<Long> id
) {
}
