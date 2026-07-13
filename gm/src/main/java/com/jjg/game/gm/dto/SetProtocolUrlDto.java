package com.jjg.game.gm.dto;

public record SetProtocolUrlDto(
        int type,  //1. 隐私协议链接   2.服务协议链接
        String url
) {
}
