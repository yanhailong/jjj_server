package com.jjg.game.gm.dto;

/**
 * @author 11
 * @date 2025/8/25 13:42
 */
public record KickAccountDto (
        int type,  //1.指定id  2.全服
        String ids,
        int langId //多语言id
){
}
