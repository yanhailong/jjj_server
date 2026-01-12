package com.jjg.game.gm.dto;

public record SetUrlPrefixDto(
        int type,  //1. 客服链接   2.积分大奖链接
        String url
) {
}
