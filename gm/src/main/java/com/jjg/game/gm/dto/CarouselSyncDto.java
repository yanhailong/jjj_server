package com.jjg.game.gm.dto;

import com.jjg.game.core.data.Carousel;

import java.util.List;

/**
 * 同步轮播数据
 */
public record CarouselSyncDto(
        List<Carousel> list
) {
}
