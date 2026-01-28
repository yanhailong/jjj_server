package com.jjg.game.gm.dto;

/**
 * @author 11
 * @date 2026/1/26
 */
public record ChannelLoginOpenDto(
        int loginType,
        int loginOpen,  //0.false  1.true
        int rewardOpen  //0.false  1.true
) {
}
